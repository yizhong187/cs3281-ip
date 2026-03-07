package duke.task;

/**
 * Represents a simple todo task with no date constraints.
 */
public class Todo extends Task {
    /**
     * Constructs a Todo with the given description.
     *
     * @param description the task description
     */
    public Todo(String description) {
        super(description);
    }

    @Override
    public String toString() {
        return "[T]" + super.toString();
    }

    @Override
    public String toFileString() {
        return "T | " + (isDone ? "1" : "0") + " | " + description + " | " + priority.name();
    }
}
