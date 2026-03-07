public class Duke {
    private static final String FILE_PATH = "./data/duke.txt";

    private Storage storage;
    private TaskList taskList;
    private Ui ui;

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

    /** Generates a response for the user's chat message. */
    public String getResponse(String input) {
        try {
            Command command = Parser.parse(input);
            return command.getOutput(taskList, storage);
        } catch (DukeException e) {
            return e.getMessage();
        }
    }

    public static void main(String[] args) {
        new Duke().run();
    }
}
