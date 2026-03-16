# Setting Up the Local LLM in Aria

This tutorial explains how Aria's natural language mode works and how to set it up for development and end-user deployment.

**Prerequisites:**
- Completed the [general Aria setup](../README.md)
- Java 17 or later
- A Unix-like terminal (macOS, Linux) or Windows with PowerShell

---

## Overview

Aria uses a **local, offline LLM** (Large Language Model) to interpret natural-language input that its rule-based parser does not recognise. For example, typing *"remind me to submit my assignment by Friday"* is converted to `deadline submit my assignment /by Friday` entirely on your machine — no cloud API, no API key required.

The integration has two components:
- **Model**: [Qwen3.5-0.8B](https://huggingface.co/Qwen/Qwen3-0.5B) (Q4_K_M quantisation, ~508 MB), bundled inside `aria.jar`.
- **Inference engine**: [`llama-server`](https://github.com/ggerganov/llama.cpp) — a system-installed binary that Aria launches as a subprocess and communicates with over a local HTTP port.

> **Graceful degradation**: If `llama-server` is not installed, Aria prints a notice on startup and continues working normally. All exact commands (e.g. `todo`, `deadline`, `list`) are unaffected.

---

## Step 1 — Install llama-server

The `llama-server` binary is the inference engine that loads and runs the model. Install it using your package manager.

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

---

## Step 2 — Download the Model File

The model file is **not** checked into the repository (it is listed in `.gitignore` because of its size). You need to download it manually and place it in the correct location before building.

1. Go to [https://huggingface.co/unsloth/Qwen3.5-0.8B-GGUF](https://huggingface.co/unsloth/Qwen3.5-0.8B-GGUF).
2. In the **Files and versions** tab, find and download **`Qwen3.5-0.8B-Q4_K_M.gguf`** (~508 MB).
3. Place the downloaded file at:
   ```
   src/main/resources/models/Qwen3.5-0.8B-Q4_K_M.gguf
   ```
   Create the `models/` directory if it does not exist.

> **Why Q4_K_M?** This quantisation offers a good balance between model accuracy and file size. Other quantisation levels (e.g. Q2_K, Q8_0) are available on the same page but are not tested with Aria's prompt template.

---

## Step 3 — Build the Fat JAR

The model file (~508 MB) is bundled into the JAR during the build. Run:

```bash
./gradlew shadowJar
```

This produces `build/libs/aria.jar`. The model is packaged at the classpath path `/models/Qwen3.5-0.8B-Q4_K_M.gguf` inside the JAR.

> **Note:** The first time you run the JAR, Aria extracts the model to `./data/model-cache/` on disk. This extraction takes a few seconds and only happens once.

---

## Step 4 — Run Aria

```bash
java -jar build/libs/aria.jar
```

On startup, you will see a **"Loading natural language model…"** message in the chat window and the input field will be disabled while the model loads. Once ready, the message changes to **"Natural language mode ready"** and you can type freely.

Try a natural-language command:
```
remind me to finish the report by next Monday
```
Aria interprets this as `deadline finish the report /by next Monday` and adds it to your task list.

### Optional: customise the inference timeout

If model initialisation or inference is slow on your machine, increase the timeout:
```bash
java -Daria.llm.timeout=30 -jar build/libs/aria.jar
```
The default is 15 seconds per inference call.

---

## Step 5 — Understand What Happens Under the Hood

This section explains the code path for contributors who want to modify or extend the NLP integration.

### Request flow

```
User types natural-language input
         │
         ▼
aria.tryGetResponseDirect(input)   ← exact rule-based Parser — no LLM
         │ success → show result, done
         │ AriaException (unrecognised input)
         ▼
Show "(thinking…)" bubble, disable input field
Start daemon thread → LlmInterpreter.interpret(input)  [timeout: 15 s]
         │
         ├── null  (timeout / UNKNOWN / model not ready)
         │         └─▶ "I'm not sure how to handle that. Type 'help'."
         │
         ├── CHAT: <message>   (greetings, jokes, chitchat)
         │         └─▶ display message directly
         │
         ├── delete / archive / rm   (destructive)
         │         └─▶ ask the user to confirm with "yes" before executing
         │
         └── safe command  (todo, deadline, event, …)
                   └─▶ aria.executeInterpreted(cmd)
                       show "(I interpreted: "<cmd>")\n<result>"
```

### Key classes

| Class | Location | Responsibility |
|-------|----------|----------------|
| `LlmServer` | [aria/nlp/LlmServer.java](../src/main/java/aria/nlp/LlmServer.java) | Finds the `llama-server` binary, starts it as a subprocess, sends HTTP `/completion` requests |
| `LlmInterpreter` | [aria/nlp/LlmInterpreter.java](../src/main/java/aria/nlp/LlmInterpreter.java) | Singleton; loads the prompt template, calls `LlmServer`, validates model output |
| `Aria` | [aria/Aria.java](../src/main/java/aria/Aria.java) | `tryGetResponseDirect()` and `executeInterpreted()` bridge exact parsing and LLM-generated commands |
| `MainWindow` | [aria/ui/MainWindow.java](../src/main/java/aria/ui/MainWindow.java) | Async orchestration: background tasks, input locking, thinking bubble, confirmation dialogs |

### Binary discovery (`LlmServer.findBinary`)

`LlmServer` searches for the executable in this order:
1. `llama-server` on the system `PATH`
2. `/opt/homebrew/bin/llama-server` (Homebrew on Apple Silicon)
3. `/usr/local/bin/llama-server` (Homebrew on Intel)
4. `/usr/bin/llama-server`

If none is found, `start()` returns `false` and Aria falls back to exact commands only.

### Model resolution (`LlmServer.resolveModelFile`)

- **Development (exploded classpath):** the model file is read directly via a `file://` URL — no extraction needed.
- **Fat-JAR:** the model is extracted from the JAR to `./data/model-cache/Qwen3.5-0.8B-Q4_K_M.gguf` on first launch.

### Server parameters

`llama-server` is started with these flags:

| Flag | Value | Purpose |
|------|-------|---------|
| `--model` | extracted model path | model to load |
| `--port` | `38765` | local HTTP port (avoids common conflicts) |
| `--ctx-size` | `2048` | context window |
| `--n-gpu-layers` | `0` | CPU-only inference (broadest compatibility) |
| `--threads` | half of available CPUs | inference thread count |
| `--no-webui --log-disable` | — | suppress server UI and logging |

Aria polls `GET http://127.0.0.1:38765/health` every 500 ms (up to 60 s) to detect when the server is ready.

---

## Step 6 — Understand the Prompt Design

The prompt is stored as a plain-text file at [`src/main/resources/nlp/prompt_template.txt`](../src/main/resources/nlp/prompt_template.txt) and loaded at class-initialisation time by `LlmInterpreter`.

It uses **ChatML format**, which Qwen3 chat models require:

```
<|im_start|>system
… system instructions …
<|im_end|>
<|im_start|>user
{USER_INPUT}
<|im_end|>
<|im_start|>assistant
<think>

</think>
```

Key design choices:

- **Explicit numbered rules** — e.g. *"if input mentions 'by [date]', ALWAYS use `deadline`, never `todo`"* — reduce common model mistakes.
- **~30 few-shot examples** — cover edge cases: plain todos, deadlines with/without dates, events, chitchat, gibberish.
- **Pre-filled `<think>` block** — the opening `<think>\n\n</think>` tag suppresses the model's internal chain-of-thought reasoning, so it emits only the command string.
- **Stop tokens** (`\n`, `<|im_end|>`, `User:`) — force single-line output and prevent the model from continuing into the next turn.
- **Temperature 0.0** — greedy decoding for deterministic, reproducible results.
- **Max 80 tokens** — prevents runaway generation.

To modify the behaviour (e.g. add support for a new command or correct a recurring mistake), edit `prompt_template.txt` — no Java changes are needed.

---

## Step 7 — Output Validation

`LlmInterpreter.validateAndNormalise(String raw)` applies two-phase extraction before the output is used:

**Phase 1 — Direct match:**
If the first word of the model output is in the `KNOWN_COMMANDS` whitelist (e.g. `todo`, `deadline`, `delete`), the string is accepted as-is.

**Phase 2 — Preamble strip:**
If the model prepends conversational filler (e.g. `"Sure! todo buy milk"`), the method scans for the first known command word at a word boundary and extracts from that point. Single-letter aliases (`t`, `d`, `e`, …) are excluded from this scan to avoid false matches inside ordinary English words.

**Special outputs:**
- `UNKNOWN` → returns `null`; Aria shows a friendly error message.
- `CHAT: <message>` → passed through unchanged; displayed directly in the chat window.

The `KNOWN_COMMANDS` set mirrors the full command vocabulary of `Parser.java`. When you add a new command to the parser, add it to `LlmInterpreter.KNOWN_COMMANDS` as well so the validator accepts it.

---

## Troubleshooting

**"Natural language model not available — exact commands still work."**
`llama-server` was not found. Re-run `llama-server --version` in your terminal to confirm it is installed and on the `PATH`, then restart Aria.

**Input field stays disabled after startup.**
The server may have timed out (60 s limit). Check your system resources. You can also try running `llama-server` manually to see its output:
```bash
llama-server --model path/to/Qwen3.5-0.8B-Q4_K_M.gguf --port 38765
```

**Natural language commands are not interpreted correctly.**
Edit [`prompt_template.txt`](../src/main/resources/nlp/prompt_template.txt) to add or adjust few-shot examples for the failing input pattern. Rebuild the JAR.

**Inference is slow.**
Default timeout is 15 s. On low-end hardware, increase it:
```bash
java -Daria.llm.timeout=60 -jar aria.jar
```
Alternatively, enable GPU layers by modifying `LlmServer.java` to pass `--n-gpu-layers 99` if your machine has a supported GPU.

---

## Summary

| What | Detail |
|------|--------|
| Model | Qwen3.5-0.8B, Q4_K_M GGUF, ~508 MB, bundled in JAR |
| Inference engine | `llama-server` (llama.cpp), installed separately |
| Server port | `localhost:38765` |
| Model cache | `./data/model-cache/` (extracted on first run from fat-JAR) |
| Startup timeout | 60 s |
| Inference timeout | 15 s (override: `-Daria.llm.timeout=N`) |
| Prompt template | `src/main/resources/nlp/prompt_template.txt` |
| Key classes | `aria.nlp.LlmServer`, `aria.nlp.LlmInterpreter` |
