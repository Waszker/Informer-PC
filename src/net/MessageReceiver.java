package net;

import com.sun.org.apache.xml.internal.security.signature.ReferenceNotInitializedException;
import engine.MainEngine;
import informer_api.conversation.Conversation;
import informer_api.conversation.Message;
import informer_api.conversation.Person;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Map;

/**
 * <p>
 * MessageReceiver class is responsible for receiving objects from connected phone.
 * </p>
 * Created by Piotr Waszkiewicz on 31.01.17.
 */
class MessageReceiver extends Thread {
    ObjectInputStream inStream;
    private boolean shouldWork;

    MessageReceiver(Socket connection) throws IOException {
        inStream = new ObjectInputStream(connection.getInputStream());
        shouldWork = true;
    }

    @Override
    public void run() {
        while (shouldWork) {
            try {
                reactToReceivedObject(inStream.readObject());
            } catch (IOException e) {
                // TODO: Broken connection should result in stopping whole service?
                System.err.println("MessageReceiver: " + e.getMessage());
                if (shouldWork) restartServer();
            } catch (ClassNotFoundException e) {
            }
        }
        System.out.println("MessageReceiver stopping work");
    }

    void cancel() {
        try {
            shouldWork = false;
            inStream.close();
        } catch (IOException e) {
        }
    }

    private void reactToReceivedObject(Object object) {
        if (object == null) return;
        if (object instanceof Map) newDatabaseInstance((Map<Person, Conversation>) object);
        else if (object instanceof Conversation) synchronizeNewMessages((Conversation) object);
    }

    private void newDatabaseInstance(Map<Person, Conversation> conversations) {
        try {
            MainEngine.getInstance().synchronizeDatabase(conversations);
        } catch (ReferenceNotInitializedException e) {
            System.err.println("Message receiver could not synchronize database. Error: " + e.getMessage());
        }
    }

    private void synchronizeNewMessages(Conversation newMessages) {
        try {
            MainEngine.getInstance().synchronizeNewMessages(newMessages);
        } catch (ReferenceNotInitializedException e) {
        }
    }

    private void restartServer() {
        try {
            MainEngine.getInstance().changeConnectionState(false);
            MainEngine.getInstance().stopAllWork(true);
        } catch (ReferenceNotInitializedException e) {
        }
    }
}
