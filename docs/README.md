# Aria User Guide

Aria is a personal task manager chatbot that helps you track todos, deadlines, events, and more —
fast and from the command line (or GUI).

![Aria Screenshot](screenshot.png)

---

## Quick Start

1. Ensure you have Java 17 installed.
2. Download `duke.jar` from the latest release.
3. Run: `java -jar duke.jar`
4. Type commands in the chat box and press **Send** or **Enter**.

Optional: specify a custom data file path:
```
java -jar duke.jar /path/to/my-tasks.txt
```

---

## Task Types

| Symbol | Type              | Command    |
|--------|-------------------|------------|
| `[T]`  | Todo              | `todo`     |
| `[D]`  | Deadline          | `deadline` |
| `[E]`  | Event             | `event`    |
| `[TE]` | Tentative Event   | `tevent`   |
| `[W]`  | Within-Period     | `within`   |
| `[F]`  | Fixed Duration    | `fixed`    |

Each task displays: `[TYPE][done][priority] description`
Priority symbols: `H` = High, `M` = Medium, `L` = Low.

---

## Features

### Adding a Todo

**Format:** `todo DESCRIPTION [/priority high|medium|low] [/every daily|weekly|monthly] [/after INDEX]`

**Example:** `todo read book /priority high`

```
Got it. I've added this task:
  [T][ ][H] read book
Now you have 1 tasks in the list.
```

---

### Adding a Deadline

**Format:** `deadline DESCRIPTION /by DATE [/priority high|medium|low]`

DATE supports natural language: `tomorrow`, `next Monday`, `Dec 25`, or `yyyy-MM-dd`.

**Example:** `deadline submit report /by 2024-12-01`

```
Got it. I've added this task:
  [D][ ][M] submit report (by: Dec 01 2024)
Now you have 2 tasks in the list.
```

---

### Adding an Event

**Format:** `event DESCRIPTION /from TIME /to TIME [/priority high|medium|low]`

**Example:** `event project meeting /from 2024-12-10 /to 2024-12-10`

```
Got it. I've added this task:
  [E][ ][M] project meeting (from: Dec 10 2024 to: Dec 10 2024)
Now you have 3 tasks in the list.
```

---

### Adding a Tentative Event

Create an event with multiple candidate slots, then confirm one later.

**Format:** `tevent DESCRIPTION /slot1 TIME /slot2 TIME ...`

**Example:** `tevent team lunch /slot1 2024-12-15 /slot2 2024-12-16`

```
Got it. I've added a tentative event:
  [TE][ ][M] team lunch (slots: 2024-12-15, 2024-12-16)
```

**Confirm a slot:** `confirm INDEX SLOT_NUMBER`

**Example:** `confirm 4 1` → converts slot 1 to a confirmed Event.

---

### Adding a Within-Period Task

A task that must be done within a date range.

**Format:** `within DESCRIPTION /from DATE /to DATE`

**Example:** `within buy birthday gift /from 2024-12-20 /to 2024-12-24`

```
Got it. I've added this task:
  [W][ ][M] buy birthday gift (do within: Dec 20 2024 to Dec 24 2024)
```

---

### Adding a Fixed-Duration Task

A task with a known time requirement but no fixed schedule.

**Format:** `fixed DESCRIPTION /duration HOURS`

**Example:** `fixed study for exam /duration 3`

```
Got it. I've added this task:
  [F][ ][M] study for exam (duration: 3h)
```

---

### Listing All Tasks

**Format:** `list`

---

### Marking / Unmarking Tasks

**Format:** `mark INDEX [INDEX ...]`  or  `mark RANGE` (e.g. `mark 1-3`)

**Format:** `unmark INDEX [INDEX ...]`

---

### Deleting Tasks

**Format:** `delete INDEX [INDEX ...]`  or  `delete RANGE`

---

### Updating a Task

**Format:** `update INDEX [/desc NEW_DESC] [/by NEW_DATE] [/from NEW_TIME] [/to NEW_TIME] [/priority high|medium|low]`

**Example:** `update 2 /by 2024-12-15 /priority high`

---

### Searching for Tasks

**Format:** `find KEYWORD`

Supports special filters:
- `find #tagname` — by tag
- `find /type todo|deadline|event` — by type
- `find /priority high|medium|low` — by priority
- `find plain text` — description substring match

---

### Sorting Tasks

**Format:** `sort name|priority|date`

---

### Tagging Tasks

**Format:** `tag INDEX TAGNAME`

**Format:** `untag INDEX TAGNAME`

**Example:** `tag 1 urgent`  → task 1 displays as `... #urgent`

---

### Archiving Tasks

Move a completed task to the archive:

**Format:** `archive INDEX`

View archived tasks:

**Format:** `archives`

---

### Task Statistics

**Format:** `stats`

```
Task Statistics:
  Total:     5
  Done:      2 (40%)
  Undone:    3
  Todos:     2
  Deadlines: 2
  Events:    1
```

---

### Snoozing a Deadline or Event

**Format:** `snooze INDEX NEWDATE`

**Example:** `snooze 2 next Monday`

---

### Viewing Schedule for a Date

**Format:** `schedule DATE`

**Example:** `schedule 2024-12-10`

---

### Finding Free Time

**Format:** `freetime DATE DURATION_HOURS`

**Example:** `freetime 2024-12-10 2`  → shows 2-hour free windows within 08:00–22:00.

---

### Recurring Tasks

Add `/every daily|weekly|monthly` to a todo:

**Format:** `todo DESCRIPTION /every weekly`

When marked done, the next occurrence is automatically added.

---

### Do-After Dependency

Link a todo to a prerequisite task:

**Format:** `todo DESCRIPTION /after INDEX`

A warning appears when marking the task done if its prerequisite is not yet done.

---

### Undo Last Action

**Format:** `undo`

Reverts the last mutating command (add, delete, mark, etc.).

---

### Help

**Format:** `help`

Displays a summary of available commands.

---

### Exiting

**Format:** `bye`

Aliases: `q`, `exit`

---

## Startup Reminders

On launch, Aria automatically shows any tasks due within the next 3 days.

---

## Command Aliases

| Alias | Command    |
|-------|------------|
| `t`   | `todo`     |
| `d`   | `deadline` |
| `e`   | `event`    |
| `ls`  | `list`     |
| `rm`  | `delete`   |
| `q`   | `bye`      |

Commands are case-insensitive.

---

## Data Storage

- Tasks: automatically saved to `./data/duke.txt` after every change.
- Archives: stored in `./data/archive.txt`.
- Data is loaded back automatically on the next launch.
- Corrupted or unrecognised lines are silently skipped.

---

## AI Assistance Acknowledgement

Portions of this project were developed with the assistance of
[Claude Code](https://claude.ai/code) (Anthropic). AI assistance was used for:
- Boilerplate code generation and refactoring
- Writing JUnit test cases
- JavaDoc documentation

All AI-generated code was reviewed, tested, and verified by the developer.
