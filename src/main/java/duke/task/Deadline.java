package duke.task;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Represents a task with a deadline (due date).
 */
public class Deadline extends Task {
    private String by;
    private LocalDate byDate;

    /**
     * Constructs a Deadline with the given description and due date string.
     * If {@code by} is a valid ISO date (yyyy-MM-dd), it is parsed for formatted display.
     *
     * @param description the task description
     * @param by          the due date string
     */
    public Deadline(String description, String by) {
        super(description);
        this.by = by;
        try {
            this.byDate = LocalDate.parse(by);
        } catch (DateTimeParseException e) {
            this.byDate = null;
        }
    }

    /**
     * Returns the by-date for sorting and comparison.
     *
     * @return the {@link LocalDate} if parsed, or null
     */
    public LocalDate getByDate() {
        return byDate;
    }

    /**
     * Returns the raw by-date string.
     *
     * @return the by string
     */
    public String getBy() {
        return by;
    }

    private String getByDisplay() {
        if (byDate != null) {
            return byDate.format(DateTimeFormatter.ofPattern("MMM dd yyyy"));
        }
        return by;
    }

    @Override
    public String toString() {
        return "[D]" + super.toString() + " (by: " + getByDisplay() + ")";
    }

    @Override
    public String toFileString() {
        return "D | " + (isDone ? "1" : "0") + " | " + description + " | " + by;
    }
}
