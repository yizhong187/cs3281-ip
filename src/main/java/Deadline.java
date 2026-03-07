import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Deadline extends Task {
    private String by;
    private LocalDate byDate;

    public Deadline(String description, String by) {
        super(description);
        this.by = by;
        try {
            this.byDate = LocalDate.parse(by);
        } catch (DateTimeParseException e) {
            this.byDate = null;
        }
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
