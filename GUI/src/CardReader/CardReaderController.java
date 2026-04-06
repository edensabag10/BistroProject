package CardReader;

import java.util.ArrayList;
import client.ChatClient;

/**
 * Client-side controller for the Card Reader module in the Bistro system.
 * Acts as a communication bridge to forward hardware-related requests to the server.
 */
public class CardReaderController {
    
    /** Static reference to the communication client. */
    private static ChatClient client;

    /**
     * Sets the static ChatClient instance for server communication.
     * * @param chatClient The active ChatClient instance to be used.
     */
    public static void setClient(ChatClient chatClient) {
        client = chatClient;
    }
    
    /**
     * Retrieves the current ChatClient instance.
     * * @return The static ChatClient used for communication.
     */
    public static ChatClient getClient() {
        return client;
    }

    /**
     * Sends a subscriber login/validation request to the server.
     * * @param id The identification string of the subscriber.
     */
    public void validateSubscriber(String id) {
        ArrayList<Object> message = new ArrayList<>();
        // שינוי קריטי: משתמשים ב-LOGIN_SUBSCRIBER כדי שיהיה זהה לטרמינל
        message.add("LOGIN_SUBSCRIBER"); 
        message.add(id);
        client.handleMessageFromClientUI(message);
    }
    
    /**
     * Requests a list of lost confirmation codes for a specific user.
     * * @param id The identification string of the user.
     */
    public void getLostConfirmationCodes(String id) {
        ArrayList<Object> message = new ArrayList<>();
        message.add("CARD_READER_GET_CODES");
        message.add(id);
        client.handleMessageFromClientUI(message);
    }

    /**
     * Sends a request to verify a specific arrival confirmation code.
     * * @param code The confirmation code to be verified.
     * @param id   The identification string of the user.
     */
    public void verifyConfirmationCode(String code, String id) {
        ArrayList<Object> message = new ArrayList<>();
        message.add("CARD_READER_VERIFY_CODE");
        message.add(code);
        message.add(id);
        client.handleMessageFromClientUI(message);
    }
}