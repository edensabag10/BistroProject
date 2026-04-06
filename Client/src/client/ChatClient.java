package client;

import java.io.IOException;
import common.ChatIF;
import ocsf.client.AbstractClient;

/**
 * The ChatClient class serves as the core communication bridge on the client side,
 * implementing the logic defined by the OCSF framework for the Bistro system.
 * * It manages socket connections, dispatches server responses to the UI, 
 * and handles outgoing message transmission.
 */

public class ChatClient extends AbstractClient {

    /**
     * A reference to the UI layer (GUI or Console).
     * Uses the ChatIF interface to remain decoupled from specific JavaFX controllers.
     */
    private ChatIF clientUI;

    /**
     * Constructs a new ChatClient and attempts to establish a connection to the host.
     * * @param host     The server's IP address or hostname.
     * @param port     The dedicated TCP port number.
     * @param clientUI The UI instance assigned to handle data visualization.
     * @throws IOException If the host is unreachable or the connection fails.
     */
    public ChatClient(String host, int port, ChatIF clientUI) throws IOException {
        super(host, port);  
        this.clientUI = clientUI;
        openConnection();   
    }
    
    /**
     * Updates the UI reference dynamically to the currently active controller.
     * * @param clientUI The new UI instance implementing ChatIF.
     */
    public void setUI(ChatIF clientUI) {
        this.clientUI = clientUI;
    }

    /**
     * Hook method triggered by the OCSF framework when a message arrives from the server.
     * * @param msg The incoming object sent by the Server.
     */
    @Override
    public void handleMessageFromServer(Object msg) {
        clientUI.display(msg);  
    }

    /**
     * Acts as the outbound gateway for sending data from the UI to the Server.
     * * @param message The data object to be transmitted (typically an ArrayList).
     */
    public void handleMessageFromClientUI(Object message) {
        try {
            sendToServer(message);  
        } catch (IOException e) {
            clientUI.display("Connection Error: Unable to reach the server. Terminating session.");
            quit();
        }
    }

    /**
     * Gracefully terminates the client connection and shuts down the application.
     */
    public void quit() {
        try {
            closeConnection();
        } catch (IOException e) {
            // Errors during shutdown are logged but ignored
        }
        System.exit(0);
    }
}