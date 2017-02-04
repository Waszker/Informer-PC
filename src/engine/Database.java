package engine;

import com.sun.org.apache.xml.internal.security.signature.ReferenceNotInitializedException;
import informer_api.conversation.Conversation;
import informer_api.conversation.Message;
import informer_api.conversation.Person;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Class for storing all conversation information.
 * </p>
 * Created by Piotr Waszkiewicz on 30.01.17.
 */
class Database implements Serializable {
    private HashMap<Person, Conversation> conversations;

    Database(Map<Person, Conversation> conversations) {
        this.conversations = new HashMap<>(conversations);
    }

    HashMap<Person, Conversation> getConversations() {
        return conversations;
    }

    Conversation getConversationWithPerson(Person person) {
        Conversation conversation = null;
        if (conversations.containsKey(person)) conversation = conversations.get(person);
        return conversation;
    }

    void addNewMessages(Conversation newMessages) {
        Person conversationWith = newMessages.getPerson();
        if (conversations.containsKey(conversationWith)) {
            for (Message m : newMessages.getMessages())
                conversations.get(conversationWith).addMessage(m);
        } else {
            conversations.put(conversationWith, newMessages);
            try {
                MainEngine.getInstance().synchronizeDatabase(conversations);
            } catch (ReferenceNotInitializedException e) {
            }
        }
    }
}
