package aria.util;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Utility class for parsing natural language date strings using the Natty library.
 * Falls back to ISO date parsing (yyyy-MM-dd) if Natty cannot parse the input.
 */
public final class DateParser {

    private static final Parser NATTY = new Parser();

    private DateParser() {}

    /**
     * Attempts to parse the given string as a date using natural language processing.
     * Recognises expressions like "next Monday", "tomorrow", "Dec 25", "2024-12-01".
     *
     * @param input the date string to parse
     * @return a {@link LocalDate} if parsing succeeds, or {@code null} if it cannot be parsed
     */
    public static LocalDate parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        List<DateGroup> groups = NATTY.parse(input.trim());
        if (!groups.isEmpty() && !groups.get(0).getDates().isEmpty()) {
            Date date = groups.get(0).getDates().get(0);
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        return null;
    }
}
