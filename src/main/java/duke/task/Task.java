package duke.task;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents an abstract task with a description, completion status, priority level, and tags.
 * Subclasses must implement {@code toFileString()} for file persistence.
 */
public abstract class Task {
    protected String description;
    protected boolean isDone;
    protected Priority priority;
    protected Set<String> tags;
    protected String recurrence;

    /**
     * Constructs a Task with the given description, initially not done and MEDIUM priority.
     *
     * @param description the task description
     */
    public Task(String description) {
        this.description = description;
        this.isDone = false;
        this.priority = Priority.MEDIUM;
        this.tags = new LinkedHashSet<>();
        this.recurrence = null;
    }

    /**
     * Returns the recurrence interval of this task ("daily", "weekly", "monthly"), or null.
     *
     * @return the recurrence string, or null if not recurring
     */
    public String getRecurrence() {
        return recurrence;
    }

    /**
     * Sets the recurrence interval for this task.
     *
     * @param recurrence the interval ("daily", "weekly", "monthly"), or null to clear
     */
    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }

    /**
     * Returns the recurrence string for file storage (empty string if not recurring).
     *
     * @return the recurrence file string
     */
    public String getRecurrenceFileString() {
        return recurrence != null ? recurrence : "";
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
     * Adds a tag to this task.
     *
     * @param tag the tag name (without #)
     */
    public void addTag(String tag) {
        tags.add(tag.toLowerCase());
    }

    /**
     * Removes a tag from this task.
     *
     * @param tag the tag name to remove
     */
    public void removeTag(String tag) {
        tags.remove(tag.toLowerCase());
    }

    /**
     * Returns the set of tags on this task.
     *
     * @return the tags set
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Returns true if this task has the given tag.
     *
     * @param tag the tag to check
     * @return true if tagged
     */
    public boolean hasTag(String tag) {
        return tags.contains(tag.toLowerCase());
    }

    /**
     * Returns a comma-separated string of tags for file storage, or empty string if no tags.
     *
     * @return the tags file string
     */
    public String getTagsFileString() {
        return String.join(",", tags);
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
        String base = "[" + getStatusIcon() + "][" + priority.getSymbol() + "] " + description;
        if (tags.isEmpty()) {
            return base;
        }
        String tagStr = tags.stream().map(t -> "#" + t)
                .collect(java.util.stream.Collectors.joining(" "));
        return base + " " + tagStr;
    }

    /**
     * Returns a string representation suitable for saving to a file.
     *
     * @return the file-format string
     */
    public abstract String toFileString();
}
