package gui;

import com.sun.org.apache.xml.internal.security.signature.ReferenceNotInitializedException;
import engine.MainEngine;
import informer_api.conversation.Person;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;

/**
 * <p>
 * Class displaying person for conversation data.
 * </p>
 * Created by Piotr Waszkiewicz on 31.01.17.
 */
class ContactCell extends ListCell<Person> {
    private Person person;
    private HBox contactWithPhoto = new HBox();
    private VBox contactInfo = new VBox();
    private Pane pane = new Pane();
    private ImageView photoImage = new ImageView();
    private Label contactName = new Label(), contactNumber = new Label();

    ContactCell() {
        super();
        contactInfo.getChildren().addAll(contactName, contactNumber);
        contactWithPhoto.getChildren().addAll(photoImage, contactInfo);
        VBox.setVgrow(contactName, Priority.ALWAYS);
        HBox.setHgrow(pane, Priority.ALWAYS);
        contactWithPhoto.setOnMouseClicked((MouseEvent e) -> loadConversationWithSelectedPerson());
    }

    @Override
    protected void updateItem(Person item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);
        if (empty) {
            setGraphic(null);
        } else {
            this.person = item;
            contactName.setText(item.getNickname());
            contactNumber.setText(item.getNumber());
            contactName.setAlignment(Pos.BASELINE_LEFT);
            contactNumber.setAlignment(Pos.BASELINE_LEFT);
            contactInfo.setAlignment(Pos.BASELINE_LEFT);
            contactInfo.setPadding(new Insets(5));
            if (person.getPhoto() != null) {
                photoImage.setImage(new Image(new ByteArrayInputStream(person.getPhoto()), 30, 30, true, true));
            } else photoImage.setImage(null);
            setGraphic(contactWithPhoto);
        }
    }

    private void loadConversationWithSelectedPerson() {
        try {
            System.out.println("Clicked on person " + person.getNickname());
            MainEngine.getInstance().loadConversation(person);
        } catch (ReferenceNotInitializedException e) {
            System.err.println("Tried to load conversation before main window was showed?");
        }
    }
}
