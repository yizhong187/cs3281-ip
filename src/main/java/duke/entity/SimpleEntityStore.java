package duke.entity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic pipe-delimited file store for simple entity lists.
 * Each entity is stored as one line of {@code "|"}-separated fields.
 */
public class SimpleEntityStore {
    private final String filePath;
    private List<String[]> entries;

    /**
     * Constructs a store backed by the given file.
     * Loads existing entries immediately.
     *
     * @param filePath path to the backing file
     */
    public SimpleEntityStore(String filePath) {
        this.filePath = filePath;
        this.entries = new ArrayList<>();
        load();
    }

    /**
     * Returns all entries as a list of field arrays.
     *
     * @return the entries list
     */
    public List<String[]> getAll() {
        return entries;
    }

    /**
     * Returns the number of entries.
     *
     * @return the entry count
     */
    public int size() {
        return entries.size();
    }

    /**
     * Adds a new entry with the given fields and saves the file.
     *
     * @param fields the field values for this entry
     */
    public void add(String... fields) {
        entries.add(fields);
        save();
    }

    /**
     * Removes the entry at the given 0-based index and saves the file.
     *
     * @param index the 0-based index to remove
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void remove(int index) {
        entries.remove(index);
        save();
    }

    private void load() {
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    entries.add(line.split(" \\| ", -1));
                }
            }
        } catch (IOException e) {
            // start with empty list on error
        }
    }

    private void save() {
        File file = new File(filePath);
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String[] entry : entries) {
                writer.write(String.join(" | ", entry));
                writer.newLine();
            }
        } catch (IOException e) {
            // ignore save errors
        }
    }
}
