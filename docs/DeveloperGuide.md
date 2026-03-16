# Aria Developer Guide

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Package Structure](#package-structure)
3. [Component Descriptions](#component-descriptions)
4. [Task Type Hierarchy](#task-type-hierarchy)
5. [Command Lifecycle](#command-lifecycle)
6. [Natural Language (LLM) Integration](#natural-language-llm-integration)
7. [Storage Format](#storage-format)
8. [Key Design Decisions](#key-design-decisions)
9. [Testing](#testing)

---

## Architecture Overview

Aria follows a layered MVC-style architecture:

```
┌──────────────────────────────────────────────────────────┐
│                        UI Layer                          │
│          aria.ui  (Ui, MainWindow, DialogBox)            │
└───────────────────────┬──────────────────────────────────┘
                        │ user input / output
┌───────────────────────▼──────────────────────────────────┐
│                     Core Layer                           │
│   aria  (Aria orchestrator, AriaException)               │
│   aria.command  (Parser → Command → CommandType)         │
└──────────────┬────────────────────────┬──────────────────┘
               │ reads/writes           │ reads/writes
┌──────────────▼──────┐   ┌────────────▼─────────────────┐
│   aria.task         │   │   aria.storage               │
│ Task hierarchy,     │   │ Storage (file I/O)            │
│ TaskList            │   │                              │
└─────────────────────┘   └──────────────────────────────┘
```

**Aria** is the orchestrator: it initialises all components and drives the CLI
event loop (`run()`) and GUI callback (`getResponse()`).

---

## Package Structure

```
src/main/java/
└── aria/
    ├── Aria.java               # Application orchestrator
    ├── AriaException.java      # Checked application exception
    ├── Launcher.java           # JavaFX launch shim
    ├── command/
    │   ├── Command.java        # Executable command with parameters
    │   ├── CommandType.java    # Enum of all command types
    │   └── Parser.java         # Parses raw input → Command
    ├── storage/
    │   └── Storage.java        # Loads/saves tasks to disk
    ├── task/
    │   ├── Task.java           # Abstract base class
    │   ├── Todo.java
    │   ├── Deadline.java
    │   ├── Event.java
    │   ├── TentativeEvent.java
    │   ├── WithinPeriodTask.java
    │   ├── FixedDurationTask.java
    │   ├── TaskList.java       # Collection wrapper + queries
    │   └── Priority.java       # HIGH / MEDIUM / LOW enum
    ├── ui/
    │   ├── Ui.java             # CLI I/O
    │   ├── MainWindow.java     # JavaFX root controller
    │   └── DialogBox.java      # Chat bubble component
    └── util/
        └── DateParser.java     # Natty-based date parsing
```

---

## Component Descriptions

### Aria (Orchestrator)

- Owns `TaskList`, `Storage`, `Ui`, and the undo `Deque<String>`.
- **CLI mode**: `run()` loops on `ui.readCommand()`, parses, executes.
- **GUI mode**: `getResponse(String)` is called per user message from `MainWindow`.
- Snapshots storage before each mutating command to support `undo`.
- `buildReminderMessage()` returns tasks due within 3 days (shown on startup).

### Parser

- `parse(String input)` → `Command` or throws `AriaException`.
- Resolves aliases via `resolveAlias()` (e.g. `t` → `todo`).
- Extracts optional flags: `/priority`, `/every`, `/after` via regex helpers.
- Returns immutable `Command` objects (fields set via setter methods for optional flags).

### Command

- Holds type + all parsed parameters (`description`, `by`, `from`, `to`, `priority`, `recurrence`, `afterIndex`).
- Two execution paths:
  - `execute(taskList, ui, storage)` — CLI path, calls Ui methods.
  - `getOutput(taskList, storage)` — GUI path, returns a String.
- `isMutating()` — returns true for commands that modify the task list.
- Private helpers: `parseIndex`, `parseIndices` (supports ranges like `1-3`),
  `buildTask`, `applyUpdates`, `spawnNextRecurrence`, `advanceDate`.

### Storage

- Pipe-delimited format: fields separated by ` | `.
- `load()` — reads file, skips blank/malformed lines.
- `save(TaskList)` — overwrites file with all tasks.
- `appendToArchive(Task)` / `loadArchive()` — manages `archive.txt`.
- `getSnapshot()` / `restoreFromSnapshot(String)` — string-based undo support.

### TaskList

- Wraps `ArrayList<Task>` with assertion-guarded `add`, `get`, `remove`, `set`.
- `find(String)` — supports `#tag`, `/type`, `/priority`, plain text.
- `sortBy(String)` — sorts by "name", "priority", or "date".
- `getScheduleOn(LocalDate)` — returns deadlines/events on a given date.
- `getUpcomingReminders(int days)` — tasks due within N days.
- `findOverlappingEvents(Event, int excludeIndex)` — conflict detection.
- `hasDuplicate(String)` — case-insensitive description match.

### DateParser (Utility)

- Wraps the Natty library (`com.joestelmach:natty:0.13`).
- `parse(String)` → `LocalDate` or null if unparseable.
- Used by `Deadline`, `Event`, and `WithinPeriodTask` constructors.

---

## Task Type Hierarchy

```
Task (abstract)
├── Todo                  T  — description only
├── Deadline              D  — description + by date
├── Event                 E  — description + from + to
├── TentativeEvent        TE — description + list of slots
├── WithinPeriodTask      W  — description + from + to date range
└── FixedDurationTask     F  — description + duration in hours
```

All tasks inherit:
- `priority` (Priority enum, default MEDIUM)
- `tags` (LinkedHashSet<String>)
- `recurrence` (String: "daily"/"weekly"/"monthly", or null)
- `afterIndex` (int: 1-based prerequisite task index, or -1)

---

## Command Lifecycle

```
User input
   │
   ▼
Parser.parse(input)
   │  resolves aliases, splits tokens, extracts flags
   ▼
Command (type + params)
   │
   ▼  isMutating? → snapshot storage for undo
   │
   ├── CLI: command.execute(taskList, ui, storage)
   │         └── calls ui.show*() methods
   │
   └── GUI: command.getOutput(taskList, storage)
             └── returns String shown in DialogBox
```

### Undo Mechanism

Before each mutating command, `storage.getSnapshot()` pushes the current file
contents onto a `Deque<String>` in Aria. The `undo` command pops the last
snapshot and calls `storage.restoreFromSnapshot(snapshot)`, which rewrites the
file and reloads the task list.

---

## Natural Language (LLM) Integration

Aria uses a **local LLM as a fallback interpreter**: exact command parsing runs first, and only
unrecognised input is sent to the model. Known commands have zero added latency.

### Architecture

```
User input
    │
    ▼
aria.tryGetResponseDirect(input)    ← exact Parser.parse() — no LLM involved
    │ success → show result, done
    │ AriaException (unknown command)
    ▼
Show "(thinking...)" bubble, disable input field
Start daemon thread → LlmInterpreter.interpret(input)   [timeout: 15 s]
    │
    ├── null  (timeout / UNKNOWN / model not ready)
    │       └─▶ "I'm not sure… type 'help'"
    │
    ├── CHAT: <message>   (greetings, jokes, chitchat)
    │       └─▶ display message directly
    │
    ├── delete / archive / rm   (destructive)
    │       └─▶ store in pendingLlmCommand, ask for confirmation
    │               next input "yes" → execute | anything else → cancel
    │
    └── safe command  (todo, deadline, event, …)
            └─▶ aria.executeInterpreted(cmd)
                show "(I interpreted: "cmd")\n<result>"
```

### Key Classes

| Class | Responsibility |
|-------|---------------|
| `aria.nlp.LlmServer` | Manages the `llama-server` subprocess; exposes `complete(prompt, maxTokens)` via its HTTP `/completion` endpoint |
| `aria.nlp.LlmInterpreter` | Singleton wrapper; builds the prompt, calls `LlmServer`, validates and normalises the raw output |
| `aria.Aria` | Adds `tryGetResponseDirect()` (throws `AriaException` on unknown input) and `executeInterpreted()` (runs a pre-validated LLM command) |
| `aria.ui.MainWindow` | Drives the async flow: `Task<Void>` for model init, `Task<String>` for each LLM call, input locking, thinking bubble, confirmation dialog |

### Setup and Model Resolution

**Model**: Qwen3.5-0.8B (Q4_K_M quantisation, ~508 MB GGUF), bundled in `aria.jar` as a
classpath resource at `/models/Qwen3.5-0.8B-Q4_K_M.gguf`.

**Inference engine**: The system-installed `llama-server` binary (from llama.cpp) is used as a
subprocess, communicating over HTTP on `localhost:38765`. This avoids JNI native library
compatibility issues.

**Model resolution** (`LlmServer.resolveModelFile()`):
- Dev mode (exploded classpath): uses the model file directly via `file://` URL.
- Fat-JAR mode: extracts the model to `./data/model-cache/` on first launch.

**Binary discovery** (`LlmServer.findBinary()`): probes `llama-server` on `PATH`, then
`/opt/homebrew/bin/`, `/usr/local/bin/`, `/usr/bin/` in order. Returns `null` if none found —
the app falls back gracefully to exact command parsing.

### Startup Flow

On `MainWindow.setAria()`:
1. Shows welcome message and startup reminders.
2. Adds a *"Loading natural language model…"* bubble and **disables the input field**.
3. Starts a `Task<Void>` daemon thread calling `LlmInterpreter.init()`.
4. On success: removes the bubble, shows *"Natural language mode ready"* or *"model not available"*, re-enables input.

### Prompt Design

The prompt uses ChatML format (required for Qwen3 chat models) with:

- **Explicit numbered rules** — e.g. "if input mentions `by [date]`, ALWAYS use `deadline`".
- **Many few-shot examples** — covering edge cases (prefix stripping for "add todo X", todo vs
  deadline with/without a date, event format, chitchat, gibberish).
- **Pre-filled `<think>\n\n</think>`** — skips the model's internal reasoning step so only the
  command string is emitted.
- **Stop tokens**: `\n`, `<|im_end|>`, `User:` — forces single-line output.
- **Temperature 0.0** — greedy decoding for deterministic results.
- **Max 80 tokens** — prevents runaway generation.

### Output Validation (`LlmInterpreter.validateAndNormalise`)

Two-phase extraction:
1. **Direct match**: first word must be in the `KNOWN_COMMANDS` whitelist.
2. **Preamble strip**: if the model prepends text (e.g. "Sure! todo buy milk"), scan for the
   first known command word at a word boundary and extract from there. Single-letter aliases
   are excluded from this scan to avoid false matches inside ordinary words.

Special outputs:
- `UNKNOWN` → returns `null` (friendly error shown to user).
- `CHAT: <message>` → passed through as a conversational response.

### Safeguards

| Safeguard | Mechanism |
|-----------|-----------|
| Command whitelist | `validateAndNormalise()` rejects any output whose first word is not a known command |
| Destructive confirmation | `delete`/`archive`/`rm` from LLM require the user to type "yes" before executing |
| Inference timeout | `CompletableFuture.get(timeout, SECONDS)` — default 15 s, overridable via `-Daria.llm.timeout=N` |
| Input locking | TextField + Send button disabled while LLM runs; re-enabled in both success and failure handlers |
| Graceful degradation | If `llama-server` is not installed or `init()` fails, `isReady()` stays false and `interpret()` returns `null` immediately; all exact commands continue to work |

---

## Storage Format

Each task is one line. Fields are separated by ` | `.

| Type | Format |
|------|--------|
| T    | `T \| done \| desc \| PRIORITY \| tags \| recurrence \| afterIndex` |
| D    | `D \| done \| desc \| by \| PRIORITY \| tags \| recurrence \| afterIndex` |
| E    | `E \| done \| desc \| from \| to \| PRIORITY \| tags \| recurrence \| afterIndex` |
| TE   | `TE \| done \| desc \| slot1\|\|slot2\|\|... \| PRIORITY \| tags \| recurrence \| afterIndex` |
| W    | `W \| done \| desc \| from \| to \| PRIORITY \| tags \| recurrence \| afterIndex` |
| F    | `F \| done \| desc \| durationHours \| PRIORITY \| tags \| recurrence \| afterIndex` |

- `done`: `1` = done, `0` = not done.
- `PRIORITY`: `HIGH`, `MEDIUM`, or `LOW`.
- `tags`: comma-separated, e.g. `urgent,work`. Empty string if none.
- `recurrence`: `daily`, `weekly`, or `monthly`. Empty string if none.
- `afterIndex`: 1-based prerequisite index. Empty string if none.

Fields beyond the minimum are optional for backward compatibility: missing fields
default to MEDIUM priority, no tags, no recurrence, no dependency.

---

## Key Design Decisions

### Two Execution Paths (CLI vs GUI)

`Command` exposes both `execute()` (CLI, uses Ui) and `getOutput()` (GUI, returns String).
This avoids duplicating logic while keeping the UI layer decoupled.

### Snapshot-Based Undo

Rather than deep-copying task objects, undo stores the serialised file content as a String.
This is simple and correct — if the file can be written, it can be snapshotted and restored.
Trade-off: undo history is limited to the in-memory Deque (lost on restart).

### Backward-Compatible File Format

Each new field (priority, tags, recurrence, afterIndex) is appended at the end.
`Storage.parseTask()` checks `parts.length > index` before reading each optional field,
so old data files are loaded correctly.

### Natural Language Dates via Natty

All date strings (deadlines, events, snooze) go through `DateParser.parse()`, which
delegates to the Natty library. The raw string is always stored in the file; the parsed
`LocalDate` is used only for display and date-based queries.

---

## Testing

Tests are in `src/test/java/aria/`.

- **AriaTest** — integration tests via `Aria.getResponse()`; covers all major command flows.
- **ParserTest** — unit tests for `Parser.parse()`; covers valid inputs and error cases.
- **StorageTest** — unit tests for `Storage.load()` and `Storage.save()`; covers round-trips,
  malformed lines, and edge cases.
- **GUI tests** (Part1–4) — TestFX-based tests verifying GUI components load correctly.

Run all tests: `./gradlew test`

Run checkstyle: `./gradlew checkstyleMain`

Build JAR: `./gradlew shadowJar` → produces `build/libs/aria.jar`
