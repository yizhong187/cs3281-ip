package duke;

import duke.command.Command;
import duke.command.CommandType;
import duke.command.Parser;
import duke.storage.Storage;
import duke.task.Task;
import duke.task.TaskList;
import duke.ui.Ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

/**
 * The main entry point for the Aria task manager application.
 * Coordinates the UI, storage, and task list components.
 */
public class Duke {
    private static final String DEFAULT_FILE_PATH = "./data/duke.txt";

    private Storage storage;
    private TaskList taskList;
    private Ui ui;
    private Deque<String> undoStack;

    /**
     * Constructs a Duke instance, loading existing tasks from storage.
     * Falls back to an empty task list if loading fails.
     */
    /** Constructs Duke using the default data file path. */
    public Duke() {
        this(DEFAULT_FILE_PATH);
    }

    /**
     * Constructs Duke using the given data file path.
     *
     * @param filePath path to the data file
     */
    public Duke(String filePath) {
        ui = new Ui();
        storage = new Storage(filePath);
        undoStack = new ArrayDeque<>();
        try {
            taskList = new TaskList(storage.load());
        } catch (DukeException e) {
            ui.showLoadingError();
            taskList = new TaskList();
        }
        assert ui != null : "Ui must be initialised";
        assert storage != null : "Storage must be initialised";
        assert taskList != null : "TaskList must be initialised";
    }

    /**
     * Builds a reminder message for tasks due within the next 3 days.
     * Returns an empty string if there are no upcoming tasks.
     *
     * @return the reminder string, or empty
     */
    public String buildReminderMessage() {
        ArrayList<Task> upcoming = taskList.getUpcomingReminders(3);
        if (upcoming.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Reminders — tasks due within 3 days:");
        for (int i = 0; i < upcoming.size(); i++) {
            sb.append("\n  ").append(i + 1).append(".").append(upcoming.get(i));
        }
        return sb.toString();
    }

    /**
     * Runs the CLI event loop, reading and executing commands until exit.
     */
    public void run() {
        ui.showWelcome();
        String reminders = buildReminderMessage();
        if (!reminders.isEmpty()) {
            ui.showMessage(reminders);
        }
        while (ui.hasNextLine()) {
            String input = ui.readCommand();
            try {
                Command command = Parser.parse(input);
                if (command.isMutating()) {
                    undoStack.push(storage.getSnapshot());
                }
                if (command.getType() == CommandType.UNDO) {
                    if (undoStack.isEmpty()) {
                        ui.showError("Nothing to undo.");
                    } else {
                        taskList = storage.restoreFromSnapshot(undoStack.pop());
                        ui.showMessage("Last action undone.");
                    }
                } else {
                    command.execute(taskList, ui, storage);
                    if (command.isExit()) {
                        break;
                    }
                }
            } catch (DukeException e) {
                ui.showError(e.getMessage());
            }
        }
    }

    /**
     * Generates a response string for the given user input (used by the GUI).
     *
     * @param input the user's input string
     * @return the response message
     */
    public String getResponse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Please enter a command. Type 'help' to see available commands.";
        }
        try {
            Command command = Parser.parse(input);
            if (command.isMutating()) {
                undoStack.push(storage.getSnapshot());
            }
            if (command.getType() == CommandType.UNDO) {
                if (undoStack.isEmpty()) {
                    return "Nothing to undo.";
                }
                taskList = storage.restoreFromSnapshot(undoStack.pop());
                return "Last action undone.";
            }
            return command.getOutput(taskList, storage);
        } catch (DukeException e) {
            return e.getMessage();
        }
    }

    /**
     * Application entry point for CLI mode.
     * Accepts an optional data file path as the first argument.
     *
     * @param args command-line arguments; args[0] overrides the default data file path
     */
    public static void main(String[] args) {
        String filePath = args.length > 0 ? args[0] : DEFAULT_FILE_PATH;
        new Duke(filePath).run();
    }
}
