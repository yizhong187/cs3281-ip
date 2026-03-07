package duke.ui;

import duke.task.Task;
import duke.task.TaskList;

import java.util.ArrayList;
import java.util.Scanner;

/**
 * Handles all command-line user interaction for Aria.
 * Reads input via {@link Scanner} and prints formatted output.
 */
public class Ui {
    private static final String LINE =
            "    ____________________________________________________________";
    private Scanner scanner;

    /** Constructs a Ui instance backed by System.in. */
    public Ui() {
        scanner = new Scanner(System.in);
    }

    /**
     * Reads the next line of user input.
     *
     * @return the trimmed input line
     */
    public String readCommand() {
        return scanner.nextLine();
    }

    /**
     * Returns true if there is another line of input available.
     *
     * @return true if more input exists
     */
    public boolean hasNextLine() {
        return scanner.hasNextLine();
    }

    private void printLine() {
        System.out.println(LINE);
    }

    /** Displays the welcome message. */
    public void showWelcome() {
        printLine();
        System.out.println("     Hello! I'm Aria");
        System.out.println("     What can I do for you?");
        printLine();
    }

    /** Displays the goodbye message. */
    public void showGoodbye() {
        printLine();
        System.out.println("     Bye. Hope to see you again soon!");
        printLine();
    }

    /**
     * Displays an error message.
     *
     * @param message the error message to display
     */
    public void showError(String message) {
        printLine();
        System.out.println("     " + message);
        printLine();
    }

    /**
     * Displays confirmation that a task was added.
     *
     * @param task the task that was added
     * @param size the new list size
     */
    public void showTaskAdded(Task task, int size) {
        printLine();
        System.out.println("     Got it. I've added this task:");
        System.out.println("       " + task);
        System.out.println("     Now you have " + size + " tasks in the list.");
        printLine();
    }

    /**
     * Displays confirmation that a task was deleted.
     *
     * @param task the task that was removed
     * @param size the new list size
     */
    public void showTaskDeleted(Task task, int size) {
        printLine();
        System.out.println("     Noted. I've removed this task:");
        System.out.println("       " + task);
        System.out.println("     Now you have " + size + " tasks in the list.");
        printLine();
    }

    /**
     * Displays confirmation that a task was marked as done.
     *
     * @param task the task that was marked
     */
    public void showTaskMarked(Task task) {
        printLine();
        System.out.println("     Nice! I've marked this task as done:");
        System.out.println("       " + task);
        printLine();
    }

    /**
     * Displays confirmation that a task was marked as not done.
     *
     * @param task the task that was unmarked
     */
    public void showTaskUnmarked(Task task) {
        printLine();
        System.out.println("     OK, I've marked this task as not done yet:");
        System.out.println("       " + task);
        printLine();
    }

    /**
     * Displays the full task list.
     *
     * @param taskList the task list to display
     */
    public void showTaskList(TaskList taskList) {
        printLine();
        System.out.println("     Here are the tasks in your list:");
        for (int i = 0; i < taskList.size(); i++) {
            System.out.println("     " + (i + 1) + "." + taskList.get(i));
        }
        printLine();
    }

    /**
     * Displays a list of tasks matching a search.
     *
     * @param tasks the matching tasks
     */
    public void showFoundTasks(ArrayList<Task> tasks) {
        printLine();
        System.out.println("     Here are the matching tasks in your list:");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println("     " + (i + 1) + "." + tasks.get(i));
        }
        printLine();
    }

    /** Displays the list of available commands. */
    public void showHelp() {
        printLine();
        System.out.println("     Available commands:");
        System.out.println("     list");
        System.out.println("     todo <desc>");
        System.out.println("     deadline <desc> /by <date>");
        System.out.println("     event <desc> /from <time> /to <time>");
        System.out.println("     mark <num>");
        System.out.println("     unmark <num>");
        System.out.println("     delete <num>");
        System.out.println("     find <keyword>");
        System.out.println("     bye");
        printLine();
    }

    /** Displays a warning that saved tasks could not be loaded. */
    public void showLoadingError() {
        System.out.println("     Warning: Could not load saved tasks. Starting fresh.");
    }
}
