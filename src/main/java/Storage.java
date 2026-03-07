import java.io.*;
import java.util.ArrayList;

public class Storage {
    private String filePath;

    public Storage(String filePath) {
        this.filePath = filePath;
    }

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
