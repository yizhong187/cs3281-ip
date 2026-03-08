package aria.task;

import aria.util.DateParser;

import java.time.LocalDate;

/**
 * Represents a task that occurs over a time range (from/to).
 * Parses from/to strings via Natty for structured date comparison.
 */
public class Event extends Task {
    private String from;
    private String to;
    private LocalDate fromDate;
    private LocalDate toDate;

    /**
     * Constructs an Event with the given description, start time, and end time.
     * The from/to strings are parsed via Natty for structured date support.
     *
     * @param description the event description
     * @param from        the start time/date string
     * @param to          the end time/date string
     */
    public Event(String description, String from, String to) {
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
        this.fromDate = DateParser.parse(from);
    }

    /**
     * Updates the end time string.
     *
     * @param to the new end time
     */
    public void setTo(String to) {
        this.to = to;
        this.toDate = DateParser.parse(to);
    }

    @Override
    public String toString() {
        return "[E]" + super.toString() + " (from: " + from + " to: " + to + ")";
    }

    @Override
    public String toFileString() {
        return "E | " + (isDone ? "1" : "0") + " | " + description
                + " | " + from + " | " + to + " | " + priority.name() + " | " + getTagsFileString()
                + " | " + getRecurrenceFileString() + " | " + getAfterIndexFileString();
    }
}
