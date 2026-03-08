package aria.task;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a tentative event with multiple possible time slots.
 * Use {@code confirm INDEX SLOT} to lock in a slot and convert to a regular Event.
 */
public class TentativeEvent extends Task {
    private List<String> slots;

    /**
     * Constructs a TentativeEvent with the given description and time slots.
     *
     * @param description the event description
     * @param slots       the list of candidate time slots
     */
    public TentativeEvent(String description, List<String> slots) {
        super(description);
        this.slots = new ArrayList<>(slots);
    }

    /**
     * Returns the list of candidate time slots.
     *
     * @return the slots list
     */
    public List<String> getSlots() {
        return slots;
    }

    @Override
    public String toString() {
        String slotStr = String.join(", ", slots);
        return "[TE]" + super.toString() + " (slots: " + slotStr + ")";
    }

    @Override
    public String toFileString() {
        String slotsStr = String.join("||", slots);
        return "TE | " + (isDone ? "1" : "0") + " | " + description + " | " + slotsStr
                + " | " + priority.name() + " | " + getTagsFileString()
                + " | " + getRecurrenceFileString() + " | " + getAfterIndexFileString();
    }
}
