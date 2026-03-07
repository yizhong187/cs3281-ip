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
import java.util.List;
import java.util.TreeSet;
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
    private String recurrence;
    private int afterIndex;

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
        this.recurrence = null;
        this.afterIndex = -1;
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
     * Sets the recurrence interval for this command's task.
     *
     * @param recurrence the interval string ("daily", "weekly", "monthly"), or null
     */
    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }

    /**
     * Sets the 1-based index of the task that must be done before this task.
     *
     * @param afterIndex the 1-based index, or -1 if no dependency
     */
    public void setAfterIndex(int afterIndex) {
        this.afterIndex = afterIndex;
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
     * Returns true if this command mutates the task list (and should be undoable).
     *
     * @return true if mutating
     */
    public boolean isMutating() {
        switch (type) {
        case MARK: case UNMARK: case DELETE: case TODO: case DEADLINE: case EVENT:
        case ARCHIVE: case TAG: case UNTAG: case UPDATE: case SORT: case SNOOZE:
        case TEVENT: case CONFIRM: case WITHIN: case FIXED:
            return true;
        default:
            return false;
        }
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
            for (int index : parseIndices(description, taskList)) {
                taskList.get(index).markDone();
                ui.showTaskMarked(taskList.get(index));
            }
            storage.save(taskList);
            break;
        }
        case UNMARK: {
            for (int index : parseIndices(description, taskList)) {
                taskList.get(index).markUndone();
                ui.showTaskUnmarked(taskList.get(index));
            }
            storage.save(taskList);
            break;
        }
        case DELETE: {
            List<Integer> indices = parseIndices(description, taskList);
            indices.sort(java.util.Collections.reverseOrder());
            for (int index : indices) {
                Task removed = taskList.remove(index);
                ui.showTaskDeleted(removed, taskList.size());
            }
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
        case FREETIME: case FIXED: case WITHIN: case TEVENT: case CONFIRM:
            ui.showMessage(getOutput(taskList, storage));
            break;
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
        case ARCHIVE: {
            int index = parseIndex(description, taskList);
            Task removed = taskList.remove(index);
            storage.appendToArchive(removed);
            storage.save(taskList);
            ui.showTaskDeleted(removed, taskList.size());
            break;
        }
        case ARCHIVES: {
            ArrayList<Task> archived = storage.loadArchive();
            ui.showFoundTasks(archived);
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
        case STATS:
            ui.showMessage(getOutput(taskList, storage));
            break;
        case SNOOZE:
            ui.showMessage(getOutput(taskList, storage));
            break;
        case SCHEDULE:
            ui.showFoundTasks(taskList.getScheduleOn(
                    duke.util.DateParser.parse(description) != null
                    ? duke.util.DateParser.parse(description)
                    : java.time.LocalDate.now()));
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
            List<Integer> markIndices = parseIndices(description, taskList);
            StringBuilder markSb = new StringBuilder("Nice! I've marked these tasks as done:");
            for (int index : markIndices) {
                Task taskToMark = taskList.get(index);
                int dep = taskToMark.getAfterIndex();
                if (dep > 0 && dep <= taskList.size() && !taskList.get(dep - 1).isDone()) {
                    markSb.append("\n  Warning: Task ").append(dep)
                            .append(" (\"").append(taskList.get(dep - 1).getDescription())
                            .append("\") should be done first!");
                }
                taskToMark.markDone();
                markSb.append("\n  ").append(taskToMark);
                spawnNextRecurrence(taskList, index);
            }
            storage.save(taskList);
            return markSb.toString();
        }
        case UNMARK: {
            List<Integer> unmarkIndices = parseIndices(description, taskList);
            StringBuilder unmarkSb = new StringBuilder("OK, I've marked these tasks as not done:");
            for (int index : unmarkIndices) {
                taskList.get(index).markUndone();
                unmarkSb.append("\n  ").append(taskList.get(index));
            }
            storage.save(taskList);
            return unmarkSb.toString();
        }
        case DELETE: {
            List<Integer> delIndices = parseIndices(description, taskList);
            delIndices.sort(java.util.Collections.reverseOrder());
            StringBuilder delSb = new StringBuilder("Noted. I've removed these tasks:");
            for (int index : delIndices) {
                Task removed = taskList.remove(index);
                delSb.append("\n  ").append(removed);
            }
            storage.save(taskList);
            delSb.append("\nNow you have ").append(taskList.size()).append(" tasks in the list.");
            return delSb.toString();
        }
        case FREETIME: {
            String[] freeParts = description.split("\\s+", 2);
            java.time.LocalDate freeDate = duke.util.DateParser.parse(freeParts[0]);
            if (freeDate == null) {
                return "Could not parse date: " + freeParts[0];
            }
            double neededHours = 1.0;
            if (freeParts.length > 1) {
                try {
                    neededHours = Double.parseDouble(freeParts[1].trim());
                } catch (NumberFormatException e) {
                    return "Could not parse duration: " + freeParts[1];
                }
            }
            // Collect all events on this date and their hour ranges
            java.util.List<int[]> busySlots = new java.util.ArrayList<>();
            for (int i = 0; i < taskList.size(); i++) {
                Task t = taskList.get(i);
                if (t instanceof Event) {
                    Event ev = (Event) t;
                    java.time.LocalDate fd = ev.getFromDate();
                    java.time.LocalDate td = ev.getToDate();
                    if (fd != null && td != null
                            && !freeDate.isBefore(fd) && !freeDate.isAfter(td)) {
                        busySlots.add(new int[]{0, 24});
                    } else if (fd != null && fd.equals(freeDate)) {
                        busySlots.add(new int[]{0, 24});
                    }
                }
            }
            // Simple output: list available windows across the 8-22 window
            int neededBlocks = (int) Math.ceil(neededHours);
            StringBuilder freeSb = new StringBuilder(
                    "Free slots on " + freeDate + " (need " + neededHours + "h):");
            boolean foundAny = false;
            if (busySlots.isEmpty()) {
                for (int h = 8; h + neededBlocks <= 22; h++) {
                    freeSb.append("\n  ").append(String.format("%02d:00", h))
                            .append(" - ").append(String.format("%02d:00", h + neededBlocks));
                    foundAny = true;
                    if (foundAny && h >= 10) {
                        freeSb.append("\n  ... (more slots available)");
                        break;
                    }
                }
            } else {
                freeSb.append("\n  No free slots found (events cover the day).");
            }
            if (!foundAny && busySlots.isEmpty()) {
                freeSb.append("\n  No free slots found.");
            }
            return freeSb.toString();
        }
        case FIXED: {
            double hours;
            try {
                hours = Double.parseDouble(by);
            } catch (NumberFormatException e) {
                throw new DukeException("OOPS!!! Duration must be a number (e.g. 1.5 for 1.5 hours).");
            }
            boolean fixedDup = taskList.hasDuplicate(description);
            Task fixedTask = new duke.task.FixedDurationTask(description, hours);
            fixedTask.setPriority(priority);
            taskList.add(fixedTask);
            storage.save(taskList);
            String fixedMsg = "Got it. I've added this task:\n  " + fixedTask
                    + "\nNow you have " + taskList.size() + " tasks in the list.";
            if (fixedDup) {
                fixedMsg = "Warning: A task with this description already exists!\n" + fixedMsg;
            }
            return fixedMsg;
        }
        case WITHIN: {
            boolean dup = taskList.hasDuplicate(description);
            Task task = new duke.task.WithinPeriodTask(description, from, to);
            task.setPriority(priority);
            taskList.add(task);
            storage.save(taskList);
            String msg = "Got it. I've added this task:\n  " + task
                    + "\nNow you have " + taskList.size() + " tasks in the list.";
            if (dup) {
                msg = "Warning: A task with this description already exists!\n" + msg;
            }
            return msg;
        }
        case TEVENT: {
            java.util.List<String> slots = new java.util.ArrayList<>();
            java.util.regex.Matcher sm = java.util.regex.Pattern
                    .compile("/slot\\d+\\s+(.+?)(?=\\s+/slot|$)").matcher(description);
            String teDesc = description.replaceAll("/slot\\d+\\s+[^/]+", "").trim();
            while (sm.find()) {
                slots.add(sm.group(1).trim());
            }
            if (slots.isEmpty()) {
                throw new DukeException("OOPS!!! Provide at least one slot: /slot1 <time>");
            }
            duke.task.TentativeEvent te = new duke.task.TentativeEvent(teDesc, slots);
            te.setPriority(priority);
            taskList.add(te);
            storage.save(taskList);
            return "Got it. I've added a tentative event:\n  " + te
                    + "\nNow you have " + taskList.size() + " tasks in the list.";
        }
        case CONFIRM: {
            String[] cp = description.split("\\s+", 2);
            int idx = parseIndex(cp[0], taskList);
            Task task = taskList.get(idx);
            if (!(task instanceof duke.task.TentativeEvent)) {
                throw new DukeException("OOPS!!! Task " + (idx + 1) + " is not a tentative event.");
            }
            duke.task.TentativeEvent te = (duke.task.TentativeEvent) task;
            int slotNum;
            try {
                slotNum = Integer.parseInt(cp.length > 1 ? cp[1].trim() : "") - 1;
            } catch (NumberFormatException e) {
                throw new DukeException("OOPS!!! Please provide a valid slot number.");
            }
            if (slotNum < 0 || slotNum >= te.getSlots().size()) {
                throw new DukeException("OOPS!!! Slot " + (slotNum + 1) + " does not exist.");
            }
            String slot = te.getSlots().get(slotNum);
            Event confirmed = new Event(te.getDescription(), slot, slot);
            confirmed.setPriority(te.getPriority());
            taskList.set(idx, confirmed);
            storage.save(taskList);
            return "Confirmed slot " + (slotNum + 1) + ":\n  " + confirmed;
        }
        case TODO: {
            boolean dup = taskList.hasDuplicate(description);
            Task task = buildTask();
            taskList.add(task);
            storage.save(taskList);
            String msg = "Got it. I've added this task:\n  " + task
                    + "\nNow you have " + taskList.size() + " tasks in the list.";
            if (dup) {
                msg = "Warning: A task with this description already exists!\n" + msg;
            }
            return msg;
        }
        case DEADLINE: {
            boolean dup = taskList.hasDuplicate(description);
            Task task = buildTask();
            taskList.add(task);
            storage.save(taskList);
            String msg = "Got it. I've added this task:\n  " + task
                    + "\nNow you have " + taskList.size() + " tasks in the list.";
            if (dup) {
                msg = "Warning: A task with this description already exists!\n" + msg;
            }
            return msg;
        }
        case EVENT: {
            boolean dup = taskList.hasDuplicate(description);
            Task task = buildTask();
            ArrayList<Task> conflicts = (task instanceof Event)
                    ? taskList.findOverlappingEvents((Event) task, -1)
                    : new ArrayList<>();
            taskList.add(task);
            storage.save(taskList);
            String msg = "Got it. I've added this task:\n  " + task
                    + "\nNow you have " + taskList.size() + " tasks in the list.";
            if (!conflicts.isEmpty()) {
                String conflictStr = conflicts.stream().map(Task::toString)
                        .collect(Collectors.joining("\n  "));
                msg = "Warning: This event overlaps with:\n  " + conflictStr + "\n" + msg;
            }
            if (dup) {
                msg = "Warning: A task with this description already exists!\n" + msg;
            }
            return msg;
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
        case ARCHIVE: {
            int index = parseIndex(description, taskList);
            Task removed = taskList.remove(index);
            storage.appendToArchive(removed);
            storage.save(taskList);
            return "Archived task:\n  " + removed
                    + "\nNow you have " + taskList.size() + " tasks in the list.";
        }
        case ARCHIVES: {
            ArrayList<Task> archived = storage.loadArchive();
            if (archived.isEmpty()) {
                return "No archived tasks.";
            }
            String archiveLines = IntStream.range(0, archived.size())
                    .mapToObj(i -> (i + 1) + "." + archived.get(i))
                    .collect(Collectors.joining("\n"));
            return "Archived tasks:\n" + archiveLines;
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
                    + "  todo <desc> [/priority high|medium|low] [/after INDEX]\n"
                    + "  deadline <desc> /by <date> [/priority high|medium|low]\n"
                    + "  event <desc> /from <time> /to <time> [/priority high|medium|low]\n"
                    + "  mark <num>\n"
                    + "  unmark <num>\n"
                    + "  delete <num>\n"
                    + "  find <keyword>\n"
                    + "  sort name|priority|date\n"
                    + "  bye";
        case STATS: {
            long done = IntStream.range(0, taskList.size())
                    .filter(i -> taskList.get(i).isDone()).count();
            long total = taskList.size();
            long todos = IntStream.range(0, taskList.size())
                    .filter(i -> taskList.get(i) instanceof duke.task.Todo).count();
            long deadlines = IntStream.range(0, taskList.size())
                    .filter(i -> taskList.get(i) instanceof Deadline).count();
            long events = IntStream.range(0, taskList.size())
                    .filter(i -> taskList.get(i) instanceof Event).count();
            long pctDone = total == 0 ? 0 : done * 100 / total;
            return "Task Statistics:\n"
                    + "  Total:     " + total + "\n"
                    + "  Done:      " + done + " (" + pctDone + "%)\n"
                    + "  Undone:    " + (total - done) + "\n"
                    + "  Todos:     " + todos + "\n"
                    + "  Deadlines: " + deadlines + "\n"
                    + "  Events:    " + events;
        }
        case SNOOZE: {
            String[] snoozeParts = description.split("\\s+", 2);
            int index = parseIndex(snoozeParts[0], taskList);
            if (snoozeParts.length < 2) {
                throw new DukeException("OOPS!!! Usage: snooze <num> <newdate>");
            }
            String newDate = snoozeParts[1];
            Task task = taskList.get(index);
            if (task instanceof Deadline) {
                Deadline old = (Deadline) task;
                Deadline updated = new Deadline(old.getDescription(), newDate);
                updated.setPriority(old.getPriority());
                if (old.isDone()) {
                    updated.markDone();
                }
                for (String tag : old.getTags()) {
                    updated.addTag(tag);
                }
                taskList.set(index, updated);
            } else if (task instanceof Event) {
                ((Event) task).setFrom(newDate);
            } else {
                throw new DukeException("OOPS!!! Only deadlines and events can be snoozed.");
            }
            storage.save(taskList);
            return "Snoozed task:\n  " + taskList.get(index);
        }
        case SCHEDULE: {
            java.time.LocalDate date = duke.util.DateParser.parse(description);
            if (date == null) {
                return "Could not parse date: " + description;
            }
            ArrayList<Task> scheduled = taskList.getScheduleOn(date);
            if (scheduled.isEmpty()) {
                return "No tasks scheduled on " + date + ".";
            }
            String lines = IntStream.range(0, scheduled.size())
                    .mapToObj(i -> (i + 1) + "." + scheduled.get(i))
                    .collect(Collectors.joining("\n"));
            return "Tasks on " + date + ":\n" + lines;
        }
        case BYE:
            return "Bye. Hope to see you again soon!";
        default:
            return "";
        }
    }

    /**
     * Parses a space-separated list of 1-based indices and ranges (e.g. "1 3-5 7")
     * and returns a list of unique 0-based indices in ascending order.
     *
     * @param indexStr the string to parse
     * @param taskList the task list to validate against
     * @return sorted list of 0-based indices
     * @throws DukeException if any token is invalid or out of range
     */
    private List<Integer> parseIndices(String indexStr, TaskList taskList) throws DukeException {
        TreeSet<Integer> indices = new TreeSet<>();
        for (String token : indexStr.trim().split("\\s+")) {
            if (token.contains("-")) {
                String[] bounds = token.split("-", 2);
                try {
                    int lo = Integer.parseInt(bounds[0]) - 1;
                    int hi = Integer.parseInt(bounds[1]) - 1;
                    for (int i = lo; i <= hi; i++) {
                        if (i < 0 || i >= taskList.size()) {
                            throw new DukeException("OOPS!!! Task " + (i + 1) + " does not exist.");
                        }
                        indices.add(i);
                    }
                } catch (NumberFormatException e) {
                    throw new DukeException("OOPS!!! Invalid range: " + token);
                }
            } else {
                indices.add(parseIndex(token, taskList));
            }
        }
        return new ArrayList<>(indices);
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
        if (recurrence != null) {
            task.setRecurrence(recurrence);
        }
        if (afterIndex >= 0) {
            task.setAfterIndex(afterIndex);
        }
        return task;
    }

    /**
     * If the task at the given index is recurring, adds a new undone copy to the list.
     *
     * @param taskList the task list
     * @param index    the index of the just-marked task
     */
    private void spawnNextRecurrence(TaskList taskList, int index) {
        Task task = taskList.get(index);
        if (task.getRecurrence() == null) {
            return;
        }
        Task next;
        if (task instanceof Deadline) {
            java.time.LocalDate nextDate = advanceDate(
                    ((Deadline) task).getByDate(), task.getRecurrence());
            String byStr = nextDate != null
                    ? nextDate.toString() : ((Deadline) task).getBy();
            next = new Deadline(task.getDescription(), byStr);
        } else if (task instanceof Event) {
            next = new Event(task.getDescription(),
                    ((Event) task).getFrom(), ((Event) task).getTo());
        } else {
            next = new Todo(task.getDescription());
        }
        next.setPriority(task.getPriority());
        next.setRecurrence(task.getRecurrence());
        for (String tag : task.getTags()) {
            next.addTag(tag);
        }
        taskList.add(next);
    }

    private java.time.LocalDate advanceDate(java.time.LocalDate base, String recurrence) {
        if (base == null) {
            return null;
        }
        switch (recurrence) {
        case "daily":
            return base.plusDays(1);
        case "weekly":
            return base.plusWeeks(1);
        case "monthly":
            return base.plusMonths(1);
        default:
            return base.plusWeeks(1);
        }
    }
}
