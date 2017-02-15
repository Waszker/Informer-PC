package gui;

import com.jfoenix.controls.JFXListCell;
import engine.MainEngine;
import informer_api.conversation.Message;
import informer_api.conversation.Person;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * <p>
 * Class displaying messages in a conversation.
 * </p>
 * Created by Piotr Waszkiewicz on 31.01.17.
 */
class MessageCell extends JFXListCell<Message> {
    private Person person;
    private HBox messageTextBox = new HBox();
    private VBox messageInfo = new VBox();
    private Label contactName = new Label(), messageText = new Label(), date = new Label();

    MessageCell() {
        super();
        messageTextBox.setPadding(new Insets(5, 0, 0, 0));
        messageTextBox.getChildren().addAll(messageText);
        messageInfo.getChildren().addAll(contactName, messageTextBox, date);

        HBox.setHgrow(messageText, Priority.ALWAYS);
        VBox.setVgrow(messageTextBox, Priority.ALWAYS);
        VBox.setVgrow(contactName, Priority.ALWAYS);
        VBox.setVgrow(date, Priority.ALWAYS);
    }

    @Override
    public void updateItem(Message item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);
        if (item == null || empty) {
            setGraphic(null);
        } else {
            person = MainEngine.getInstance().getCurrentlyOpenedConversation();
            if (person != null) contactName.setText(item.isSentByOwner() ? "Me" : person.getNickname());
            messageText.setText(item.getText());
            date.setText(item.getDate());

            // Styling
            messageText.setWrapText(true);
            contactName.setStyle("-fx-font-weight: bold;");
            date.setStyle("-fx-font-weight: lighter;");
            messageInfo.setAlignment(item.isSentByOwner() ? Pos.BASELINE_RIGHT : Pos.BASELINE_LEFT);
            messageTextBox.setAlignment(item.isSentByOwner() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            messageText.setTextAlignment(item.isSentByOwner() ? TextAlignment.RIGHT : TextAlignment.LEFT);
            messageInfo.setMouseTransparent(true);
            setGraphic(messageInfo);
        }
    }
}
