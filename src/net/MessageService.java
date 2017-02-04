package net;

import com.sun.org.apache.xml.internal.security.signature.ReferenceNotInitializedException;
import engine.MainEngine;
import informer_api.conversation.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * <p>
 * Service used to retrieve and send information to connected phone.
 * </p>
 * Created by Piotr Waszkiewicz on 30.01.17.
 */
public class MessageService extends Thread {
    private boolean shouldWork;
    private ServerSocket listener;
    private MessageSender messageSender;
    private MessageReceiver messageReceiver;
    private Object lock = new Object();

    public MessageService(int port) throws IOException {
        shouldWork = true;
        listener = new ServerSocket(port);
    }

    /**
     * <p>
     * Cancels work, stops all threads and exits.
     * </p>
     */
    public void cancel() {
        synchronized (lock) {
            shouldWork = false;
            lock.notify();
            if (messageReceiver != null) messageReceiver.cancel();
            if (messageSender != null) messageSender.cancel();
            try {
                listener.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * <p>
     * Sends message to phone.
     * </p>
     *
     * @param message - message to send
     */
    public void sendMessage(Message message) {
        messageSender.sendMessage(message);
    }

    @Override
    public void run() {
        try {
            Socket connection = listener.accept();
            messageSender = new MessageSender(connection);
            messageReceiver = new MessageReceiver(connection);
            messageSender.start();
            messageReceiver.start();
            MainEngine.getInstance().changeConnectionState(true);
        } catch (IOException | ReferenceNotInitializedException e) {
            System.err.println("MessageService run: " + e.getMessage());
        }

        // Infinite loop
        synchronized (lock) {
            while (shouldWork) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        System.out.println("MessageService stopping work");
    }
}
