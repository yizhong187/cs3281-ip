package duke.task;

/**
 * Represents a task that occurs over a time range (from/to).
 */
public class Event extends Task {
    private String from;
    private String to;

    /**
     * Constructs an Event with the given description, start time, and end time.
     *
     * @param description the event description
     * @param from        the start time/date string
     * @param to          the end time/date string
     */
    public Event(String description, String from, String to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    /**
     * Returns the start time string.
     *
     * @return from string
     */
    public String getFrom() {
        return from;
    }

    /**
     * Returns the end time string.
     *
     * @return to string
     */
    public String getTo() {
        return to;
    }

    /**
     * Updates the start time string.
     *
     * @param from the new start time
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Updates the end time string.
     *
     * @param to the new end time
     */
    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public String toString() {
        return "[E]" + super.toString() + " (from: " + from + " to: " + to + ")";
    }

    @Override
    public String toFileString() {
        return "E | " + (isDone ? "1" : "0") + " | " + description
                + " | " + from + " | " + to + " | " + priority.name() + " | " + getTagsFileString();
    }
}
