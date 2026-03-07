package duke;

import duke.command.Command;
import duke.command.Parser;
import duke.storage.Storage;
import duke.task.TaskList;
import duke.ui.Ui;

/**
 * The main entry point for the Aria task manager application.
 * Coordinates the UI, storage, and task list components.
 */
public class Duke {
    private static final String FILE_PATH = "./data/duke.txt";

    private Storage storage;
    private TaskList taskList;
    private Ui ui;

    /**
     * Constructs a Duke instance, loading existing tasks from storage.
     * Falls back to an empty task list if loading fails.
     */
    public Duke() {
        ui = new Ui();
        storage = new Storage(FILE_PATH);
        try {
            taskList = new TaskList(storage.load());
        } catch (DukeException e) {
            ui.showLoadingError();
            taskList = new TaskList();
        }
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
                command.execute(taskList, ui, storage);
                if (command.isExit()) {
                    break;
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
        try {
            Command command = Parser.parse(input);
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
