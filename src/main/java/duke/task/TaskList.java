package duke.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Manages a list of {@link Task} objects.
 */
public class TaskList {
    private ArrayList<Task> tasks;

    /** Constructs an empty TaskList. */
    public TaskList() {
        this.tasks = new ArrayList<>();
    }

    /**
     * Constructs a TaskList from an existing list of tasks.
     *
     * @param tasks the initial task list
     */
    public TaskList(ArrayList<Task> tasks) {
        this.tasks = tasks;
    }

    /**
     * Returns true if any existing task has the same description (case-insensitive).
     *
     * @param description the description to check
     * @return true if a duplicate exists
     */
    public boolean hasDuplicate(String description) {
        String lower = description.toLowerCase();
        return tasks.stream().anyMatch(t -> t.getDescription().toLowerCase().equals(lower));
    }

    /**
     * Adds a task to the list.
     *
     * @param task the task to add
     */
    public void add(Task task) {
        assert task != null : "Cannot add a null task";
        tasks.add(task);
    }

    /**
     * Removes and returns the task at the given index.
     *
     * @param index the zero-based index
     * @return the removed task
     */
    public Task remove(int index) {
        assert index >= 0 && index < tasks.size() : "Remove index out of range: " + index;
        return tasks.remove(index);
    }

    /**
     * Returns the task at the given index.
     *
     * @param index the zero-based index
     * @return the task
     */
    public Task get(int index) {
        assert index >= 0 && index < tasks.size() : "Get index out of range: " + index;
        return tasks.get(index);
    }

    /**
     * Replaces the task at the given index.
     *
     * @param index the zero-based index
     * @param task  the replacement task
     */
    public void set(int index, Task task) {
        assert index >= 0 && index < tasks.size() : "Set index out of range: " + index;
        assert task != null : "Cannot set a null task";
        tasks.set(index, task);
    }

    /**
     * Returns the number of tasks.
     *
     * @return the list size
     */
    public int size() {
        return tasks.size();
    }

    /**
     * Returns all tasks.
     *
     * @return the underlying list
     */
    public ArrayList<Task> getAll() {
        return tasks;
    }

    /**
     * Sorts this list in-place by the given criterion.
     * Supported criteria: "name" (alphabetical), "priority" (HIGH first), "date" (earliest first).
     *
     * @param criterion the sort criterion
     */
    public void sortBy(String criterion) {
        Comparator<Task> comparator;
        switch (criterion.toLowerCase()) {
        case "priority":
            comparator = Comparator.comparingInt(t -> t.getPriority().ordinal());
            break;
        case "date":
            comparator = (a, b) -> {
                LocalDate da = (a instanceof Deadline) ? ((Deadline) a).getByDate() : null;
                LocalDate db = (b instanceof Deadline) ? ((Deadline) b).getByDate() : null;
                if (da == null && db == null) {
                    return 0;
                }
                if (da == null) {
                    return 1;
                }
                if (db == null) {
                    return -1;
                }
                return da.compareTo(db);
            };
            break;
        default:
            comparator = Comparator.comparing(Task::getDescription, String.CASE_INSENSITIVE_ORDER);
        }
        tasks.sort(comparator);
    }

    /**
     * Finds tasks whose description contains the given keyword (case-insensitive).
     *
     * @param keyword the search keyword
     * @return list of matching tasks
     */
    /**
     * Finds tasks matching the given keyword or filter.
     * Supports:
     * - {@code #tagname} – filter by tag
     * - {@code /type todo|deadline|event} – filter by task type
     * - {@code /priority high|medium|low} – filter by priority
     * - plain text – substring match on description (case-insensitive)
     *
     * @param keyword the search keyword or filter expression
     * @return list of matching tasks
     */
    public ArrayList<Task> find(String keyword) {
        String lower = keyword.toLowerCase().trim();
        ArrayList<Task> result = new ArrayList<>();
        if (lower.startsWith("#")) {
            String tag = lower.substring(1);
            tasks.stream().filter(t -> t.hasTag(tag)).forEach(result::add);
        } else if (lower.startsWith("/type ")) {
            String type = lower.substring(6).trim();
            tasks.stream().filter(t -> matchesType(t, type)).forEach(result::add);
        } else if (lower.startsWith("/priority ")) {
            String prio = lower.substring(10).trim();
            duke.task.Priority target = duke.task.Priority.fromString(prio);
            tasks.stream().filter(t -> t.getPriority() == target).forEach(result::add);
        } else {
            tasks.stream()
                    .filter(t -> t.getDescription().toLowerCase().contains(lower))
                    .forEach(result::add);
        }
        return result;
    }

    private boolean matchesType(Task task, String type) {
        switch (type) {
        case "todo":
            return task instanceof Todo;
        case "deadline":
            return task instanceof Deadline;
        case "event":
            return task instanceof Event;
        default:
            return false;
        }
    }
}
