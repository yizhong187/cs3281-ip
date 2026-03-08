package aria.storage;

import aria.AriaException;
import aria.task.Deadline;
import aria.task.Event;
import aria.task.Task;
import aria.task.TaskList;
import aria.task.Todo;

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
    private String archivePath;

    /**
     * Constructs a Storage instance for the given file path.
     *
     * @param filePath the path to the data file
     */
    public Storage(String filePath) {
        assert filePath != null && !filePath.isEmpty() : "File path must not be null or empty";
        this.filePath = filePath;
        File f = new File(filePath);
        String parent = f.getParent() != null ? f.getParent() : ".";
        this.archivePath = parent + File.separator + "archive.txt";
    }

    /**
     * Appends a task to the archive file.
     *
     * @param task the task to archive
     * @throws AriaException if an I/O error occurs
     */
    public void appendToArchive(Task task) throws AriaException {
        File file = new File(archivePath);
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(task.toFileString());
            writer.newLine();
        } catch (IOException e) {
            throw new AriaException("Error writing to archive: " + e.getMessage());
        }
    }

    /**
     * Loads all tasks from the archive file.
     *
     * @return list of archived tasks
     * @throws AriaException if an I/O error occurs
     */
    public ArrayList<Task> loadArchive() throws AriaException {
        Storage archiveStorage = new Storage(archivePath);
        return archiveStorage.load();
    }

    /**
     * Captures the current file contents as a snapshot string for undo support.
     *
     * @return the file contents, or empty string if file does not exist
     */
    public String getSnapshot() {
        File file = new File(filePath);
        if (!file.exists()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            return "";
        }
        return sb.toString();
    }

    /**
     * Restores the task list from a previously captured snapshot string.
     *
     * @param snapshot the snapshot string (as returned by {@link #getSnapshot()})
     * @return the restored TaskList
     * @throws AriaException if an I/O error occurs
     */
    public TaskList restoreFromSnapshot(String snapshot) throws AriaException {
        File file = new File(filePath);
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(snapshot);
        } catch (IOException e) {
            throw new AriaException("Error restoring snapshot: " + e.getMessage());
        }
        return new aria.task.TaskList(load());
    }

    /**
     * Loads tasks from the data file.
     *
     * @return list of loaded tasks
     * @throws AriaException if an I/O error occurs
     */
    public ArrayList<Task> load() throws AriaException {
        ArrayList<Task> tasks = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            return tasks;
        }

        if (!file.canRead()) {
            throw new AriaException("Cannot read data file: " + filePath
                    + ". Check file permissions.");
        }

        int skipped = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                Task task = parseTask(line);
                if (task != null) {
                    tasks.add(task);
                } else {
                    skipped++;
                }
            }
        } catch (IOException e) {
            throw new AriaException("Error loading tasks: " + e.getMessage());
        }

        if (skipped > 0) {
            System.out.println("     Warning: Skipped " + skipped
                    + " malformed line(s) in data file.");
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
            int priorityIndex;
            switch (type) {
            case "T" -> {
                task = new Todo(description);
                priorityIndex = 3;
            }
            case "D" -> {
                if (parts.length < 4) {
                    return null;
                }
                task = new Deadline(description, parts[3]);
                priorityIndex = 4;
            }
            case "E" -> {
                if (parts.length < 5) {
                    return null;
                }
                task = new Event(description, parts[3], parts[4]);
                priorityIndex = 5;
            }
            case "F" -> {
                if (parts.length < 4) {
                    return null;
                }
                try {
                    double hours = Double.parseDouble(parts[3].trim());
                    task = new aria.task.FixedDurationTask(description, hours);
                } catch (NumberFormatException e) {
                    return null;
                }
                priorityIndex = 4;
            }
            case "W" -> {
                if (parts.length < 5) {
                    return null;
                }
                task = new aria.task.WithinPeriodTask(description, parts[3], parts[4]);
                priorityIndex = 5;
            }
            case "TE" -> {
                if (parts.length < 4) {
                    return null;
                }
                java.util.List<String> slots = java.util.Arrays.asList(parts[3].split("\\|\\|"));
                task = new aria.task.TentativeEvent(description, slots);
                priorityIndex = 4;
            }
            default -> {
                return null;
            }
            }

            // Load priority if present; default MEDIUM for backward compatibility
            if (parts.length > priorityIndex) {
                task.setPriority(aria.task.Priority.fromString(parts[priorityIndex]));
            }

            // Load tags if present (comma-separated, field after priority)
            int tagsIndex = priorityIndex + 1;
            if (parts.length > tagsIndex && !parts[tagsIndex].trim().isEmpty()) {
                for (String tag : parts[tagsIndex].split(",")) {
                    task.addTag(tag.trim());
                }
            }

            // Load recurrence if present (field after tags)
            int recurIndex = tagsIndex + 1;
            if (parts.length > recurIndex && !parts[recurIndex].trim().isEmpty()) {
                task.setRecurrence(parts[recurIndex].trim());
            }

            // Load afterIndex if present (field after recurrence)
            int afterIdx = recurIndex + 1;
            if (parts.length > afterIdx && !parts[afterIdx].trim().isEmpty()) {
                try {
                    task.setAfterIndex(Integer.parseInt(parts[afterIdx].trim()));
                } catch (NumberFormatException e) {
                    // ignore, defaults to -1
                }
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
     * @throws AriaException if an I/O error occurs
     */
    public void save(TaskList taskList) throws AriaException {
        assert taskList != null : "TaskList to save must not be null";
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
            throw new AriaException("Error saving tasks: " + e.getMessage());
        }
    }
}
