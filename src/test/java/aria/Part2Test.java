package aria;

import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.junit.jupiter.api.extension.ExtendWith;

/** Tests that the main window contains all required UI controls. */
@ExtendWith(ApplicationExtension.class)
public class Part2Test {

    @Start
    public void start(Stage stage) throws Exception {
        new Main().start(stage);
    }

    @Test
    public void sendButtonExists(FxRobot robot) {
        robot.lookup("#sendButton").queryButton();
    }

    @Test
    public void userInputFieldExists(FxRobot robot) {
        robot.lookup("#userInput").query();
    }

    @Test
    public void scrollPaneExists(FxRobot robot) {
        robot.lookup("#scrollPane").query();
    }
}
