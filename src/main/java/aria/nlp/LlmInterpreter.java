package aria.nlp;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Singleton NLP interpreter that converts natural-language user input into valid
 * Aria command strings.
 *
 * <p>Uses a local {@code llama-server} subprocess (via {@link LlmServer}) so it
 * works with any GGUF model — including architectures not yet supported by
 * embedded JNI bindings.  Install the binary with {@code brew install llama.cpp}.
 *
 * <p>Safeguards:
 * <ul>
 *   <li>Two-phase output validation: exact first-word match, then preamble-stripping scan.</li>
 *   <li>Destructive commands (delete, archive) flagged for caller confirmation.</li>
 *   <li>Inference timeout (default 15 s, override with {@code -Daria.llm.timeout=N}).</li>
 *   <li>Input locking in the UI prevents double-submission during inference.</li>
 *   <li>Graceful degradation: if the server fails to start, {@link #interpret} returns
 *       {@code null} and the app continues working normally for known commands.</li>
 * </ul>
 */
public class LlmInterpreter {

    private static final Logger LOGGER = Logger.getLogger(LlmInterpreter.class.getName());

    private static final int DEFAULT_TIMEOUT_SECONDS = Integer.getInteger("aria.llm.timeout", 15);
    private static final int MAX_TOKENS = 80;

    /** All recognised command words (mirrors Parser switch + aliases). */
    static final Set<String> KNOWN_COMMANDS = Set.of(
            "bye", "list", "mark", "unmark", "delete", "todo", "deadline", "event",
            "find", "archive", "archives", "tag", "untag", "update", "sort", "stats",
            "undo", "snooze", "schedule", "note", "contact", "expense", "loan",
            "place", "trivia", "client", "item", "help", "freetime", "fixed",
            "within", "tevent", "confirm",
            // short aliases
            "t", "d", "e", "ls", "rm", "q", "exit"
    );

    /**
     * Multi-character command words used for the fallback preamble-stripping scan.
     * Single-letter aliases are excluded to avoid false matches inside ordinary words.
     */
    private static final Set<String> SCANNABLE_COMMANDS = KNOWN_COMMANDS.stream()
            .filter(c -> c.length() > 2)
            .collect(Collectors.toUnmodifiableSet());

    /** Commands that modify or remove tasks and require explicit user confirmation when LLM-generated. */
    static final Set<String> DESTRUCTIVE_COMMANDS = Set.of("delete", "archive", "rm");

    /** Pre-compiled pattern that matches any multi-char command word at a word boundary. */
    private static final Pattern COMMAND_SCAN_PATTERN;

    static {
        String cmdAlternation = SCANNABLE_COMMANDS.stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        COMMAND_SCAN_PATTERN = Pattern.compile(
                "(?:^|\\s)(" + cmdAlternation + ")(?=\\s|$)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
    }

    // ChatML-format system prompt tuned for a 0.8B model: short, concrete, few-shot.
    private static final String PROMPT_TEMPLATE =
            "<|im_start|>system\n"
            + "You are Aria, a friendly task manager assistant."
            + " Convert user input into ONE Aria command string.\n\n"
            + "COMMANDS:\n"
            + "- todo <desc> [/priority high|medium|low] [/every daily|weekly|monthly]\n"
            + "- deadline <desc> /by <date>\n"
            + "- event <desc> /from <date> /to <date>\n"
            + "- list\n"
            + "- mark <n>  |  unmark <n>  |  delete <n>\n"
            + "- find <keyword>\n"
            + "- sort name|priority|date\n"
            + "- archive <n>  |  tag <n> <tag>  |  untag <n> <tag>\n"
            + "- update <n> [/desc X] [/by X] [/priority X]\n"
            + "- note [add <text>|list|delete <n>]\n"
            + "- stats  |  undo  |  help  |  bye\n\n"
            + "RULES:\n"
            + "1. Output ONLY the command string, no explanation.\n"
            + "2. If the input mentions a due date or \"by [date]\", ALWAYS use deadline, never todo.\n"
            + "3. For greetings, jokes, or chitchat, output: CHAT: <short friendly reply>\n"
            + "4. For nonsense, gibberish, or offensive input, output: CHAT: Sorry, I didn't understand that!\n"
            + "5. If no command fits, output: UNKNOWN\n"
            + "6. Use natural language dates (tomorrow, next Monday, Dec 25 2024)\n\n"
            + "EXAMPLES:\n"
            + "remind me to buy groceries -> todo buy groceries\n"
            + "remind me to watch lecture 2 by tmr -> deadline watch lecture 2 /by tomorrow\n"
            + "remind me to submit assignment by next Monday -> deadline submit assignment /by next Monday\n"
            + "can you help me add a task to watch lecture 2 tmr -> deadline watch lecture 2 /by tomorrow\n"
            + "i need to call the dentist -> todo call the dentist\n"
            + "please add watch lecture 2 of cs2218 -> todo watch lecture 2 of cs2218\n"
            + "add a todo i need to read my science textbook -> todo read my science textbook\n"
            + "add todo buy milk -> todo buy milk\n"
            + "add a task to finish the slides -> todo finish the slides\n"
            + "create a todo call the bank -> todo call the bank\n"
            + "can you help me add a task to read all my textbooks by tmr -> deadline read all my textbooks /by tomorrow\n"
            + "add a task to submit the report by next Friday -> deadline submit the report /by next Friday\n"
            + "add a task to clean the house -> todo clean the house\n"
            + "i need to finish cs3281 homework by tmr -> deadline finish cs3281 homework /by tomorrow\n"
            + "i need to finish cs3281 homework -> todo finish cs3281 homework\n"
            + "i need to read the textbook by next Monday -> deadline read the textbook /by next Monday\n"
            + "need to call the dentist by Friday -> deadline call the dentist /by Friday\n"
            + "meeting with John next Friday 9am to 11am -> event meeting with John"
            + " /from next Friday 9am /to next Friday 11am\n"
            + "finish the report by end of tomorrow -> deadline finish the report /by tomorrow\n"
            + "remove task 3 -> delete 3\n"
            + "what are my tasks -> list\n"
            + "high priority task to call dentist -> todo call dentist /priority high\n"
            + "hello -> CHAT: Hey there! Ready to tackle your to-do list?\n"
            + "hi there -> CHAT: Hi! What can I help you get done today?\n"
            + "tell me a joke -> CHAT: Why did the task cross the road? To get to the deadline on time!\n"
            + "how are you -> CHAT: I'm doing great and ready to help you stay on top of things!\n"
            + "asdfjklqwerty -> CHAT: Sorry, I didn't understand that!\n"
            + "hfkjsdhkfjsdh -> CHAT: Sorry, I didn't understand that!\n"
            + "<|im_end|>\n"
            + "<|im_start|>user\n"
            + "{USER_INPUT}\n"
            + "<|im_end|>\n"
            + "<|im_start|>assistant\n"
            // Pre-fill the thinking block as empty so the model skips reasoning
            // and outputs only the command string (required for Qwen3 thinking models).
            + "<think>\n\n</think>\n";

    // ── Singleton ──────────────────────────────────────────────────────────────

    private static volatile LlmInterpreter instance;

    private final LlmServer server = new LlmServer();
    private boolean ready = false;
    private final int timeoutSeconds;

    private LlmInterpreter(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /** Returns the singleton instance, creating it on first call. */
    public static LlmInterpreter getInstance() {
        if (instance == null) {
            synchronized (LlmInterpreter.class) {
                if (instance == null) {
                    instance = new LlmInterpreter(DEFAULT_TIMEOUT_SECONDS);
                }
            }
        }
        return instance;
    }

    // ── Initialisation ─────────────────────────────────────────────────────────

    /**
     * Starts the llama-server subprocess and waits for it to become ready.
     * Safe to call from a background thread. Subsequent calls are no-ops.
     *
     * <p>Requires {@code llama-server} to be installed ({@code brew install llama.cpp}).
     */
    public void init() {
        synchronized (this) {
            if (ready) {
                return;
            }
            ready = server.start();
        }
    }

    // ── Interpretation ─────────────────────────────────────────────────────────

    /**
     * Interprets natural-language input and returns a valid Aria command string,
     * or {@code null} if interpretation fails, times out, or no command is recognised.
     *
     * <p>Must be called from a background thread (blocks up to {@code timeoutSeconds}).
     *
     * @param userInput raw user input that failed traditional parsing
     * @return a validated Aria command string, or {@code null}
     */
    public String interpret(String userInput) {
        if (!ready) {
            return null;
        }

        String prompt = PROMPT_TEMPLATE.replace("{USER_INPUT}", userInput.trim());

        CompletableFuture<String> future = CompletableFuture.supplyAsync(
                () -> server.complete(prompt, MAX_TOKENS)
        );

        try {
            String raw = future.get(timeoutSeconds, TimeUnit.SECONDS);
            return validateAndNormalise(raw);
        } catch (TimeoutException e) {
            future.cancel(true);
            LOGGER.warning("LLM inference timed out after " + timeoutSeconds + "s");
            return null;
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.warning("LLM inference failed: " + e.getMessage());
            return null;
        }
    }

    // ── Validation & Safeguards ────────────────────────────────────────────────

    /**
     * Validates and normalises raw LLM output into a usable Aria command string.
     *
     * <p>Two-phase extraction:
     * <ol>
     *   <li><b>Direct match</b>: if the first word is a known command, return as-is.</li>
     *   <li><b>Scan</b>: if the model prepended preamble (e.g. "Sure! todo buy milk"),
     *       locate the first known command word at a word boundary and return the substring
     *       from that point.  Single-letter aliases are excluded from the scan to avoid
     *       false matches inside ordinary English words.</li>
     * </ol>
     *
     * @param raw the raw string produced by the model
     * @return a clean command string, or {@code null}
     */
    String validateAndNormalise(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        // Strip accidental surrounding quotes or markdown backticks.
        String cleaned = raw.replaceAll("^['\"`]+|['\"`]+$", "").trim();
        if (cleaned.equalsIgnoreCase("UNKNOWN")) {
            return null;
        }

        // Phase 1a: CHAT response — pass through directly.
        if (cleaned.regionMatches(true, 0, "CHAT:", 0, 5)) {
            return cleaned;
        }

        // Phase 1b: first word is a known command — accept directly.
        String firstWord = cleaned.split("\\s+")[0].toLowerCase();
        if (KNOWN_COMMANDS.contains(firstWord)) {
            return cleaned;
        }

        // Phase 2: scan for a command word preceded by whitespace or string start.
        Matcher matcher = COMMAND_SCAN_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            String extracted = cleaned.substring(matcher.start(1)).trim();
            LOGGER.info("LLM preamble stripped; extracted: " + extracted);
            return extracted;
        }

        LOGGER.info("LLM output contains no recognised command: " + cleaned);
        return null;
    }

    /**
     * Returns {@code true} if the given string is a conversational chat response
     * (starts with {@code CHAT:}) rather than an executable command.
     *
     * @param commandString a validated string from {@link #interpret}
     * @return {@code true} if it is a chat response
     */
    public boolean isChatResponse(String commandString) {
        return commandString != null && commandString.regionMatches(true, 0, "CHAT:", 0, 5);
    }

    /**
     * Extracts the message text from a {@code CHAT:<message>} response string.
     *
     * @param chatResponse a string starting with {@code CHAT:}
     * @return the message after the {@code CHAT:} prefix, trimmed
     */
    public String extractChatMessage(String chatResponse) {
        return chatResponse.substring(5).trim();
    }

    /**
     * Returns {@code true} if the given command string is a destructive operation
     * (delete / archive / rm) requiring explicit user confirmation before executing.
     *
     * @param commandString a validated command string
     * @return {@code true} if destructive
     */
    public boolean isDestructive(String commandString) {
        if (commandString == null || commandString.isBlank()) {
            return false;
        }
        String firstWord = commandString.split("\\s+")[0].toLowerCase();
        return DESTRUCTIVE_COMMANDS.contains(firstWord);
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    /** Returns {@code true} if the model server is running and ready to interpret. */
    public boolean isReady() {
        return ready;
    }

    /** Shuts down the llama-server subprocess. Call on application exit. */
    public void close() {
        server.stop();
        ready = false;
    }
}
