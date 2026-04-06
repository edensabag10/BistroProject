package clientGUI.Controllers.MenuControlls; // Defining the package for menu control helpers

import javafx.application.Platform; // Import Platform to run UI updates on the JavaFX thread
import javafx.scene.control.Alert; // Import Alert class for displaying dialog boxes
import javafx.scene.control.Alert.AlertType; // Import AlertType for defining the style of the alert
import javafx.scene.control.ButtonType; // Import ButtonType for handling user button clicks
import client.ChatClient; // Import ChatClient to communicate with the server
import java.util.ArrayList; // Import ArrayList to structure messages sent to the server

/**
 * Utility helper class for managing waiting list removal operations in the Bistro system.
 * It handles the user confirmation dialog, server request transmission, and processing of feedback.
 */
public class ExitWaitingListHelper { // Start of ExitWaitingListHelper class definition

    /**
     * Initiates the process of leaving the waiting list by showing a confirmation dialog.
     * If the user confirms, a cancellation request is sent to the server.
     * @param client The active network client used for server communication.
     * @param userId The unique identifier of the user requesting to leave the list.
     * @return None.
     */
    public static void requestLeaveWaitingList(ChatClient client, int userId) { // Method to initiate leaving process
        
        // Creating a new confirmation alert with specific message and Yes/No buttons
        Alert confirm = new Alert(AlertType.CONFIRMATION, 
            "Are you sure you want to leave the waiting list?", 
            ButtonType.YES, ButtonType.NO); // End of Alert initialization
            
        // Setting the title of the confirmation dialog window
        confirm.setTitle("Waiting List Confirmation"); // End of title setting
        
        // Displaying the dialog and waiting for user interaction
        confirm.showAndWait().ifPresent(response -> { // Start of response handling lambda
            
            // Checking if the user clicked the 'YES' button
            if (response == ButtonType.YES) { // Start of conditional block for 'YES'
                
                // Preparing a list to hold the command and data for the server
                ArrayList<Object> message = new ArrayList<>(); // Initializing the message list
                
                // Adding the specific command string understood by the server
                message.add("CANCEL_WAITING_LIST"); // Adding command to list
                
                // Adding the user's unique ID to the message payload
                message.add(userId); // Adding user ID to list
                
                // Transmitting the constructed message to the server via the client
                client.handleMessageFromClientUI(message); // Calling the client handler
                
            } // End of 'YES' conditional block
            
        }); // End of lambda and ifPresent block
        
    } // End of requestLeaveWaitingList method

    /**
     * Processes server responses related to waiting list cancellation and triggers UI feedback.
     * This method ensures that alerts are displayed on the JavaFX Application Thread.
     * @param response The response code string received from the server.
     * @return None.
     */
    public static void handleServerResponse(String response) { // Method to process server feedback
        
        // Ensuring the UI updates happen on the main JavaFX Application Thread
        Platform.runLater(() -> { // Start of runLater lambda block
            
            // Using a switch statement to handle different response scenarios clearly
            switch (response) { // Start of switch block on the response string
                
                // Case where the server successfully removed the user from the list
                case "CANCEL_WAITING_SUCCESS": // Match success string
                    // Display an informative popup for successful removal
                    showPopup("Success", "Removed from waiting list successfully!", AlertType.INFORMATION); // Show popup
                    break; // Exit switch
                
                // Case where the server didn't find the user in the waiting list
                case "NOT_ON_WAITING_LIST": // Match "not found" string
                    // Display a warning that no active entry was found
                    showPopup("Notice", "No active waiting entry found.", AlertType.WARNING); // Show popup
                    break; // Exit switch
                
                // Case where something went wrong on the server side
                case "SERVER_ERROR": // Match error string
                    // Display an error popup indicating a technical failure
                    showPopup("Error", "Server error occurred. Please try again later.", AlertType.ERROR); // Show popup
                    break; // Exit switch
                    
                // Default case if an unknown response is received (Optional safety)
                default: // Start of default case
                    break; // Exit switch
                    
            } // End of switch block
            
        }); // End of runLater lambda
        
    } // End of handleServerResponse method

    /**
     * Internal utility to create and display standardized JavaFX alert popups.
     * @param title   The text to be displayed in the window title bar.
     * @param content The main message body to be shown to the user.
     * @param type    The AlertType defining the visual style (Info, Warning, Error).
     * @return None.
     */
    private static void showPopup(String title, String content, AlertType type) { // Helper method for alerts
        
        // Instantiating a new Alert with the specified alert type (Success, Warning, Error)
        Alert alert = new Alert(type); // End of Alert creation
        
        // Setting the window title for the popup
        alert.setTitle(title); // End of title setting
        
        // Removing the header text for a cleaner and more compact look
        alert.setHeaderText(null); // End of header setting
        
        // Setting the main message content for the user to read
        alert.setContentText(content); // End of content setting
        
        // Displaying the alert and blocking until the user closes it
        alert.showAndWait(); // End of showAndWait call
        
    } // End of showPopup method
    
} // End of ExitWaitingListHelper class definition