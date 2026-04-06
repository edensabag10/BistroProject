package common;

/**
 * yanivvvvvvvvv
 * The ServerIF interface defines a communication contract between the server's 
 * backend logic and its User Interface (UI).
 * * By implementing this interface, any UI component (such as a JavaFX Controller) 
 * can receive and display real-time status updates from the OCSF server 
 * without being tightly coupled to the network logic.
 * * This approach follows the Dependency Inversion Principle, allowing the 
 * server to function with different UI implementations (Console, GUI, etc.).
 * * @author Software Engineering Student
 * @version 1.0
 */
public interface ServerIF {

    /**
     * Appends a status or event message to the server's log window or console.
     * * Implementation Note:
     * Since network events on the OCSF server (like client connections or 
     * database queries) occur on background threads, any UI updates performed 
     * within the implementation of this method MUST be wrapped in 
     * Platform.runLater() to ensure thread safety with the JavaFX Application Thread.
     *
     * @param message The descriptive text or event data to be displayed in the log.
     */
    void appendLog(String message);
}
// commit