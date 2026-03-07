package duke.task;

import java.util.ArrayList;

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
     * Finds tasks whose description contains the given keyword (case-insensitive).
     *
     * @param keyword the search keyword
     * @return list of matching tasks
     */
    public ArrayList<Task> find(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        ArrayList<Task> result = new ArrayList<>();
        tasks.stream()
                .filter(task -> task.getDescription().toLowerCase().contains(lowerKeyword))
                .forEach(result::add);
        return result;
    }
}
