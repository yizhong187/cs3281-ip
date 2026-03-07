package duke;

import duke.storage.Storage;
import duke.task.Deadline;
import duke.task.Event;
import duke.task.Task;
import duke.task.TaskList;
import duke.task.Todo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the Storage class. */
public class StorageTest {

    @TempDir
    Path tempDir;

    @Test
    public void load_fileNotExist_returnsEmptyList() throws DukeException {
        Storage storage = new Storage(tempDir.resolve("nonexistent.txt").toString());
        ArrayList<Task> tasks = storage.load();
        assertTrue(tasks.isEmpty());
    }

    @Test
    public void save_and_load_roundTrip() throws DukeException {
        String path = tempDir.resolve("tasks.txt").toString();
        Storage storage = new Storage(path);

        TaskList list = new TaskList();
        list.add(new Todo("read book"));
        Deadline d = new Deadline("submit", "2024-12-01");
        list.add(d);
        Event e = new Event("meeting", "Mon 2pm", "4pm");
        list.add(e);

        storage.save(list);
        ArrayList<Task> loaded = storage.load();

        assertEquals(3, loaded.size());
        assertEquals("[T][ ][M] read book", loaded.get(0).toString());
        assertEquals("[D][ ][M] submit (by: Dec 01 2024)", loaded.get(1).toString());
        assertEquals("[E][ ][M] meeting (from: Mon 2pm to: 4pm)", loaded.get(2).toString());
    }

    @Test
    public void load_markedDone_preservesStatus() throws DukeException {
        String path = tempDir.resolve("tasks.txt").toString();
        Storage storage = new Storage(path);

        TaskList list = new TaskList();
        Todo todo = new Todo("done task");
        todo.markDone();
        list.add(todo);

        storage.save(list);
        ArrayList<Task> loaded = storage.load();

        assertTrue(loaded.get(0).isDone());
    }

    @Test
    public void load_malformedLines_skipsGracefully() throws DukeException, IOException {
        File file = tempDir.resolve("bad.txt").toFile();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("GARBAGE LINE\n");
            fw.write("T | 0 | valid todo\n");
            fw.write("X | 0\n");
        }

        Storage storage = new Storage(file.getAbsolutePath());
        ArrayList<Task> tasks = storage.load();
        assertEquals(1, tasks.size());
        assertEquals("[T][ ][M] valid todo", tasks.get(0).toString());
    }

    @Test
    public void load_emptyLines_skippedSilently() throws DukeException, IOException {
        File file = tempDir.resolve("empty.txt").toFile();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("\n");
            fw.write("T | 0 | a task\n");
            fw.write("\n");
        }

        Storage storage = new Storage(file.getAbsolutePath());
        ArrayList<Task> tasks = storage.load();
        assertEquals(1, tasks.size());
    }

    @Test
    public void constructor_nullPath_throwsAssertionError() {
        assertThrows(AssertionError.class, () -> new Storage(null));
    }
}
