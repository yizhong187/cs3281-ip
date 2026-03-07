package duke.task;

import duke.util.DateParser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Represents a task that must be completed within a specified date range.
 * The task has a start and end date and should be done somewhere in between.
 */
public class WithinPeriodTask extends Task {
    private String from;
    private String to;
    private LocalDate fromDate;
    private LocalDate toDate;

    /**
     * Constructs a WithinPeriodTask with the given description and date range.
     *
     * @param description the task description
     * @param from        the start of the period (inclusive)
     * @param to          the end of the period (inclusive)
     */
    public WithinPeriodTask(String description, String from, String to) {
        super(description);
        this.from = from;
        this.to = to;
        this.fromDate = DateParser.parse(from);
        this.toDate = DateParser.parse(to);
    }

    /**
     * Returns the parsed start date, or null if unparseable.
     *
     * @return the from LocalDate
     */
    public LocalDate getFromDate() {
        return fromDate;
    }

    /**
     * Returns the parsed end date, or null if unparseable.
     *
     * @return the to LocalDate
     */
    public LocalDate getToDate() {
        return toDate;
    }

    /**
     * Returns the raw start date string.
     *
     * @return the from string
     */
    public String getFrom() {
        return from;
    }

    /**
     * Returns the raw end date string.
     *
     * @return the to string
     */
    public String getTo() {
        return to;
    }

    private String formatDate(LocalDate date, String raw) {
        if (date != null) {
            return date.format(DateTimeFormatter.ofPattern("MMM dd yyyy"));
        }
        return raw;
    }

    @Override
    public String toString() {
        return "[W]" + super.toString()
                + " (do within: " + formatDate(fromDate, from)
                + " to " + formatDate(toDate, to) + ")";
    }

    @Override
    public String toFileString() {
        return "W | " + (isDone ? "1" : "0") + " | " + description
                + " | " + from + " | " + to + " | " + priority.name()
                + " | " + getTagsFileString() + " | " + getRecurrenceFileString()
                + " | " + getAfterIndexFileString();
    }
}
