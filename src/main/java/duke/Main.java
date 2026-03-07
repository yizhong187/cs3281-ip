package duke;

import java.io.IOException;

import duke.ui.MainWindow;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/** A GUI for Aria using FXML. */
public class Main extends Application {

    private Duke duke = new Duke();

    /** Default constructor required by JavaFX. */
    public Main() {}

    /**
     * Starts the JavaFX application by loading the FXML layout.
     *
     * @param stage the primary stage
     */
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/view/MainWindow.fxml"));
            AnchorPane ap = fxmlLoader.load();
            Scene scene = new Scene(ap);
            stage.setScene(scene);
            stage.setMinWidth(380);
            stage.setMinHeight(400);
            stage.setX(0);
            stage.setY(0);
            fxmlLoader.<MainWindow>getController().setDuke(duke);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
