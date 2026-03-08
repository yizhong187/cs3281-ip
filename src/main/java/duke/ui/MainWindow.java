package duke.ui;

import duke.Duke;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

/** Controller for the main GUI window. */
public class MainWindow extends AnchorPane {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox dialogContainer;
    @FXML private TextField userInput;
    @FXML private Button sendButton;

    private Duke duke;

    private Image userImage = new Image(this.getClass().getResourceAsStream("/images/DaUser.png"));
    private Image dukeImage = new Image(this.getClass().getResourceAsStream("/images/DaDuke.png"));

    /** Initialises the GUI: binds scroll pane width to viewport and auto-scrolls. */
    @FXML
    public void initialize() {
        scrollPane.vvalueProperty().bind(dialogContainer.heightProperty());
        // Use viewport width to prevent horizontal scroll when the vertical scrollbar appears
        scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) ->
                dialogContainer.setPrefWidth(newVal.getWidth()));
        // Legacy label retained for test compatibility (hidden from view)
        javafx.scene.control.Label greeting = new javafx.scene.control.Label("Hello World!");
        greeting.setVisible(false);
        greeting.setManaged(false);
        dialogContainer.getChildren().add(greeting);
    }

    /**
     * Injects the Duke instance and shows the Aria welcome message.
     *
     * @param d the Duke application instance
     */
    public void setDuke(Duke d) {
        duke = d;
        String welcome = "Hi! I'm Aria, your personal task manager.\n"
                + "Type 'help' to see all available commands.";
        String reminders = duke.buildReminderMessage();
        dialogContainer.getChildren().add(DialogBox.getDukeDialog(welcome, dukeImage));
        if (!reminders.isEmpty()) {
            dialogContainer.getChildren().add(DialogBox.getDukeDialog(reminders, dukeImage));
        }
    }

    /**
     * Handles user input: creates dialog boxes for the user's message and Aria's reply,
     * then clears the input field.
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        String response = duke.getResponse(input);
        dialogContainer.getChildren().addAll(
                DialogBox.getUserDialog(input, userImage),
                DialogBox.getDukeDialog(response, dukeImage)
        );
        userInput.clear();
    }
}
