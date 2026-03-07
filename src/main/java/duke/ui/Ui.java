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

    /**
     * Prints one or more lines, each prefixed with indentation, wrapped in separator lines.
     *
     * @param lines the lines to print
     */
    private void printBlock(String... lines) {
        printLine();
        for (String line : lines) {
            System.out.println("     " + line);
        }
        printLine();
    }

    /** Displays the welcome message. */
    public void showWelcome() {
        printBlock("Hello! I'm Aria", "What can I do for you?");
    }

    /** Displays the goodbye message. */
    public void showGoodbye() {
        printBlock("Bye. Hope to see you again soon!");
    }

    /**
     * Displays an error message.
     *
     * @param message the error message to display
     */
    public void showError(String message) {
        printBlock(message);
    }

    /**
     * Displays confirmation that a task was added.
     *
     * @param task the task that was added
     * @param size the new list size
     */
    public void showTaskAdded(Task task, int size) {
        printBlock(
                "Got it. I've added this task:",
                "  " + task,
                "Now you have " + size + " tasks in the list."
        );
    }

    /**
     * Displays confirmation that a task was deleted.
     *
     * @param task the task that was removed
     * @param size the new list size
     */
    public void showTaskDeleted(Task task, int size) {
        printBlock(
                "Noted. I've removed this task:",
                "  " + task,
                "Now you have " + size + " tasks in the list."
        );
    }

    /**
     * Displays confirmation that a task was marked as done.
     *
     * @param task the task that was marked
     */
    public void showTaskMarked(Task task) {
        printBlock("Nice! I've marked this task as done:", "  " + task);
    }

    /**
     * Displays confirmation that a task was marked as not done.
     *
     * @param task the task that was unmarked
     */
    public void showTaskUnmarked(Task task) {
        printBlock("OK, I've marked this task as not done yet:", "  " + task);
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
        printBlock(
                "Available commands:",
                "list",
                "todo <desc>",
                "deadline <desc> /by <date>",
                "event <desc> /from <time> /to <time>",
                "mark <num>",
                "unmark <num>",
                "delete <num>",
                "find <keyword>",
                "bye"
        );
    }

    /**
     * Displays an arbitrary message wrapped in separator lines.
     *
     * @param message the message to display
     */
    public void showMessage(String message) {
        printBlock(message);
    }

    /** Displays a warning that saved tasks could not be loaded. */
    public void showLoadingError() {
        System.out.println("     Warning: Could not load saved tasks. Starting fresh.");
    }
}
