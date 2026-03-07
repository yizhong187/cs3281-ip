# Aria Developer Guide

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Package Structure](#package-structure)
3. [Component Descriptions](#component-descriptions)
4. [Task Type Hierarchy](#task-type-hierarchy)
5. [Command Lifecycle](#command-lifecycle)
6. [Storage Format](#storage-format)
7. [Key Design Decisions](#key-design-decisions)
8. [Testing](#testing)

---

## Architecture Overview

Aria follows a layered MVC-style architecture:

```
┌──────────────────────────────────────────────────────────┐
│                        UI Layer                          │
│          duke.ui  (Ui, MainWindow, DialogBox)            │
└───────────────────────┬──────────────────────────────────┘
                        │ user input / output
┌───────────────────────▼──────────────────────────────────┐
│                     Core Layer                           │
│   duke  (Duke orchestrator, DukeException)               │
│   duke.command  (Parser → Command → CommandType)         │
└──────────────┬────────────────────────┬──────────────────┘
               │ reads/writes           │ reads/writes
┌──────────────▼──────┐   ┌────────────▼─────────────────┐
│   duke.task         │   │   duke.storage               │
│ Task hierarchy,     │   │ Storage (file I/O)            │
│ TaskList            │   │                              │
└─────────────────────┘   └──────────────────────────────┘
```

**Duke** is the orchestrator: it initialises all components and drives the CLI
event loop (`run()`) and GUI callback (`getResponse()`).

---

## Package Structure

```
src/main/java/
└── duke/
    ├── Duke.java               # Application orchestrator
    ├── DukeException.java      # Checked application exception
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

### Duke (Orchestrator)

- Owns `TaskList`, `Storage`, `Ui`, and the undo `Deque<String>`.
- **CLI mode**: `run()` loops on `ui.readCommand()`, parses, executes.
- **GUI mode**: `getResponse(String)` is called per user message from `MainWindow`.
- Snapshots storage before each mutating command to support `undo`.
- `buildReminderMessage()` returns tasks due within 3 days (shown on startup).

### Parser

- `parse(String input)` → `Command` or throws `DukeException`.
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
contents onto a `Deque<String>` in Duke. The `undo` command pops the last
snapshot and calls `storage.restoreFromSnapshot(snapshot)`, which rewrites the
file and reloads the task list.

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

Tests are in `src/test/java/duke/`.

- **DukeTest** — integration tests via `Duke.getResponse()`; covers all major command flows.
- **ParserTest** — unit tests for `Parser.parse()`; covers valid inputs and error cases.
- **StorageTest** — unit tests for `Storage.load()` and `Storage.save()`; covers round-trips,
  malformed lines, and edge cases.
- **GUI tests** (Part1–4) — TestFX-based tests verifying GUI components load correctly.

Run all tests: `./gradlew test`

Run checkstyle: `./gradlew checkstyleMain`

Build JAR: `./gradlew shadowJar` → produces `build/libs/duke.jar`
