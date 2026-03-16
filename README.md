# Aria — Your Personal Task Manager

> "Stay organised. Stay ahead."

Aria is a fast, keyboard-driven task manager that lives in your terminal or as a sleek chat-style GUI. Add tasks, set deadlines, track events, manage contacts and expenses — all with simple, natural commands or plain English.

---

## Features at a Glance

- **Natural language input** — just describe what you want; Aria's built-in LLM figures out the command
- **Tasks** — todos, deadlines, events, fixed-duration tasks, and do-within-period tasks
- **Smart dates** — natural language parsing ("next Monday", "tomorrow", "Dec 25")
- **Priority levels** — HIGH / MEDIUM / LOW, sortable
- **Tags** — flexible `#tag` system with tag-based search
- **Reminders** — upcoming deadlines shown on every launch
- **Undo** — instantly reverse your last action
- **Archives** — move completed tasks out of your main list
- **Statistics** — see your completion rate at a glance
- **Entity management** — notes, contacts, expenses, loans, places, trivia, clients, and merchandise
- **Command aliases** — `t`, `d`, `e`, `ls`, `rm`, `q` for speed

---

## Quick Start

**Prerequisites:** Java 17+, and for NLP mode: install `llama-server` ([see instructions below](#nlp-mode-prerequisites))

```
java -jar aria.jar
```

Or specify a custom data file:
```
java -jar aria.jar /path/to/my-tasks.txt
```

The standalone JAR (~520 MB) includes the bundled LLM model. On first launch it extracts the model to `./data/model-cache/` and starts a local inference server — no internet connection required.

> **NLP mode is optional.** If `llama-server` is not installed, Aria falls back to exact command parsing and all core features continue to work normally.

---

## Natural Language Mode

Once the model loads, you can type in plain English instead of exact commands:

| You type | Aria does |
|---|---|
| `remind me to call the dentist` | `todo call the dentist` |
| `finish the report by next Friday` | `deadline finish the report /by next Friday` |
| `meeting with John tmr 9am to 11am` | `event meeting with John /from tomorrow 9am /to tomorrow 11am` |
| `remove task 3` | `delete 3` (with confirmation) |
| `hello` | friendly greeting |
| `tell me a joke` | a productivity-themed joke |

Aria shows what it interpreted so you always know what was understood.

---

## NLP Mode Prerequisites

Install `llama-server` for your platform:

| Platform | Command |
|---|---|
| macOS | `brew install llama.cpp` |
| Linux (Debian/Ubuntu) | `sudo apt install llama-cpp` |
| Windows / other | Download a pre-built binary from the [llama.cpp releases page](https://github.com/ggerganov/llama.cpp/releases) and add it to your `PATH` |

> **NLP mode is optional.** If `llama-server` is not on your `PATH`, Aria still works normally for all exact commands — type `help` to see them.

---

## Setting Up in IntelliJ IDEA

1. Open IntelliJ. If a project is already open, go to `File` > `Close Project`.
2. Click **Open** and select the project directory.
3. Set the project SDK to **JDK 17** (`File` > `Project Structure` > `SDKs`).
4. Set **Project language level** to `SDK default`.
5. Run `./gradlew run` or locate `src/main/java/aria/Launcher.java` and run `Launcher.main()`.

---

## Building

```bash
# Run tests
./gradlew test

# Run the app
./gradlew run

# Build a standalone JAR (includes the ~512 MB LLM model)
./gradlew shadowJar
# Output: build/libs/aria.jar
```

---

## Documentation

- [User Guide](docs/README.md) — full command reference with examples
- [Developer Guide](docs/DeveloperGuide.md) — architecture, design decisions, and testing

---

## Data

Tasks are saved automatically to `./data/aria.txt` after every change. Archived tasks go to `./data/archive.txt`. Both files are plain text and human-readable.
