package aria.ui;

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

        // Stretch HBox to full container width, then cap label at HBox minus avatar+gaps
        setMaxWidth(Double.MAX_VALUE);
        // avatar(40) + spacing(10) + left+right padding(24) + scroll gutter buffer(4) = 78
        dialog.maxWidthProperty().bind(this.widthProperty().subtract(78));
    }

    /** Flips the dialog box so that the avatar is on the left and text on the right. */
    private void flip() {
        ObservableList<Node> tmp = FXCollections.observableArrayList(this.getChildren());
        Collections.reverse(tmp);
        getChildren().setAll(tmp);
        setAlignment(Pos.TOP_LEFT);
    }

    /**
     * Creates a dialog box for the user (avatar right, blue bubble, right-aligned).
     *
     * @param text the user's message
     * @param img  the user's avatar image
     * @return a styled DialogBox for the user
     */
    public static DialogBox getUserDialog(String text, Image img) {
        var db = new DialogBox(text, img);
        db.dialog.setStyle(
                "-fx-background-color: #5b8dee;"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: 16 4 16 16;"
                + "-fx-padding: 10 14 10 14;"
                + "-fx-font-size: 13;");
        db.dialog.setEffect(new javafx.scene.effect.DropShadow(4, 0, 2,
                javafx.scene.paint.Color.rgb(0, 0, 0, 0.12)));
        return db;
    }

    /**
     * Creates a dialog box for Aria (avatar left, white bubble, left-aligned).
     *
     * @param text the bot's response message
     * @param img  the bot's avatar image
     * @return a styled DialogBox for Aria
     */
    public static DialogBox getAriaDialog(String text, Image img) {
        var db = new DialogBox(text, img);
        db.dialog.setStyle(
                "-fx-background-color: #ffffff;"
                + "-fx-text-fill: #2c3e50;"
                + "-fx-background-radius: 4 16 16 16;"
                + "-fx-padding: 10 14 10 14;"
                + "-fx-font-size: 13;");
        db.dialog.setEffect(new javafx.scene.effect.DropShadow(4, 0, 2,
                javafx.scene.paint.Color.rgb(0, 0, 0, 0.08)));
        db.flip();
        return db;
    }
}
