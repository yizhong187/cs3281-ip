package aria.ui;

import aria.Aria;
import aria.nlp.LlmInterpreter;

import javafx.concurrent.Task;
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

    /** Pending destructive command awaiting user confirmation (or {@code null} if none). */
    private volatile String pendingLlmCommand = null;

    /** Thinking indicator bubble, removed once LLM responds. */
    private DialogBox thinkingBubble = null;

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
     * Injects the Aria instance, shows the welcome message, and warms up the LLM in the background.
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

        // Warm up the LLM on a daemon thread so it's ready when the user first needs it.
        DialogBox loadingBubble = DialogBox.getAriaDialog(
                "(Loading natural language model... this may take a moment)", ariaImage);
        dialogContainer.getChildren().add(loadingBubble);
        setInputEnabled(false);

        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() {
                LlmInterpreter.getInstance().init();
                return null;
            }
        };
        initTask.setOnSucceeded(e -> {
            dialogContainer.getChildren().remove(loadingBubble);
            String status = LlmInterpreter.getInstance().isReady()
                    ? "(Natural language mode ready. You can now type in plain English!)"
                    : "(Natural language model not available — exact commands still work.)";
            dialogContainer.getChildren().add(DialogBox.getAriaDialog(status, ariaImage));
            setInputEnabled(true);
        });
        initTask.setOnFailed(e -> {
            dialogContainer.getChildren().remove(loadingBubble);
            setInputEnabled(true);
        });

        Thread llmInit = new Thread(initTask, "llm-init");
        llmInit.setDaemon(true);
        llmInit.start();
    }

    /**
     * Handles user input. Routes to confirmation handler if a destructive LLM command is pending,
     * otherwise attempts direct parsing and falls back to the LLM asynchronously.
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText().trim();
        if (input.isEmpty()) {
            return;
        }

        // If awaiting confirmation for a destructive LLM command, route there instead.
        if (pendingLlmCommand != null) {
            dialogContainer.getChildren().add(DialogBox.getUserDialog(input, userImage));
            userInput.clear();
            handleConfirmation(input);
            return;
        }

        dialogContainer.getChildren().add(DialogBox.getUserDialog(input, userImage));
        userInput.clear();

        // Try direct (exact) parsing first — no LLM involvement.
        try {
            String response = aria.tryGetResponseDirect(input);
            dialogContainer.getChildren().add(DialogBox.getAriaDialog(response, ariaImage));
            return;
        } catch (aria.AriaException ignored) {
            // Unknown command — fall through to LLM.
        }

        // LLM fallback: show thinking indicator and disable input while waiting.
        thinkingBubble = DialogBox.getAriaDialog("(thinking...)", ariaImage);
        dialogContainer.getChildren().add(thinkingBubble);
        setInputEnabled(false);

        Task<String> llmTask = new Task<>() {
            @Override
            protected String call() {
                return LlmInterpreter.getInstance().interpret(input);
            }
        };

        llmTask.setOnSucceeded(e -> {
            removeThinkingBubble();
            setInputEnabled(true);
            handleLlmResult(llmTask.getValue());
        });

        llmTask.setOnFailed(e -> {
            removeThinkingBubble();
            setInputEnabled(true);
            dialogContainer.getChildren().add(DialogBox.getAriaDialog(
                    "Something went wrong. Please try again.", ariaImage));
        });

        Thread t = new Thread(llmTask, "llm-interpret");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Processes the LLM's interpretation result and takes the appropriate action.
     *
     * @param candidate the validated command string from the LLM, or {@code null}
     */
    private void handleLlmResult(String candidate) {
        LlmInterpreter llm = LlmInterpreter.getInstance();

        if (candidate == null) {
            dialogContainer.getChildren().add(DialogBox.getAriaDialog(
                    "I'm not sure how to handle that. Type 'help' to see available commands.",
                    ariaImage));
            return;
        }

        if (llm.isChatResponse(candidate)) {
            dialogContainer.getChildren().add(
                    DialogBox.getAriaDialog(llm.extractChatMessage(candidate), ariaImage));
        } else if (llm.isDestructive(candidate)) {
            pendingLlmCommand = candidate;
            dialogContainer.getChildren().add(DialogBox.getAriaDialog(
                    "I interpreted that as: \"" + candidate + "\"\n"
                    + "This will permanently modify your task list.\n"
                    + "Type yes to confirm, or anything else to cancel.",
                    ariaImage));
        } else {
            String result = aria.executeInterpreted(candidate);
            dialogContainer.getChildren().add(DialogBox.getAriaDialog(
                    "(I interpreted: \"" + candidate + "\")\n" + result, ariaImage));
        }
    }

    /**
     * Handles the user's yes/no reply to a pending destructive-command confirmation.
     *
     * @param reply the user's reply
     */
    private void handleConfirmation(String reply) {
        String cmd = pendingLlmCommand;
        pendingLlmCommand = null;

        if (reply.equalsIgnoreCase("yes") || reply.equalsIgnoreCase("y")) {
            String result = aria.executeInterpreted(cmd);
            dialogContainer.getChildren().add(DialogBox.getAriaDialog(result, ariaImage));
        } else {
            dialogContainer.getChildren().add(DialogBox.getAriaDialog(
                    "Cancelled. No changes made.", ariaImage));
        }
    }

    /** Removes the thinking indicator bubble from the dialog container. */
    private void removeThinkingBubble() {
        if (thinkingBubble != null) {
            dialogContainer.getChildren().remove(thinkingBubble);
            thinkingBubble = null;
        }
    }

    /** Enables or disables the text field and send button. */
    private void setInputEnabled(boolean enabled) {
        userInput.setDisable(!enabled);
        sendButton.setDisable(!enabled);
    }
}
