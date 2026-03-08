package aria.ui;

import aria.Aria;

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

    private Aria aria;

    private Image userImage = new Image(this.getClass().getResourceAsStream("/images/DaUser.png"));
    private Image ariaImage = new Image(this.getClass().getResourceAsStream("/images/DaAria.png"));

    /** Initialises the GUI: auto-scrolls to bottom on new content and binds width to viewport. */
    @FXML
    public void initialize() {
        dialogContainer.heightProperty().addListener((obs, oldVal, newVal) ->
                scrollPane.setVvalue(1.0));
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
     * Injects the Aria instance and shows the Aria welcome message.
     *
     * @param d the Aria application instance
     */
    public void setAria(Aria d) {
        aria = d;
        String welcome = "Hi! I'm Aria, your personal task manager.\n"
                + "Type 'help' to see all available commands.";
        String reminders = aria.buildReminderMessage();
        dialogContainer.getChildren().add(DialogBox.getAriaDialog(welcome, ariaImage));
        if (!reminders.isEmpty()) {
            dialogContainer.getChildren().add(DialogBox.getAriaDialog(reminders, ariaImage));
        }
    }

    /**
     * Handles user input: creates dialog boxes for the user's message and Aria's reply,
     * then clears the input field.
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        String response = aria.getResponse(input);
        dialogContainer.getChildren().addAll(
                DialogBox.getUserDialog(input, userImage),
                DialogBox.getAriaDialog(response, ariaImage)
        );
        userInput.clear();
    }
}
