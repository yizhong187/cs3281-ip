package aria;

import javafx.application.Application;

/** A launcher class to work around classpath issues with JavaFX. */
public class Launcher {
    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
