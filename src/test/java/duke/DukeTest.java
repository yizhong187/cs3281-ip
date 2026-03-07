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
        assertEquals("[T][ ] read book", todo.toString());
        assertFalse(todo.isDone());
    }

    @Test
    public void todo_markDone_showsX() {
        Todo todo = new Todo("return book");
        todo.markDone();
        assertEquals("[T][X] return book", todo.toString());
        assertTrue(todo.isDone());
    }

    @Test
    public void deadline_validDateString_displaysCorrectly() {
        Deadline deadline = new Deadline("submit report", "Sunday");
        assertEquals("[D][ ] submit report (by: Sunday)", deadline.toString());
    }

    @Test
    public void deadline_isoDate_formatsToReadable() {
        Deadline deadline = new Deadline("submit report", "2024-12-01");
        assertEquals("[D][ ] submit report (by: Dec 01 2024)", deadline.toString());
    }

    @Test
    public void event_fromTo_displaysCorrectly() {
        Event event = new Event("project meeting", "Mon 2pm", "4pm");
        assertEquals("[E][ ] project meeting (from: Mon 2pm to: 4pm)", event.toString());
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
