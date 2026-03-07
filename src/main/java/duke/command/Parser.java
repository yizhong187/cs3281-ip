package duke.command;

import duke.DukeException;
import duke.task.Priority;

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
        case "todo": {
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! The description of a todo cannot be empty.");
            }
            Priority todoPriority = extractPriority(parts[1]);
            String todoDesc = stripPriority(parts[1]).trim();
            if (todoDesc.isEmpty()) {
                throw new DukeException("OOPS!!! The description of a todo cannot be empty.");
            }
            Command todoCmd = new Command(CommandType.TODO, todoDesc, null, null, null);
            todoCmd.setPriority(todoPriority);
            return todoCmd;
        }
        case "deadline": {
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! The description of a deadline cannot be empty.");
            }
            Priority deadlinePriority = extractPriority(parts[1]);
            String deadlineArgs = stripPriority(parts[1]);
            String[] deadlineParts = deadlineArgs.split(" /by ", 2);
            if (deadlineParts.length < 2) {
                throw new DukeException(
                        "OOPS!!! A deadline needs a /by date. Usage: deadline <desc> /by <date>");
            }
            Command deadlineCmd = new Command(CommandType.DEADLINE, deadlineParts[0].trim(),
                    deadlineParts[1].trim(), null, null);
            deadlineCmd.setPriority(deadlinePriority);
            return deadlineCmd;
        }
        case "event": {
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! The description of an event cannot be empty.");
            }
            Priority eventPriority = extractPriority(parts[1]);
            String eventArgs = stripPriority(parts[1]);
            String[] eventParts = eventArgs.split(" /from ", 2);
            if (eventParts.length < 2) {
                throw new DukeException("OOPS!!! An event needs /from and /to times. "
                        + "Usage: event <desc> /from <time> /to <time>");
            }
            String[] timeParts = eventParts[1].split(" /to ", 2);
            if (timeParts.length < 2) {
                throw new DukeException("OOPS!!! An event needs a /to time. "
                        + "Usage: event <desc> /from <time> /to <time>");
            }
            Command eventCmd = new Command(CommandType.EVENT, eventParts[0].trim(),
                    null, timeParts[0].trim(), timeParts[1].trim());
            eventCmd.setPriority(eventPriority);
            return eventCmd;
        }
        case "find":
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! Please provide a keyword to search for.");
            }
            return new Command(CommandType.FIND, parts[1].trim(), null, null, null);
        case "tag":
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! Usage: tag <num> <tagname>");
            }
            return new Command(CommandType.TAG, parts[1].trim(), null, null, null);
        case "untag":
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! Usage: untag <num> <tagname>");
            }
            return new Command(CommandType.UNTAG, parts[1].trim(), null, null, null);
        case "update":
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException("OOPS!!! Usage: update <num> [/desc NEW] [/by NEW] "
                        + "[/from NEW] [/to NEW] [/priority high|medium|low]");
            }
            return new Command(CommandType.UPDATE, parts[1].trim(), null, null, null);
        case "sort":
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                throw new DukeException(
                        "OOPS!!! Please provide a sort criterion: name, priority, or date.");
            }
            return new Command(CommandType.SORT, parts[1].trim(), null, null, null);
        case "help":
            return new Command(CommandType.HELP, null, null, null, null);
        default:
            throw new DukeException(
                    "OOPS!!! I'm sorry, but I don't know what that means :-( "
                    + "Type 'help' to see a list of available commands.");
        }
    }

    /**
     * Extracts the priority level from the args string by looking for {@code /priority VALUE}.
     * Defaults to {@link Priority#MEDIUM} if not present.
     *
     * @param args the argument string, possibly containing {@code /priority high|medium|low}
     * @return the extracted Priority
     */
    private static Priority extractPriority(String args) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("/priority\\s+(\\S+)").matcher(args);
        return m.find() ? Priority.fromString(m.group(1)) : Priority.MEDIUM;
    }

    /**
     * Strips the {@code /priority VALUE} token from the args string.
     *
     * @param args the argument string
     * @return the args string with the priority token removed
     */
    private static String stripPriority(String args) {
        return args.replaceAll("/priority\\s+\\S+", "").trim();
    }
}
