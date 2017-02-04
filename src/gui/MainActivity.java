package gui;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextArea;
import com.vdurmont.emoji.EmojiParser;
import engine.MainEngine;
import informer_api.conversation.Conversation;
import informer_api.conversation.Message;
import informer_api.conversation.Person;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * MainActivity is a gui application and a main program entry. It takes care of displaying data and starting other
 * services. It does not perform any network or other similar blocking operations (except for displaying dialog
 * windows).
 * </p>
 * <p>
 * Some notes regarding design - most of the work done in MainActivity is quite straightforward and the functions are
 * (in my opinion) well documented. But the access modifier for connectionWaitDialog can be tricky. Why is it static?
 * The method that is invoked after clicking "start" MenuItem has reference set to different MainActivity (probably
 * this could be solved by assigning onAction listener for MenuItem in code rather than .fxml file).
 * </p>
 */
public class MainActivity extends Application {
    public static final String APPLICATION_NAME = "Informer";
    private static Dialog<Void> connectionWaitDialog; // this is static because starting server via menuItem invokes whole procedure from other MainActivity instance!
    private int serverPort = 8888;
    private Stage window;
    private JFXTextArea messageText;
    private JFXButton sendButton;
    private JFXListView<Person> conversations;
    private JFXListView<Message> conversation;
    private ObservableList<Person> people = FXCollections.observableArrayList();
    private ObservableList<Message> messages = FXCollections.observableArrayList();

    @Override
    public void start(Stage window) throws Exception {
        this.window = window;
        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        Scene scene = new Scene(root);
        setFieldReferences(scene);
        if (messageText == null || sendButton == null || conversations == null || conversation == null)
            throw new IllegalArgumentException("Cannot find required fields on the provided scene!");
        formatFields();
        MainEngine.getInstance().setMainGuiReference(this);
        window.setTitle(APPLICATION_NAME);
        window.setScene(scene);
        window.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        MainEngine.getInstance().stopAllWork(false);
        System.out.println("Application closing");
    }

    /**
     * <p>
     * Loads provided messages into conversation view where user can view them.
     * After loading conversation, list view is scrolled to the bottom to show most recent messages.
     * </p>
     *
     * @param conversation - messages to display
     */
    public void loadConversation(Conversation conversation) {
        messages.clear();
        messages.addAll(conversation.getMessages());
        this.conversation.scrollTo(conversation.getMessages().size() - 1);
    }

    /**
     * <p>
     * Loads provided people into contact list and displays their info.
     * This method sorts list by phone numbers in a descending order.
     * </p>
     *
     * @param peopleList - list of contacts to have conversation with
     */
    public void loadPeople(List<Person> peopleList) {
        people.clear();
        peopleList.sort((Person p1, Person p2) -> p1.getNumber().compareTo(p2.getNumber()));
        people.addAll(peopleList);
    }

    /**
     * <p>
     * Shows dialog window informing about new messages and their content.
     * </p>
     *
     * @param newConversation - object containing both person and messages data
     */
    public void showNewMessageInformation(Conversation newConversation) {
        Person person = newConversation.getPerson();
        StringBuilder messagesContent = new StringBuilder();
        for (Message m : newConversation.getMessages()) messagesContent.append(m.getText() + "\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("New Message");
        alert.setHeaderText("You got new message(s) from " + person.getNickname() + " (" + person.getNumber() + ")");
        alert.setContentText(messagesContent.toString());
        alert.showAndWait();
    }

    /**
     * <p>
     * Clears everything shown on the screen.
     * </p>
     */
    public void clearAllViews() {
        people.clear();
        messages.clear();
    }

    /**
     * <p>
     * Shows waiting progress dialog. This method should be invoked by on JavaFX thread.
     * To cancel the dialog invoke closeWaitingDialog() method.
     * </p>
     */
    public void showWaitingDialog() {
        connectionWaitDialog = new Dialog<>();
        connectionWaitDialog.initModality(Modality.WINDOW_MODAL);
        connectionWaitDialog.initOwner(this.window);//stage here is the stage of your webview
        connectionWaitDialog.initStyle(StageStyle.UTILITY);
        Label loader = new Label("Waiting for connection on port " + serverPort);
        loader.setContentDisplay(ContentDisplay.CENTER);
        loader.setGraphic(new ProgressIndicator());
        connectionWaitDialog.getDialogPane().setGraphic(loader);
        connectionWaitDialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
        connectionWaitDialog.setOnCloseRequest((dialogEvent) -> MainEngine.getInstance().stopServices());
        connectionWaitDialog.showAndWait();
    }

    /**
     * <p>
     * Closes waiting progress dialog if one is displayed.
     * </p>
     */
    public void closeWaitingDialog() {
        if (connectionWaitDialog != null && connectionWaitDialog.isShowing()) {
            connectionWaitDialog.setOnCloseRequest(null);
            connectionWaitDialog.close();
        }
    }

    /**
     * <p>
     * OnClick callback for MenuItem Start.
     * </p>
     *
     * @param e
     */
    public void startServerProcedure(ActionEvent e) {
        MainEngine.getInstance().stopServices();
        TextInputDialog portDialog = new TextInputDialog(String.valueOf(serverPort));
        portDialog.setContentText("Input server port number");
        Optional<String> result = portDialog.showAndWait();
        if (result.isPresent()) {
            try {
                serverPort = Integer.parseInt(result.get());
                MainEngine.getInstance().startServer(serverPort);
                showWaitingDialog();
            } catch (NumberFormatException | IOException exception) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText(exception.getMessage());
                alert.showAndWait();
            }
        }

    }

    /**
     * <p>
     * Shows dialog about this application.
     * </p>
     *
     * @param e
     */
    public void showAbout(ActionEvent e) {
        Alert aboutDialog = new Alert(Alert.AlertType.INFORMATION);
        aboutDialog.setTitle("Informer");
        aboutDialog.setHeaderText(null);
        aboutDialog.setContentText("Copyright by Piotr Waszkiewicz");
        aboutDialog.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void setFieldReferences(Scene scene) {
        messageText = (JFXTextArea) scene.lookup("#message-text");
        sendButton = (JFXButton) scene.lookup("#send-message");
        conversations = (JFXListView<Person>) scene.lookup("#conversations");
        conversation = (JFXListView<Message>) scene.lookup("#conversation");
    }

    private void formatFields() {
        messageText.setWrapText(true);
        messageText.setOnKeyTyped(event -> {
            if (event.getCode() == KeyCode.ENTER) sendButton.fire();
            else {
                int newCaretPosition = messageText.getCaretPosition();
                String text = EmojiParser.parseToUnicode(messageText.getText());
                messageText.setText(text);
                messageText.positionCaret(newCaretPosition);
            }
        });
        sendButton.setOnMouseClicked((MouseEvent e) -> {
            if (messageText.getText().length() > 0) {
                MainEngine.getInstance().sendMessageToPhone(messageText.getText());
                messageText.clear();
            }
        });

        // TODO: Custom cell view breaks ripple effect...?
        conversations.setItems(people);
        conversations.setCellFactory((ListView<Person> view) -> {
            ContactCell cell = new ContactCell();
            cell.prefWidthProperty().bind(conversations.widthProperty().subtract(25));
            return cell;
        });
        conversation.setItems(messages);
        conversation.setCellFactory((ListView<Message> view) -> {
            MessageCell cell = new MessageCell();
            cell.prefWidthProperty().bind(conversation.widthProperty().subtract(25));
            return cell;
        });
    }
}
