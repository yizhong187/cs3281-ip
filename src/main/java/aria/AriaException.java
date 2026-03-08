package aria;

/**
 * Represents an exception specific to the Aria application.
 * Used for user input validation and error handling.
 */
public class AriaException extends Exception {
    /**
     * Constructs a AriaException with the given message.
     *
     * @param message the error message
     */
    public AriaException(String message) {
        super(message);
    }
}
