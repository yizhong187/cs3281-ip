package duke.ui;

import java.io.IOException;
import java.util.Collections;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

/**
 * Represents a dialog box consisting of an ImageView (speaker's avatar)
 * and a label containing the speaker's message.
 */
public class DialogBox extends HBox {

    @FXML private Label dialog;
    @FXML private ImageView displayPicture;

    private DialogBox(String text, Image img) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainWindow.class.getResource("/view/DialogBox.fxml"));
            fxmlLoader.setController(this);
            fxmlLoader.setRoot(this);
            fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dialog.setText(text);
        displayPicture.setImage(img);

        double r = displayPicture.getFitWidth() / 2;
        Circle clip = new Circle(r, r, r);
        displayPicture.setClip(clip);
    }

    /** Flips the dialog box so that the avatar is on the left and text on the right. */
    private void flip() {
        ObservableList<Node> tmp = FXCollections.observableArrayList(this.getChildren());
        Collections.reverse(tmp);
        getChildren().setAll(tmp);
        setAlignment(Pos.TOP_LEFT);
    }

    /**
     * Creates a dialog box for the user (avatar on the right, blue background).
     *
     * @param text the user's message
     * @param img  the user's avatar image
     * @return a styled DialogBox for the user
     */
    public static DialogBox getUserDialog(String text, Image img) {
        var db = new DialogBox(text, img);
        db.dialog.setStyle(
                "-fx-background-color: #5b8dee; -fx-text-fill: white; "
                + "-fx-background-radius: 12; -fx-padding: 8 12 8 12;");
        return db;
    }

    /**
     * Creates a dialog box for Aria (avatar on the left, grey background).
     *
     * @param text the bot's response message
     * @param img  the bot's avatar image
     * @return a styled DialogBox for Aria
     */
    public static DialogBox getDukeDialog(String text, Image img) {
        var db = new DialogBox(text, img);
        db.dialog.setStyle(
                "-fx-background-color: #e8e8e8; -fx-text-fill: #222; "
                + "-fx-background-radius: 12; -fx-padding: 8 12 8 12;");
        db.flip();
        return db;
    }
}
