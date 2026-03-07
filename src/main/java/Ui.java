import java.util.ArrayList;
import java.util.Scanner;

public class Ui {
    private static final String LINE = "    ____________________________________________________________";
    private Scanner scanner;

    public Ui() {
        scanner = new Scanner(System.in);
    }

    public String readCommand() {
        return scanner.nextLine();
    }

    public boolean hasNextLine() {
        return scanner.hasNextLine();
    }

    private void printLine() {
        System.out.println(LINE);
    }

    public void showWelcome() {
        printLine();
        System.out.println("     Hello! I'm Aria");
        System.out.println("     What can I do for you?");
        printLine();
    }

    public void showGoodbye() {
        printLine();
        System.out.println("     Bye. Hope to see you again soon!");
        printLine();
    }

    public void showError(String message) {
        printLine();
        System.out.println("     " + message);
        printLine();
    }

    public void showTaskAdded(Task task, int size) {
        printLine();
        System.out.println("     Got it. I've added this task:");
        System.out.println("       " + task);
        System.out.println("     Now you have " + size + " tasks in the list.");
        printLine();
    }

    public void showTaskDeleted(Task task, int size) {
        printLine();
        System.out.println("     Noted. I've removed this task:");
        System.out.println("       " + task);
        System.out.println("     Now you have " + size + " tasks in the list.");
        printLine();
    }

    public void showTaskMarked(Task task) {
        printLine();
        System.out.println("     Nice! I've marked this task as done:");
        System.out.println("       " + task);
        printLine();
    }

    public void showTaskUnmarked(Task task) {
        printLine();
        System.out.println("     OK, I've marked this task as not done yet:");
        System.out.println("       " + task);
        printLine();
    }

    public void showTaskList(TaskList taskList) {
        printLine();
        System.out.println("     Here are the tasks in your list:");
        for (int i = 0; i < taskList.size(); i++) {
            System.out.println("     " + (i + 1) + "." + taskList.get(i));
        }
        printLine();
    }

    public void showFoundTasks(ArrayList<Task> tasks) {
        printLine();
        System.out.println("     Here are the matching tasks in your list:");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println("     " + (i + 1) + "." + tasks.get(i));
        }
        printLine();
    }

    public void showHelp() {
        printLine();
        System.out.println("     Available commands:");
        System.out.println("     • list");
        System.out.println("     • todo <desc>");
        System.out.println("     • deadline <desc> /by <date>");
        System.out.println("     • event <desc> /from <time> /to <time>");
        System.out.println("     • mark <num>");
        System.out.println("     • unmark <num>");
        System.out.println("     • delete <num>");
        System.out.println("     • find <keyword>");
        System.out.println("     • bye");
        printLine();
    }

    public void showLoadingError() {
        System.out.println("     Warning: Could not load saved tasks. Starting fresh.");
    }
}
