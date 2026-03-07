package duke.command;

import duke.DukeException;
import duke.storage.Storage;
import duke.task.Deadline;
import duke.task.Event;
import duke.task.Priority;
import duke.task.Task;
import duke.task.TaskList;
import duke.task.Todo;
import duke.ui.Ui;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Represents a parsed user command with its associated parameters.
 * Supports both CLI execution (via {@link #execute}) and GUI output (via {@link #getOutput}).
 */
public class Command {
    private CommandType type;
    private String description;
    private String by;
    private String from;
    private String to;
    private Priority priority;

    /**
     * Constructs a Command with the given type and parameters.
     *
     * @param type        the command type
     * @param description the main argument (task description or index)
     * @param by          the deadline date (for DEADLINE commands)
     * @param from        the start time (for EVENT commands)
     * @param to          the end time (for EVENT commands)
     */
    public Command(CommandType type, String description, String by, String from, String to) {
        this.type = type;
        this.description = description;
        this.by = by;
        this.from = from;
        this.to = to;
        this.priority = Priority.MEDIUM;
    }

    /**
     * Sets the priority for this command's task.
     *
     * @param priority the priority level
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    /**
     * Returns the type of this command.
     *
     * @return the CommandType
     */
    public CommandType getType() {
        return type;
    }

    /**
     * Returns true if this is an exit (BYE) command.
     *
     * @return true if BYE, false otherwise
     */
    public boolean isExit() {
        return type == CommandType.BYE;
    }

    /**
     * Executes this command in CLI mode, producing output via {@link Ui}
     * and persisting changes via {@link Storage}.
     *
     * @param taskList the current task list
     * @param ui       the CLI user interface
     * @param storage  the storage handler
     * @throws DukeException if the command cannot be executed
     */
    public void execute(TaskList taskList, Ui ui, Storage storage) throws DukeException {
        switch (type) {
        case LIST:
            ui.showTaskList(taskList);
            break;
        case MARK: {
            int index = parseIndex(description, taskList);
            taskList.get(index).markDone();
            ui.showTaskMarked(taskList.get(index));
            storage.save(taskList);
            break;
        }
        case UNMARK: {
            int index = parseIndex(description, taskList);
            taskList.get(index).markUndone();
            ui.showTaskUnmarked(taskList.get(index));
            storage.save(taskList);
            break;
        }
        case DELETE: {
            int index = parseIndex(description, taskList);
            Task removed = taskList.remove(index);
            ui.showTaskDeleted(removed, taskList.size());
            storage.save(taskList);
            break;
        }
        case TODO: {
            Task task = buildTask();
            taskList.add(task);
            ui.showTaskAdded(task, taskList.size());
            storage.save(taskList);
            break;
        }
        case DEADLINE: {
            Task task = buildTask();
            taskList.add(task);
            ui.showTaskAdded(task, taskList.size());
            storage.save(taskList);
            break;
        }
        case EVENT: {
            Task task = buildTask();
            taskList.add(task);
            ui.showTaskAdded(task, taskList.size());
            storage.save(taskList);
            break;
        }
        case FIND:
            ui.showFoundTasks(taskList.find(description));
            break;
        case TAG: {
            String[] tagParts = description.split("\\s+", 2);
            int index = parseIndex(tagParts[0], taskList);
            if (tagParts.length < 2) {
                throw new DukeException("OOPS!!! Please provide a tag name.");
            }
            taskList.get(index).addTag(tagParts[1]);
            storage.save(taskList);
            ui.showTaskMarked(taskList.get(index));
            break;
        }
        case UNTAG: {
            String[] untagParts = description.split("\\s+", 2);
            int index = parseIndex(untagParts[0], taskList);
            if (untagParts.length < 2) {
                throw new DukeException("OOPS!!! Please provide a tag name to remove.");
            }
            taskList.get(index).removeTag(untagParts[1]);
            storage.save(taskList);
            ui.showTaskMarked(taskList.get(index));
            break;
        }
        case UPDATE: {
            String[] updateParts = description.split("\\s+", 2);
            int index = parseIndex(updateParts[0], taskList);
            String args = updateParts.length > 1 ? updateParts[1] : "";
            applyUpdates(taskList, index, args);
            storage.save(taskList);
            ui.showTaskMarked(taskList.get(index));
            break;
        }
        case SORT:
            taskList.sortBy(description);
            storage.save(taskList);
            ui.showTaskList(taskList);
            break;
        case HELP:
            ui.showHelp();
            break;
        case BYE:
            ui.showGoodbye();
            break;
        default:
            break;
        }
    }

    /**
     * Executes this command and returns the response as a String for GUI display.
     *
     * @param taskList the current task list
     * @param storage  the storage handler
     * @return the response string
     * @throws DukeException if the command cannot be executed
     */
    public String getOutput(TaskList taskList, Storage storage) throws DukeException {
        switch (type) {
        case LIST: {
            if (taskList.size() == 0) {
                return "No tasks in your list yet.";
            }
            String taskLines = IntStream.range(0, taskList.size())
                    .mapToObj(i -> (i + 1) + "." + taskList.get(i))
                    .collect(Collectors.joining("\n"));
            return "Here are the tasks in your list:\n" + taskLines;
        }
        case MARK: {
            int index = parseIndex(description, taskList);
            taskList.get(index).markDone();
            storage.save(taskList);
            return "Nice! I've marked this task as done:\n  " + taskList.get(index);
        }
        case UNMARK: {
            int index = parseIndex(description, taskList);
            taskList.get(index).markUndone();
            storage.save(taskList);
            return "OK, I've marked this task as not done yet:\n  " + taskList.get(index);
        }
        case DELETE: {
            int index = parseIndex(description, taskList);
            Task removed = taskList.remove(index);
            storage.save(taskList);
            return "Noted. I've removed this task:\n  " + removed
                    + "\nNow you have " + taskList.size() + " tasks in the list.";
        }
        case TODO: {
            Task task = buildTask();
            taskList.add(task);
            storage.save(taskList);
            return "Got it. I've added this task:\n  " + task
                    + "\nNow you have " + taskList.size() + " tasks in the list.";
        }
        case DEADLINE: {
            Task task = buildTask();
            taskList.add(task);
            storage.save(taskList);
            return "Got it. I've added this task:\n  " + task
                    + "\nNow you have " + taskList.size() + " tasks in the list.";
        }
        case EVENT: {
            Task task = buildTask();
            taskList.add(task);
            storage.save(taskList);
            return "Got it. I've added this task:\n  " + task
                    + "\nNow you have " + taskList.size() + " tasks in the list.";
        }
        case FIND: {
            ArrayList<Task> results = taskList.find(description);
            if (results.isEmpty()) {
                return "No matching tasks found.";
            }
            String matchLines = IntStream.range(0, results.size())
                    .mapToObj(i -> (i + 1) + "." + results.get(i))
                    .collect(Collectors.joining("\n"));
            return "Here are the matching tasks in your list:\n" + matchLines;
        }
        case TAG: {
            String[] tagParts = description.split("\\s+", 2);
            int index = parseIndex(tagParts[0], taskList);
            if (tagParts.length < 2) {
                throw new DukeException("OOPS!!! Please provide a tag name.");
            }
            taskList.get(index).addTag(tagParts[1]);
            storage.save(taskList);
            return "Tagged task:\n  " + taskList.get(index);
        }
        case UNTAG: {
            String[] untagParts = description.split("\\s+", 2);
            int index = parseIndex(untagParts[0], taskList);
            if (untagParts.length < 2) {
                throw new DukeException("OOPS!!! Please provide a tag name to remove.");
            }
            taskList.get(index).removeTag(untagParts[1]);
            storage.save(taskList);
            return "Removed tag from task:\n  " + taskList.get(index);
        }
        case UPDATE: {
            String[] updateParts = description.split("\\s+", 2);
            int index = parseIndex(updateParts[0], taskList);
            String args = updateParts.length > 1 ? updateParts[1] : "";
            applyUpdates(taskList, index, args);
            storage.save(taskList);
            return "Got it. I've updated this task:\n  " + taskList.get(index);
        }
        case SORT: {
            taskList.sortBy(description);
            storage.save(taskList);
            if (taskList.size() == 0) {
                return "No tasks to sort.";
            }
            String sortedLines = IntStream.range(0, taskList.size())
                    .mapToObj(i -> (i + 1) + "." + taskList.get(i))
                    .collect(Collectors.joining("\n"));
            return "Tasks sorted by " + description + ":\n" + sortedLines;
        }
        case HELP:
            return "Available commands:\n"
                    + "  list\n"
                    + "  todo <desc> [/priority high|medium|low]\n"
                    + "  deadline <desc> /by <date> [/priority high|medium|low]\n"
                    + "  event <desc> /from <time> /to <time> [/priority high|medium|low]\n"
                    + "  mark <num>\n"
                    + "  unmark <num>\n"
                    + "  delete <num>\n"
                    + "  find <keyword>\n"
                    + "  sort name|priority|date\n"
                    + "  bye";
        case BYE:
            return "Bye. Hope to see you again soon!";
        default:
            return "";
        }
    }

    /**
     * Parses a 1-based index string and returns the 0-based index.
     *
     * @param indexStr the string to parse
     * @param taskList the task list to validate against
     * @return the 0-based index
     * @throws DukeException if the index is not a valid number or out of range
     */
    private int parseIndex(String indexStr, TaskList taskList) throws DukeException {
        try {
            int index = Integer.parseInt(indexStr) - 1;
            if (index < 0 || index >= taskList.size()) {
                throw new DukeException("OOPS!!! Task number " + (index + 1) + " does not exist.");
            }
            return index;
        } catch (NumberFormatException e) {
            throw new DukeException("OOPS!!! Please provide a valid task number.");
        }
    }

    /**
     * Applies field updates parsed from the args string to the task at the given index.
     * Supported flags: /desc, /by (Deadline only), /from (Event only), /to (Event only), /priority.
     * For /by updates on a Deadline, replaces the task with a new Deadline instance.
     *
     * @param taskList the current task list
     * @param index    the zero-based index of the task to update
     * @param args     the update arguments string (e.g. "/desc New name /priority high")
     */
    private void applyUpdates(TaskList taskList, int index, String args) {
        Task task = taskList.get(index);
        java.util.regex.Matcher descMatcher = java.util.regex.Pattern
                .compile("/desc\\s+(.+?)(?=\\s+/|$)").matcher(args);
        if (descMatcher.find()) {
            task.setDescription(descMatcher.group(1).trim());
        }
        java.util.regex.Matcher priorityMatcher = java.util.regex.Pattern
                .compile("/priority\\s+(\\S+)").matcher(args);
        if (priorityMatcher.find()) {
            task.setPriority(duke.task.Priority.fromString(priorityMatcher.group(1)));
        }
        if (task instanceof Deadline) {
            java.util.regex.Matcher byMatcher = java.util.regex.Pattern
                    .compile("/by\\s+(.+?)(?=\\s+/|$)").matcher(args);
            if (byMatcher.find()) {
                Deadline old = (Deadline) task;
                Deadline updated = new Deadline(task.getDescription(), byMatcher.group(1).trim());
                updated.setPriority(old.getPriority());
                if (old.isDone()) {
                    updated.markDone();
                }
                taskList.set(index, updated);
            }
        } else if (task instanceof Event) {
            Event event = (Event) task;
            java.util.regex.Matcher fromMatcher = java.util.regex.Pattern
                    .compile("/from\\s+(.+?)(?=\\s+/|$)").matcher(args);
            if (fromMatcher.find()) {
                event.setFrom(fromMatcher.group(1).trim());
            }
            java.util.regex.Matcher toMatcher = java.util.regex.Pattern
                    .compile("/to\\s+(.+?)(?=\\s+/|$)").matcher(args);
            if (toMatcher.find()) {
                event.setTo(toMatcher.group(1).trim());
            }
        }
    }

    /**
     * Builds the appropriate Task subclass from this command's fields,
     * applying the stored priority level.
     *
     * @return the constructed Task with priority set
     */
    private Task buildTask() {
        Task task;
        switch (type) {
        case DEADLINE:
            task = new Deadline(description, by);
            break;
        case EVENT:
            task = new Event(description, from, to);
            break;
        default:
            task = new Todo(description);
            break;
        }
        task.setPriority(priority);
        return task;
    }
}
