package duke.task;

/**
 * Represents a task that requires a fixed amount of time to complete,
 * but has no specific scheduled start time.
 */
public class FixedDurationTask extends Task {
    private double durationHours;

    /**
     * Constructs a FixedDurationTask with the given description and duration.
     *
     * @param description   the task description
     * @param durationHours the estimated duration in hours
     */
    public FixedDurationTask(String description, double durationHours) {
        super(description);
        this.durationHours = durationHours;
    }

    /**
     * Returns the estimated duration in hours.
     *
     * @return the duration in hours
     */
    public double getDurationHours() {
        return durationHours;
    }

    @Override
    public String toString() {
        String dur = durationHours == (long) durationHours
                ? String.valueOf((long) durationHours)
                : String.valueOf(durationHours);
        return "[F]" + super.toString() + " (duration: " + dur + "h)";
    }

    @Override
    public String toFileString() {
        return "F | " + (isDone ? "1" : "0") + " | " + description
                + " | " + durationHours + " | " + priority.name()
                + " | " + getTagsFileString() + " | " + getRecurrenceFileString()
                + " | " + getAfterIndexFileString();
    }
}
