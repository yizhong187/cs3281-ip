package duke;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests that sending messages produces responses and clears input. */
@ExtendWith(ApplicationExtension.class)
public class Part3Test {

    @Start
    public void start(Stage stage) throws Exception {
        new Main().start(stage);
    }

    @Test
    public void sendingMessageShowsResponse(FxRobot robot) {
        robot.clickOn("#userInput").write("todo read book");
        robot.clickOn("#sendButton");

        assertTrue(
            robot.lookup(".label").queryAllAs(Label.class)
                 .stream().anyMatch(l -> l.getText().contains("Got it. I've added this task"))
        );
    }

    @Test
    public void inputFieldClearedAfterSend(FxRobot robot) {
        robot.clickOn("#userInput").write("todo return book");
        robot.clickOn("#sendButton");

        assertTrue(robot.lookup("#userInput").queryAs(TextField.class).getText().isEmpty());
    }
}
