import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
public class Part4Test {

    @Start
    public void start(Stage stage) throws Exception {
        new Main().start(stage);
    }

    @Test
    public void fxmlWindowLoads(FxRobot robot) {
        robot.lookup("#userInput").query();
        robot.lookup("#sendButton").queryButton();
        robot.lookup("#scrollPane").query();
    }

    @Test
    public void sendingMessageWorksAfterFxmlRefactor(FxRobot robot) {
        robot.clickOn("#userInput").write("todo fxml task");
        robot.clickOn("#sendButton");

        assertTrue(
            robot.lookup(".label").queryAllAs(Label.class)
                 .stream().anyMatch(l -> l.getText().contains("Got it. I've added this task"))
        );
    }
}
