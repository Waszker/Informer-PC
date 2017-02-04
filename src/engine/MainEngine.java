package engine;

import com.sun.org.apache.xml.internal.security.signature.ReferenceNotInitializedException;
import com.vdurmont.emoji.EmojiParser;
import gui.MainActivity;
import informer_api.conversation.Conversation;
import informer_api.conversation.Message;
import informer_api.conversation.Person;
import javafx.application.Platform;
import net.MessageService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * <p>
 * The main engine of the whole program. Takes care of synchronizing every part of the system.
 * </p>
 * Created by Piotr Waszkieiwcz on 31.01.17.
 */
public class MainEngine {
    private static MainEngine instance;
    private int port;
    private boolean isClosing = false;
    private MainActivity mainGui;
    private Database database;
    private MessageService messageService;
    private Person currentlyOpenedConversation;

    /**
     * <p>
     * Get instance of the main engine.
     * </p>
     *
     * @return instance of main engine
     */
    public static synchronized MainEngine getInstance() {
        if (instance == null) {
            instance = new MainEngine();
        }
        return instance;
    }

    /**
     * <p>
     * Stops all net services and prepares clean shutdown or restarts whole activity for another connection.
     * </p>
     *
     * @param isRestart - should await another connections
     */
    public void stopAllWork(boolean isRestart) {
        currentlyOpenedConversation = null;
        if (!isRestart) System.out.println("Preparing for shutdown");
        else System.out.println("Server restarting");

        isClosing = !isRestart;
        stopServices();
        if (isRestart) {
            try {
                startServer(port);
            } catch (IOException e) {
            }
        }
    }

    /**
     * <p>
     * Stops server services making it ready for future restart.
     * This is different from stopAllWork method which invokes this method but instead makes whole system unable to future work.
     * </p>
     */
    public void stopServices() {
        if (messageService != null) messageService.cancel();
        messageService = null;
        Platform.runLater(() -> mainGui.clearAllViews());
    }

    /**
     * <p>
     * Sets reference to main window. It will be used for loading important information.
     * </p>
     *
     * @param gui main window reference
     */
    public void setMainGuiReference(MainActivity gui) {
        mainGui = gui;
        if (database != null)
            Platform.runLater(() -> mainGui.loadPeople(new ArrayList<>(database.getConversations().keySet())));
    }

    /**
     * <p>
     * Starts server and awaits connection.
     * </p>
     *
     * @param port
     */
    public void startServer(int port) throws IOException {
        System.out.println("Server listening on " + port);
        this.port = port;
        messageService = new MessageService(port);
        messageService.start();
    }

    /**
     * <p>
     * Informs main program window about connection status.
     * </p>
     *
     * @param isConnected - information is somebody connected to server
     * @throws ReferenceNotInitializedException - if gui is not yet attached to engine
     */
    public void changeConnectionState(boolean isConnected) throws ReferenceNotInitializedException {
        if (isClosing) return;
        checkGuiReference();
        Platform.runLater(() -> {
            if (isConnected) mainGui.closeWaitingDialog();
            else mainGui.showWaitingDialog();
        });
    }

    /**
     * <p>
     * Loads messages for conversation with selected person.
     * </p>
     *
     * @param conversationWith
     * @throws ReferenceNotInitializedException
     */
    public void loadConversation(Person conversationWith) throws ReferenceNotInitializedException {
        checkGuiReference();
        Conversation conversation = database.getConversationWithPerson(conversationWith);
        if (conversation != null) {
            currentlyOpenedConversation = conversationWith;
            Platform.runLater(() -> mainGui.loadConversation(conversation));
        }
    }

    /**
     * <p>
     * Returns person that currently opened conversation applies to.
     * </p>
     *
     * @return
     */
    public Person getCurrentlyOpenedConversation() {
        return currentlyOpenedConversation;
    }

    /**
     * <p>
     * Sends message with provided body content (text) to the person who is visible in current conversation window.
     * </p>
     *
     * @param text - body of the message
     */
    public void sendMessageToPhone(String text) {
        if (currentlyOpenedConversation != null && messageService != null) {
            System.out.println("Sending message " + text);
            text = EmojiParser.parseToUnicode(text);
            Message message = new Message(currentlyOpenedConversation.getNumber(), text);
            messageService.sendMessage(message);
            appendNewlySentMessageToConversation(text);
        } else {
            System.err.println("Can't send message due to no opened conversations (" +
                    (currentlyOpenedConversation == null) + ") or no message service running (" +
                    (messageService == null) + ")");
        }
    }

    /**
     * <p>
     * Removes all data in an existing database and creates a new one.
     * </p>
     *
     * @param data
     * @throws ReferenceNotInitializedException
     */
    public void synchronizeDatabase(Map<Person, Conversation> data) throws ReferenceNotInitializedException {
        database = new Database(data);
        checkGuiReference();
        Platform.runLater(() -> mainGui.loadPeople(new ArrayList<>(data.keySet())));
    }

    /**
     * <p>
     * Adds new messages from conversation and updates conversation displayed if needed.
     * </p>
     *
     * @param newMessages
     * @throws ReferenceNotInitializedException
     */
    public void synchronizeNewMessages(Conversation newMessages) throws ReferenceNotInitializedException {
        if (database != null) {
            database.addNewMessages(newMessages);
            // TODO: Check if loadConversation is really needed (could use some better performance techniques maybe?)
            checkGuiReference();
            Platform.runLater(() -> mainGui.showNewMessageInformation(newMessages));
            if (currentlyOpenedConversation != null && currentlyOpenedConversation.equals(newMessages.getPerson()))
                loadConversation(newMessages.getPerson());
        }
    }

    private void appendNewlySentMessageToConversation(String text) {
        String date = (new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")).format(new Date());
        Conversation conversation = new Conversation(currentlyOpenedConversation);
        conversation.addMessage(new Message(true, date, text));
        database.addNewMessages(conversation);
        try {
            loadConversation(currentlyOpenedConversation);
        } catch (ReferenceNotInitializedException e) {
        }
    }

    private void checkGuiReference() throws ReferenceNotInitializedException {
        if (mainGui == null) throw new ReferenceNotInitializedException("Main gui not set! Cannot perform action.");
    }

    private MainEngine() {
    }
}
