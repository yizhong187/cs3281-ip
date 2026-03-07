package duke.task;

/**
 * Represents the priority level of a task.
 */
public enum Priority {
    HIGH("H"), MEDIUM("M"), LOW("L");

    private final String symbol;

    Priority(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Returns the single-character symbol used in task display (H, M, L).
     *
     * @return the symbol string
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Parses a string to a Priority, case-insensitively.
     * Accepts "high"/"h", "medium"/"m", "low"/"l".
     *
     * @param s the string to parse
     * @return the matching Priority, defaulting to MEDIUM if unrecognised
     */
    public static Priority fromString(String s) {
        if (s == null) {
            return MEDIUM;
        }
        switch (s.trim().toLowerCase()) {
        case "high": case "h":
            return HIGH;
        case "low": case "l":
            return LOW;
        default:
            return MEDIUM;
        }
    }
}
