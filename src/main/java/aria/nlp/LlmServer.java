package aria.nlp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages a {@code llama-server} subprocess (from the system's llama.cpp installation)
 * and exposes a single {@link #complete(String, int)} method that calls its
 * OpenAI-compatible {@code /completion} HTTP endpoint.
 *
 * <p>The server binary is located by searching common install paths:
 * <ol>
 *   <li>{@code llama-server} on the system {@code PATH}</li>
 *   <li>{@code /opt/homebrew/bin/llama-server} (Homebrew on Apple Silicon)</li>
 *   <li>{@code /usr/local/bin/llama-server} (Homebrew on Intel)</li>
 * </ol>
 *
 * <p>The GGUF model is resolved from the classpath resource
 * {@code /models/Qwen3.5-0.8B-Q4_K_M.gguf}.  When running from an exploded
 * build the file path is used directly; inside a fat-JAR the model is extracted
 * to {@code ./data/model-cache/} on first use.
 *
 * <p>Install the binary on macOS with: {@code brew install llama.cpp}
 */
public class LlmServer {

    private static final Logger LOGGER = Logger.getLogger(LlmServer.class.getName());

    private static final String MODEL_RESOURCE = "/models/Qwen3.5-0.8B-Q4_K_M.gguf";
    private static final String CACHE_DIR = "./data/model-cache";
    private static final String CACHED_MODEL_NAME = "Qwen3.5-0.8B-Q4_K_M.gguf";

    /** Port used by the embedded llama-server. Chosen to avoid common conflicts. */
    static final int SERVER_PORT = 38765;

    private static final String BASE_URL = "http://127.0.0.1:" + SERVER_PORT;
    private static final int STARTUP_TIMEOUT_SECONDS = 60;
    private static final int REQUEST_TIMEOUT_SECONDS = 20;

    /** Binary names / paths to probe in order. */
    private static final String[] CANDIDATE_PATHS = {
        "llama-server",
        "/opt/homebrew/bin/llama-server",
        "/usr/local/bin/llama-server",
        "/usr/bin/llama-server"
    };

    private Process serverProcess;
    private HttpClient httpClient;
    private boolean running = false;

    /**
     * Starts the {@code llama-server} subprocess.
     * Blocks until the server is ready (up to {@value #STARTUP_TIMEOUT_SECONDS} s).
     *
     * @return {@code true} if the server started successfully, {@code false} otherwise
     */
    public boolean start() {
        String binary = findBinary();
        if (binary == null) {
            LOGGER.warning("llama-server not found. Install with: brew install llama.cpp");
            return false;
        }

        File modelFile;
        try {
            modelFile = resolveModelFile();
        } catch (IOException e) {
            LOGGER.warning("Cannot resolve model file: " + e.getMessage());
            return false;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    binary,
                    "--model", modelFile.getAbsolutePath(),
                    "--port", String.valueOf(SERVER_PORT),
                    "--ctx-size", "2048",
                    "--n-gpu-layers", "0",
                    "--threads", String.valueOf(Math.max(1, Runtime.getRuntime().availableProcessors() / 2)),
                    "--no-webui",
                    "--log-disable"
            );
            pb.redirectErrorStream(true);   // merge stderr into stdout so it doesn't block
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            serverProcess = pb.start();

            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            if (!waitForReady()) {
                stop();
                return false;
            }

            running = true;
            LOGGER.info("llama-server ready on port " + SERVER_PORT
                    + " with model: " + modelFile.getName());
            return true;

        } catch (IOException e) {
            LOGGER.warning("Failed to start llama-server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sends a completion request to the running server.
     *
     * @param prompt the full prompt string
     * @param maxTokens maximum number of tokens to generate
     * @return the generated text, or {@code null} if the call fails
     */
    public String complete(String prompt, int maxTokens) {
        if (!running) {
            return null;
        }

        // Build a minimal JSON body — escape backslashes, quotes, and control characters.
        String escapedPrompt = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        String body = "{"
                + "\"prompt\":\"" + escapedPrompt + "\","
                + "\"n_predict\":" + maxTokens + ","
                + "\"temperature\":0.0,"
                + "\"repeat_penalty\":1.0,"
                + "\"stop\":[\"\\n\",\"<|im_end|>\",\"User:\"],"
                + "\"stream\":false"
                + "}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(BASE_URL + "/completion"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseContent(response.body());
            }
            LOGGER.warning("llama-server returned HTTP " + response.statusCode());
            return null;

        } catch (IOException | InterruptedException | URISyntaxException e) {
            LOGGER.warning("llama-server request failed: " + e.getMessage());
            return null;
        }
    }

    /** Returns {@code true} if the server subprocess is running. */
    public boolean isRunning() {
        return running;
    }

    /**
     * Shuts down the server subprocess. Safe to call multiple times.
     */
    public void stop() {
        running = false;
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroy();
            serverProcess = null;
            LOGGER.info("llama-server stopped.");
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Probes candidate binary locations and returns the first one that is executable.
     * Returns {@code null} if none is found.
     */
    private String findBinary() {
        for (String candidate : CANDIDATE_PATHS) {
            // Absolute paths: just check existence.
            if (candidate.startsWith("/")) {
                if (new File(candidate).canExecute()) {
                    return candidate;
                }
                continue;
            }
            // Relative / bare name: try to locate via PATH using a quick probe.
            try {
                Process probe = new ProcessBuilder(candidate, "--version")
                        .redirectErrorStream(true)
                        .start();
                probe.getInputStream().readAllBytes();
                probe.destroy();
                return candidate;   // didn't throw → binary is in PATH
            } catch (IOException ignored) {
                // not found on PATH, try next
            }
        }
        return null;
    }

    /**
     * Resolves the GGUF model file from classpath resources.
     * Uses the file directly for {@code file://} URLs (dev mode);
     * extracts to {@value #CACHE_DIR} for fat-JAR deployments.
     */
    private File resolveModelFile() throws IOException {
        URL resourceUrl = LlmServer.class.getResource(MODEL_RESOURCE);
        if (resourceUrl == null) {
            throw new IOException("Model not on classpath: " + MODEL_RESOURCE);
        }
        if ("file".equals(resourceUrl.getProtocol())) {
            try {
                return new File(resourceUrl.toURI());
            } catch (URISyntaxException e) {
                return new File(resourceUrl.getPath());
            }
        }
        // Fat-JAR: extract to local cache directory.
        File cacheDir = new File(CACHE_DIR);
        cacheDir.mkdirs();
        File dest = new File(cacheDir, CACHED_MODEL_NAME);
        if (!dest.exists()) {
            LOGGER.info("Extracting model to cache: " + dest.getAbsolutePath());
            try (InputStream in = LlmServer.class.getResourceAsStream(MODEL_RESOURCE)) {
                if (in == null) {
                    throw new IOException("Cannot open model resource stream");
                }
                Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return dest;
    }

    /**
     * Polls {@code GET /health} until the server responds, up to
     * {@value #STARTUP_TIMEOUT_SECONDS} seconds.
     *
     * @return {@code true} if the server became ready in time
     */
    private boolean waitForReady() {
        long deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_SECONDS * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (!serverProcess.isAlive()) {
                LOGGER.warning("llama-server process exited prematurely.");
                return false;
            }
            try {
                HttpRequest ping = HttpRequest.newBuilder()
                        .uri(new URI(BASE_URL + "/health"))
                        .timeout(Duration.ofSeconds(2))
                        .GET()
                        .build();
                HttpResponse<Void> resp = httpClient.send(
                        ping, HttpResponse.BodyHandlers.discarding());
                if (resp.statusCode() == 200) {
                    return true;
                }
            } catch (Exception ignored) {
                // server not ready yet
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        LOGGER.warning("llama-server did not become ready within "
                + STARTUP_TIMEOUT_SECONDS + "s.");
        return false;
    }

    /**
     * Extracts the {@code "content"} field from the JSON response body.
     * Uses a simple regex that handles common escape sequences in the value.
     *
     * @param json the raw JSON response string
     * @return the content string, or {@code null} if not found
     */
    static String parseContent(String json) {
        Pattern p = Pattern.compile("\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            // Unescape the most common JSON escape sequences.
            return m.group(1)
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .trim();
        }
        return null;
    }
}
