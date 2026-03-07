# Aria User Guide

Aria is a personal task manager chatbot that helps you track todos, deadlines, and events —
fast and from the command line (or GUI).

![Aria Screenshot](screenshot.png)

---

## Quick Start

1. Ensure you have Java 17 installed.
2. Download `duke.jar` from the latest release.
3. Run: `java -jar duke.jar`
4. Type commands in the chat box and press **Send** or **Enter**.

---

## Features

### Adding a Todo

Adds a simple task with no date.

**Format:** `todo DESCRIPTION`

**Example:** `todo read book`

```
Got it. I've added this task:
  [T][ ] read book
Now you have 1 tasks in the list.
```

---

### Adding a Deadline

Adds a task with a due date.

**Format:** `deadline DESCRIPTION /by DATE`

DATE can be a natural expression: `tomorrow`, `next Monday`, `Dec 25`, or `yyyy-MM-dd`.

**Example:** `deadline submit report /by 2024-12-01`

```
Got it. I've added this task:
  [D][ ] submit report (by: Dec 01 2024)
Now you have 2 tasks in the list.
```

---

### Adding an Event

Adds a task with a start and end time.

**Format:** `event DESCRIPTION /from TIME /to TIME`

**Example:** `event project meeting /from Mon 2pm /to 4pm`

```
Got it. I've added this task:
  [E][ ] project meeting (from: Mon 2pm to: 4pm)
Now you have 3 tasks in the list.
```

---

### Listing All Tasks

**Format:** `list`

```
Here are the tasks in your list:
1.[T][ ] read book
2.[D][ ] submit report (by: Dec 01 2024)
3.[E][ ] project meeting (from: Mon 2pm to: 4pm)
```

---

### Marking a Task as Done

**Format:** `mark INDEX`

**Example:** `mark 1`

```
Nice! I've marked this task as done:
  [T][X] read book
```

---

### Unmarking a Task

**Format:** `unmark INDEX`

**Example:** `unmark 1`

```
OK, I've marked this task as not done yet:
  [T][ ] read book
```

---

### Deleting a Task

**Format:** `delete INDEX`

**Example:** `delete 2`

```
Noted. I've removed this task:
  [D][ ] submit report (by: Dec 01 2024)
Now you have 2 tasks in the list.
```

---

### Searching for Tasks

**Format:** `find KEYWORD`

**Example:** `find book`

```
Here are the matching tasks in your list:
1.[T][ ] read book
```

---

### Viewing Help

**Format:** `help`

Displays a summary of all available commands.

---

### Exiting

**Format:** `bye`

---

## Data Storage

Tasks are automatically saved to `./data/duke.txt` after every change.
They are loaded back on the next launch.

---

## AI Assistance Acknowledgement

Portions of this project were developed with the assistance of
[Claude Code](https://claude.ai/code) (Anthropic). AI assistance was used for:
- Boilerplate code generation and refactoring
- Writing JUnit test cases
- JavaDoc documentation

All AI-generated code was reviewed, tested, and verified by the developer.
