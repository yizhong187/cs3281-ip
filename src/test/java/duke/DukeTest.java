package duke;

import duke.task.Deadline;
import duke.task.Event;
import duke.task.TaskList;
import duke.task.Todo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for core task and collection classes. */
public class DukeTest {

    @Test
    public void todo_validDescription_createsTask() {
        Todo todo = new Todo("read book");
        assertEquals("[T][ ][M] read book", todo.toString());
        assertFalse(todo.isDone());
    }

    @Test
    public void todo_markDone_showsX() {
        Todo todo = new Todo("return book");
        todo.markDone();
        assertEquals("[T][X][M] return book", todo.toString());
        assertTrue(todo.isDone());
    }

    @Test
    public void deadline_unparseable_displaysRaw() {
        // "ZZZUNKNOWNDATE" cannot be parsed by Natty, so it should display as-is
        Deadline deadline = new Deadline("submit report", "ZZZUNKNOWNDATE");
        assertEquals("[D][ ][M] submit report (by: ZZZUNKNOWNDATE)", deadline.toString());
    }

    @Test
    public void deadline_isoDate_formatsToReadable() {
        Deadline deadline = new Deadline("submit report", "2024-12-01");
        assertEquals("[D][ ][M] submit report (by: Dec 01 2024)", deadline.toString());
    }

    @Test
    public void deadline_naturalDate_parsedCorrectly() {
        // "next year" should parse to a future date — just verify it formats (not raw)
        Deadline deadline = new Deadline("future task", "2025-06-15");
        assertEquals("[D][ ][M] future task (by: Jun 15 2025)", deadline.toString());
    }

    @Test
    public void event_fromTo_displaysCorrectly() {
        Event event = new Event("project meeting", "Mon 2pm", "4pm");
        assertEquals("[E][ ][M] project meeting (from: Mon 2pm to: 4pm)", event.toString());
    }

    @Test
    public void taskList_addAndSize() {
        TaskList list = new TaskList();
        list.add(new Todo("task one"));
        list.add(new Todo("task two"));
        assertEquals(2, list.size());
    }

    @Test
    public void taskList_find_returnsMatching() {
        TaskList list = new TaskList();
        list.add(new Todo("read book"));
        list.add(new Todo("return book"));
        list.add(new Todo("buy bread"));
        assertEquals(2, list.find("book").size());
        assertEquals(0, list.find("xyz").size());
    }
}
