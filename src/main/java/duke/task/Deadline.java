package duke.task;

import duke.util.DateParser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Represents a task with a deadline (due date).
 * Supports natural language date strings via the Natty library (e.g. "next Monday", "Dec 25").
 */
public class Deadline extends Task {
    private final String by;
    private final LocalDate byDate;

    /**
     * Constructs a Deadline with the given description and due date string.
     * The date string is parsed using Natty for natural language support,
     * falling back gracefully if unparseable.
     *
     * @param description the task description
     * @param by          the due date string (e.g. "tomorrow", "2024-12-01", "next Monday")
     */
    public Deadline(String description, String by) {
        super(description);
        this.by = by;
        this.byDate = DateParser.parse(by);
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
     * Returns the raw by-date string as entered by the user.
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
