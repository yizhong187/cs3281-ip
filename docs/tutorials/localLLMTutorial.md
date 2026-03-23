# Adding AI Features to a Java App (with llama.cpp)

**Goal:** Learn how the natural language mode in Aria works — how a local `llama-server` subprocess is managed, how user input is interpreted into app commands, and how everything is wired into the GUI.

---

## Architecture Overview

Unlike embedded-model approaches (e.g. JLama), Aria runs inference via **llama.cpp's `llama-server`** as an external subprocess. The app spawns the server on startup, then communicates with it over HTTP on `localhost:38765` using Java's built-in `HttpClient` — no extra AI library dependencies needed.

```
User input
    │
    ▼
Parser.parse()  ──── success ──▶  execute command directly
    │ throws AriaException
    ▼
LlmInterpreter.interpret()
    │  builds ChatML prompt from template
    ▼
LlmServer.complete()  ──HTTP POST /completion──▶  llama-server subprocess
    │                                                      │
    │  ◀──────────────── raw text ──────────────────────────
    ▼
validateAndNormalise()
    │  strips preamble, checks known commands
    ▼
Aria.executeInterpreted()  ──▶  Parser.parse()  ──▶  execute
```

---

## Prerequisites

- **Java 17+** (`java -version`)
- **llama.cpp** installed on your system — the `llama-server` binary must be on your PATH or in a standard Homebrew location:

<panel header="**Installation instructions (macOS / Linux / Windows)**">

**macOS (Homebrew):**
```bash
brew install llama.cpp
```

**Linux (Debian / Ubuntu):**
```bash
sudo apt update && sudo apt install llama-cpp
```

**Windows:**
1. Download the latest release zip from the [llama.cpp releases page](https://github.com/ggerganov/llama.cpp/releases). Look for a filename like `llama-b<version>-bin-win-avx2-x64.zip`.
2. Extract the zip to a permanent folder, e.g. `C:\llama.cpp`.
3. Add that folder to your `PATH`:
   - Click the search bar at the bottom of the screen and type **"Control Panel"**, then open it.
   - Click **System and Security**, then **System**.
   - Click **Advanced System Settings** (under "Related links").
   - Click **Environment Variables…** at the bottom of the dialog.
   - Under **User variables**, select **Path** and click **Edit…**.
   - Click **New** and enter the folder path, e.g. `C:\llama.cpp`.
   - Click **OK** on all three dialogs to save.
4. Open a **new** Command Prompt or terminal window (existing ones won't see the updated `PATH`).

**Verify the installation:**
```bash
llama-server --version
```
You should see a version string. If this command is not found, check that the binary is on your `PATH`.

</panel>

- **A GGUF model file** placed at `src/main/resources/models/Qwen3.5-0.8B-Q4_K_M.gguf`. Download it with `huggingface-cli`:

```bash
pip install huggingface_hub
huggingface-cli download Qwen/Qwen3.5-0.8B --local-dir ./src/main/resources/models
```

> The model is bundled into the fat JAR as a classpath resource. When running from the JAR, `LlmServer` extracts it to `./data/model-cache/` on first launch.

> **No Ollama, no API key, no extra Gradle dependencies.** `llama-server` is a standalone binary and Java's `java.net.http.HttpClient` (available since Java 11) handles the HTTP calls.

---

## Step 1: Start the llama-server Subprocess

`LlmServer` ([src/main/java/aria/nlp/LlmServer.java](src/main/java/aria/nlp/LlmServer.java)) manages the entire lifecycle of the `llama-server` process.

### Finding the binary

The server probes a list of candidate paths in order:

```java
private static final String[] CANDIDATE_PATHS = {
    "llama-server",                       // on PATH
    "/opt/homebrew/bin/llama-server",     // Homebrew Apple Silicon
    "/usr/local/bin/llama-server",        // Homebrew Intel
    "/usr/bin/llama-server"
};
```

### Resolving the model file

When running from an exploded build the model is used directly from the classpath. Inside a fat JAR it is extracted to `./data/model-cache/`:

```java
private File resolveModelFile() throws IOException {
    URL resourceUrl = LlmServer.class.getResource(MODEL_RESOURCE);
    if ("file".equals(resourceUrl.getProtocol())) {
        return new File(resourceUrl.toURI());   // dev mode: use directly
    }
    // Fat-JAR: extract to local cache on first run
    File dest = new File(CACHE_DIR, CACHED_MODEL_NAME);
    if (!dest.exists()) {
        try (InputStream in = LlmServer.class.getResourceAsStream(MODEL_RESOURCE)) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
    return dest;
}
```

### Launching the process

```java
ProcessBuilder pb = new ProcessBuilder(
        binary,
        "--model", modelFile.getAbsolutePath(),
        "--port", String.valueOf(SERVER_PORT),   // 38765
        "--ctx-size", "2048",
        "--n-gpu-layers", "0",
        "--threads", String.valueOf(Runtime.getRuntime().availableProcessors() / 2),
        "--no-webui",
        "--log-disable"
);
pb.redirectErrorStream(true);
pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
serverProcess = pb.start();
```

The server polls `GET /health` every 500 ms (up to 60 s) until it responds with HTTP 200 before marking itself ready.

---

## Step 2: Send a Prompt and Parse the Response

`LlmServer.complete()` POSTs a JSON body to `http://127.0.0.1:38765/completion` and extracts the `"content"` field from the response with a small regex:

```java
public String complete(String prompt, int maxTokens) {
    String body = "{"
            + "\"prompt\":\"" + escapedPrompt + "\","
            + "\"n_predict\":" + maxTokens + ","
            + "\"temperature\":0.0,"
            + "\"stop\":[\"\\n\",\"<|im_end|>\",\"User:\"],"
            + "\"stream\":false"
            + "}";

    HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(BASE_URL + "/completion"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return parseContent(response.body());   // extracts "content" field via regex
}
```

> **Why `temperature=0.0` and `stop` tokens?** We want deterministic, single-line output. The stop tokens `\n` and `<|im_end|>` cut off the model immediately after it produces the command.

---

## Step 3: Define the System Prompt in a Template File

Rather than hardcoding the prompt in Java, it lives in
[src/main/resources/nlp/prompt_template.txt](src/main/resources/nlp/prompt_template.txt)
and is loaded once at class-init time:

```java
private static final String PROMPT_TEMPLATE = loadPromptTemplate();

private static String loadPromptTemplate() {
    try (InputStream is = LlmInterpreter.class.getResourceAsStream("/nlp/prompt_template.txt")) {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}
```

The template uses **ChatML** format (required by Qwen models) and instructs the model to output *only* a valid command string:

```
<|im_start|>system
You are Aria, a friendly task manager assistant. Convert user input into ONE Aria command string.

COMMANDS:
- todo <desc> [/priority high|medium|low]
- deadline <desc> /by <date>
- event <desc> /from <date> /to <date>
- list
- mark <n>  |  unmark <n>  |  delete <n>
- find <keyword>
...

RULES:
1. Output ONLY the command string, no explanation.
2. For greetings or chitchat, output: CHAT: <short friendly reply>
3. If no command fits, output: UNKNOWN

EXAMPLES:
remind me to buy groceries -> todo buy groceries
remove task 3 -> delete 3
hello -> CHAT: Hey there! Ready to tackle your to-do list?
<|im_end|>
<|im_start|>user
{USER_INPUT}
<|im_end|>
<|im_start|>assistant
```

At runtime `{USER_INPUT}` is replaced with the actual user input before the prompt is sent.

---

## Step 4: Validate and Normalise the LLM Output

Small models sometimes prepend preamble ("Sure! here is the command: `todo buy milk`"). `LlmInterpreter.validateAndNormalise()` handles this with a two-phase check:

```java
String validateAndNormalise(String raw) {
    String cleaned = raw.replaceAll("^['\"`]+|['\"`]+$", "").trim();

    // Pass-through for chat responses
    if (cleaned.regionMatches(true, 0, "CHAT:", 0, 5)) {
        return cleaned;
    }

    // Phase 1: first word is a known command — accept directly
    String firstWord = cleaned.split("\\s+")[0].toLowerCase();
    if (KNOWN_COMMANDS.contains(firstWord)) {
        return cleaned;
    }

    // Phase 2: scan for a command word after preamble
    Matcher matcher = COMMAND_SCAN_PATTERN.matcher(cleaned);
    if (matcher.find()) {
        return cleaned.substring(matcher.start(1)).trim();
    }

    return null;   // unrecognised — caller shows fallback message
}
```

`KNOWN_COMMANDS` mirrors every command word in `Parser.java` plus short aliases (`t`, `d`, `rm`, etc.), so the whitelist stays in sync with the app.

---

## Step 5: Wire It into the GUI

`LlmInterpreter` is a singleton initialised on a background thread in
[MainWindow.java](src/main/java/aria/ui/MainWindow.java)
so the GUI stays responsive:

```java
Task<Void> initTask = new Task<>() {
    @Override
    protected Void call() {
        LlmInterpreter.getInstance().init();   // starts llama-server, blocks until ready
        return null;
    }
};
initTask.setOnSucceeded(e -> {
    String status = LlmInterpreter.getInstance().isReady()
            ? "(Natural language mode ready. You can now type in plain English!)"
            : "(Natural language model not available — exact commands still work.)";
    dialogContainer.getChildren().add(DialogBox.getAriaDialog(status, ariaImage));
    setInputEnabled(true);
});
Thread llmInit = new Thread(initTask, "llm-init");
llmInit.setDaemon(true);
llmInit.start();
```

### The fallback flow

When the user submits a message, the app first tries the traditional parser. Only if that fails (unknown command) does it call the LLM:

```java
// Try exact command parsing first — no LLM involvement
try {
    String response = aria.tryGetResponseDirect(input);
    dialogContainer.getChildren().add(DialogBox.getAriaDialog(response, ariaImage));
    return;
} catch (aria.AriaException ignored) {
    // Unknown command — fall through to LLM
}

// LLM fallback: run on a daemon thread to avoid blocking the JavaFX thread
Task<String> llmTask = new Task<>() {
    @Override
    protected String call() {
        return LlmInterpreter.getInstance().interpret(input);
    }
};
llmTask.setOnSucceeded(e -> handleLlmResult(llmTask.getValue()));
new Thread(llmTask, "llm-interpret").start();
```

### Destructive command confirmation

For `delete`, `archive`, and `rm`, the app pauses and asks the user to confirm before executing:

```java
if (llm.isDestructive(candidate)) {
    pendingLlmCommand = candidate;
    dialogContainer.getChildren().add(DialogBox.getAriaDialog(
            "I interpreted that as: \"" + candidate + "\"\n"
            + "This will permanently modify your task list.\n"
            + "Type yes to confirm, or anything else to cancel.",
            ariaImage));
}
```

---

## Step 6: Package Everything into a Fat JAR

The model file is placed in `src/main/resources/models/` so it is automatically included in the fat JAR by Gradle.

The project uses the [Shadow plugin](https://github.com/johnrengelman/shadow):

```gradle
plugins {
    id 'application'
    id 'com.github.johnrengelman.shadow' version '7.1.2'
}

shadowJar {
    archiveBaseName = "aria"
    archiveClassifier = null
    mergeServiceFiles()
}
```

Build it:

```bash
./gradlew shadowJar
```

The resulting `build/libs/aria.jar` is self-contained. On first run from the JAR, `LlmServer` extracts the model to `./data/model-cache/` automatically — no manual setup required by the user beyond having `llama-server` installed.

> **Note:** `llama-server` itself must still be installed on the target machine (`brew install llama.cpp` on macOS). Only the model weights are bundled; the inference binary is a system dependency.

---

## Step 7: Next Steps

- **Graceful degradation:** `LlmInterpreter.isReady()` returns `false` if `llama-server` is missing or the model fails to load. The app continues to work with exact commands — consider adding a visible status indicator in the UI.
- **Timeout tuning:** The default inference timeout is 15 s, overridable via `-Daria.llm.timeout=N`. Adjust based on your hardware.
- **Swap models:** Change the `MODEL_RESOURCE` constant in `LlmServer.java` and the ChatML examples in `prompt_template.txt` to use a different GGUF model. Larger models give better accuracy at the cost of startup time.
- **Prompt tuning:** Test with varied inputs and expand the `EXAMPLES` section in `prompt_template.txt` to cover edge cases specific to your command set.
- **GPU acceleration:** Set `--n-gpu-layers` to a positive number in `LlmServer.start()` to offload layers to the GPU for faster inference on supported hardware.
