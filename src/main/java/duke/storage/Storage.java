package duke.storage;

import duke.DukeException;
import duke.task.Deadline;
import duke.task.Event;
import duke.task.Task;
import duke.task.TaskList;
import duke.task.Todo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Handles loading and saving of tasks to a text file.
 * The file format is pipe-delimited: {@code TYPE | STATUS | DESCRIPTION [| EXTRA...]}.
 */
public class Storage {
    private String filePath;

    /**
     * Constructs a Storage instance for the given file path.
     *
     * @param filePath the path to the data file
     */
    public Storage(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Loads tasks from the data file.
     *
     * @return list of loaded tasks
     * @throws DukeException if an I/O error occurs
     */
    public ArrayList<Task> load() throws DukeException {
        ArrayList<Task> tasks = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            return tasks;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Task task = parseTask(line);
                if (task != null) {
                    tasks.add(task);
                }
            }
        } catch (IOException e) {
            throw new DukeException("Error loading tasks: " + e.getMessage());
        }

        return tasks;
    }

    /**
     * Parses a single line from the data file into a Task.
     * Returns null if the line is malformed or unrecognised.
     *
     * @param line the line to parse
     * @return the parsed Task, or null on failure
     */
    private Task parseTask(String line) {
        String[] parts = line.split(" \\| ");
        if (parts.length < 3) {
            return null;
        }

        try {
            String type = parts[0];
            boolean isDone = parts[1].equals("1");
            String description = parts[2];

            Task task;
            switch (type) {
            case "T":
                task = new Todo(description);
                break;
            case "D":
                if (parts.length < 4) {
                    return null;
                }
                task = new Deadline(description, parts[3]);
                break;
            case "E":
                if (parts.length < 5) {
                    return null;
                }
                task = new Event(description, parts[3], parts[4]);
                break;
            default:
                return null;
            }

            if (isDone) {
                task.markDone();
            }
            return task;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Saves the current task list to the data file.
     *
     * @param taskList the task list to save
     * @throws DukeException if an I/O error occurs
     */
    public void save(TaskList taskList) throws DukeException {
        File file = new File(filePath);
        File dir = file.getParentFile();

        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Task task : taskList.getAll()) {
                writer.write(task.toFileString());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new DukeException("Error saving tasks: " + e.getMessage());
        }
    }
}
