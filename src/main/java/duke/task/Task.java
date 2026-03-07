package duke.task;

/**
 * Represents an abstract task with a description, completion status, and priority level.
 * Subclasses must implement {@code toFileString()} for file persistence.
 */
public abstract class Task {
    protected String description;
    protected boolean isDone;
    protected Priority priority;

    /**
     * Constructs a Task with the given description, initially not done and MEDIUM priority.
     *
     * @param description the task description
     */
    public Task(String description) {
        this.description = description;
        this.isDone = false;
        this.priority = Priority.MEDIUM;
    }

    /** Marks this task as done. */
    public void markDone() {
        isDone = true;
    }

    /** Marks this task as not done. */
    public void markUndone() {
        isDone = false;
    }

    /**
     * Returns whether this task is done.
     *
     * @return true if done, false otherwise
     */
    public boolean isDone() {
        return isDone;
    }

    /**
     * Returns the task description.
     *
     * @return the description string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the priority of this task.
     *
     * @return the {@link Priority}
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * Sets the priority of this task.
     *
     * @param priority the new priority
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    /**
     * Updates the description of this task.
     *
     * @param description the new description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the status icon for display.
     *
     * @return "X" if done, " " otherwise
     */
    public String getStatusIcon() {
        return isDone ? "X" : " ";
    }

    @Override
    public String toString() {
        return "[" + getStatusIcon() + "][" + priority.getSymbol() + "] " + description;
    }

    /**
     * Returns a string representation suitable for saving to a file.
     *
     * @return the file-format string
     */
    public abstract String toFileString();
}
