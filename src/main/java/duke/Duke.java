package duke;

import duke.command.Command;
import duke.command.CommandType;
import duke.command.Parser;
import duke.storage.Storage;
import duke.task.TaskList;
import duke.ui.Ui;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The main entry point for the Aria task manager application.
 * Coordinates the UI, storage, and task list components.
 */
public class Duke {
    private static final String FILE_PATH = "./data/duke.txt";

    private Storage storage;
    private TaskList taskList;
    private Ui ui;
    private Deque<String> undoStack;

    /**
     * Constructs a Duke instance, loading existing tasks from storage.
     * Falls back to an empty task list if loading fails.
     */
    public Duke() {
        ui = new Ui();
        storage = new Storage(FILE_PATH);
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
     * Runs the CLI event loop, reading and executing commands until exit.
     */
    public void run() {
        ui.showWelcome();
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
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        new Duke().run();
    }
}
