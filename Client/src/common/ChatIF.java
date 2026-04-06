package common;

/**
 * The ChatIF interface defines the communication contract for the Client-side 
 * User Interface (UI) in the Bistro system.
 * Any class acting as a UI layer must implement this interface to receive 
 * data asynchronously from the server.
 */
public interface ChatIF {
    
    /**
     * This method is invoked to display or process data received from the server.
     * * @param message The message object sent from the server. This can be a String, 
     * ArrayList, or custom DTOs like ServiceResponse.
     */
    void display(Object message);
}