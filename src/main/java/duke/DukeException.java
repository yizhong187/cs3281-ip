package duke;

/**
 * Represents an exception specific to the Duke application.
 * Used for user input validation and error handling.
 */
public class DukeException extends Exception {
    /**
     * Constructs a DukeException with the given message.
     *
     * @param message the error message
     */
    public DukeException(String message) {
        super(message);
    }
}
