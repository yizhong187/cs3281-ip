package duke.command;

import duke.DukeException;

/**
 * Parses raw user input strings into {@link Command} objects.
 */
public class Parser {
    /**
     * Parses the given user input and returns the corresponding {@link Command}.
     *
     * @param input the raw user input string
     * @return the parsed Command
     * @throws DukeException if the input is invalid or unrecognised
     */
    public static Command parse(String input) throws DukeException {
        String trimmed = input.trim();
        String[] parts = trimmed.split(" ", 2);
        String commandWord = parts[0].toLowerCase();

        switch (commandWord) {
        case "bye":
            return new Command(CommandType.BYE, null, null, null, null);
        case "list":
            return new Command(CommandType.LIST, null, null, null, null);
        case "mark":
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! Please provide a task number to mark.");
            }
            return new Command(CommandType.MARK, parts[1].trim(), null, null, null);
        case "unmark":
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! Please provide a task number to unmark.");
            }
            return new Command(CommandType.UNMARK, parts[1].trim(), null, null, null);
        case "delete":
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! Please provide a task number to delete.");
            }
            return new Command(CommandType.DELETE, parts[1].trim(), null, null, null);
        case "todo":
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! The description of a todo cannot be empty.");
            }
            return new Command(CommandType.TODO, parts[1].trim(), null, null, null);
        case "deadline": {
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! The description of a deadline cannot be empty.");
            }
            String[] deadlineParts = parts[1].split(" /by ", 2);
            if (deadlineParts.length < 2) {
                throw new DukeException(
                        "OOPS!!! A deadline needs a /by date. Usage: deadline <desc> /by <date>");
            }
            return new Command(CommandType.DEADLINE, deadlineParts[0].trim(),
                    deadlineParts[1].trim(), null, null);
        }
        case "event": {
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! The description of an event cannot be empty.");
            }
            String[] eventParts = parts[1].split(" /from ", 2);
            if (eventParts.length < 2) {
                throw new DukeException("OOPS!!! An event needs /from and /to times. "
                        + "Usage: event <desc> /from <time> /to <time>");
            }
            String[] timeParts = eventParts[1].split(" /to ", 2);
            if (timeParts.length < 2) {
                throw new DukeException("OOPS!!! An event needs a /to time. "
                        + "Usage: event <desc> /from <time> /to <time>");
            }
            return new Command(CommandType.EVENT, eventParts[0].trim(),
                    null, timeParts[0].trim(), timeParts[1].trim());
        }
        case "find":
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! Please provide a keyword to search for.");
            }
            return new Command(CommandType.FIND, parts[1].trim(), null, null, null);
        case "help":
            return new Command(CommandType.HELP, null, null, null, null);
        default:
            throw new DukeException(
                    "OOPS!!! I'm sorry, but I don't know what that means :-( "
                    + "Type 'help' to see a list of available commands.");
        }
    }
}
