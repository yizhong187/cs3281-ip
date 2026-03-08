package aria;

import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.junit.jupiter.api.extension.ExtendWith;

/** Tests that the main JavaFX window initialises with a greeting label. */
@ExtendWith(ApplicationExtension.class)
public class Part1Test {

    @Start
    public void start(Stage stage) throws Exception {
        new Main().start(stage);
    }

    @Test
    public void helloWorldLabelExists(FxRobot robot) {
        robot.lookup("Hello World!").queryLabeled();
    }
}
