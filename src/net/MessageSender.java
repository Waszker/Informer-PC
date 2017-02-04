package net;

import informer_api.conversation.Message;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <p>
 * Class used in sending messages via phone.
 * </p>
 * Created by waszka on 30.01.17.
 */
class MessageSender extends Thread {
    private ObjectOutputStream outStream;
    private boolean shouldWork;
    private final ConcurrentLinkedQueue<Message> messagesToSend;

    MessageSender(Socket connection) throws IOException {
        outStream = new ObjectOutputStream(connection.getOutputStream());
        messagesToSend = new ConcurrentLinkedQueue<>();
        shouldWork = true;
    }

    @Override
    public void run() {
        synchronized (messagesToSend) {
            while (shouldWork) {
                waitForMessagesToSend();
            }
        }
        System.out.println("MessageSender stopping work");
    }

    void cancel() {
        synchronized (messagesToSend) {
            shouldWork = false;
            messagesToSend.notify();
        }
    }

    void sendMessage(Message message) {
        synchronized (messagesToSend) {
            messagesToSend.add(message);
            messagesToSend.notify();
        }
    }

    private void waitForMessagesToSend() {
        try {
            if (messagesToSend.isEmpty()) messagesToSend.wait();
            for (Message m : messagesToSend) outStream.writeObject(m);
        } catch (InterruptedException | IOException e) {
        } finally {
            messagesToSend.clear();
        }
    }
}
