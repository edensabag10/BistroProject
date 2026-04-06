package terminalGUI.Controllers.TerminalControllers; // Defining the package for terminal-side controllers

import client.ChatClient; // Importing the main client class for communication
import clientGUI.Controllers.OccasionalControlls.OccasionalLoginController; // Importing the guest controller for casting
import clientGUI.Controllers.SubscriberControlls.SubscriberLoginController; // Importing the member controller for casting
import common.ChatIF; // Importing the interface for server-side responses
import common.LoginSource;
import javafx.event.ActionEvent; // Importing ActionEvent for UI interaction handling
import javafx.fxml.FXML; // Importing FXML annotation for UI component injection
import javafx.fxml.FXMLLoader; // Importing FXMLLoader for loading login frames
import javafx.scene.Parent; // Importing Parent as the root node for scenes
import javafx.scene.Scene; // Importing Scene to manage the window content
import javafx.stage.Stage; // Importing Stage for window management
import javafx.scene.Node; // Importing Node to identify the current window

/**
 * Integrated TerminalLoginController.
 * This class handles the transition from a physical terminal to specific login portals
 * while maintaining the "Pipe" architecture and specific terminal logic.
 */
public class TerminalLoginController implements ChatIF { 

    /** Shared network client instance used for server communication. */
    private ChatClient client; 

    /**
     * Injects the persistent ChatClient instance into this controller.
     * @param client The active network client.
     * @return None.
     */
    public void setClient(ChatClient client) { 
        this.client = client; 
    } 

    /**
     * Event handler for the 'Subscriber Login' button on the terminal.
     * Initiates navigation to the Subscriber login portal.
     * @param event The ActionEvent triggered by the button click.
     * @return None.
     */
    @FXML 
    void clickSubscriber(ActionEvent event) { 
        loadScreen(event, "/clientGUI/fxmlFiles/SubscriberFXML/SubscriberLoginFrame.fxml", true, "Subscriber Login"); 
    } 

    /**
     * Event handler for the 'Occasional Login' button on the terminal.
     * Initiates navigation to the Occasional (Guest) login portal.
     * @param event The ActionEvent triggered by the button click.
     * @return None.
     */
    @FXML 
    void clickOccasional(ActionEvent event) { 
        loadScreen(event, "/clientGUI/fxmlFiles/OccasionalFXML/OccasionalLoginFrame.fxml", false, "Occasional Login"); 
    } 

    /**
     * Core Navigation Engine: Loads the target FXML and injects session data and terminal flag.
     * This method ensures the "Pipe" architecture is maintained by passing the client 
     * and setting the LoginSource to TERMINAL.
     * 
     * @param event        The ActionEvent used to identify the current window.
     * @param fxmlPath     The path to the login FXML.
     * @param isSubscriber Flag to distinguish between subscriber and guest controllers for casting.
     * @param title        The title for the new window stage.
     * @return None.
     */
    private void loadScreen(ActionEvent event, String fxmlPath, boolean isSubscriber, String title) { 
        
        try { 
            
            // Step 1: Initialize the FXML loader for the requested login portal
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath)); 
            
            // Step 2: Load the UI graph root from the FXML resource
            Parent root = loader.load(); 
            
            // Step 3: Extract the controller instance from the loader
            Object controller = loader.getController(); 

            // Branching logic to handle specific controller casting and data injection
            if (isSubscriber) { 
                SubscriberLoginController subController = (SubscriberLoginController) controller; 
                
                // Marking the origin as a physical Terminal for backend logic
                subController.setLoginSource(LoginSource.TERMINAL); 
                
                // Injecting the client and session parameters into the Pipe
                subController.setClient(client, "Subscriber", -1); 
                                            
            } else { 
                OccasionalLoginController occController = (OccasionalLoginController) controller; 
                
                // Marking the origin as a physical Terminal for backend logic
                occController.setLoginSource(LoginSource.TERMINAL); 
                
                // Injecting the client and session parameters into the Pipe
                occController.setClient(client, "Occasional", -1); 
                
            } 

            // Step 4: Configure the window (Stage) and assign the new scene
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
            Scene scene = new Scene(root); 
            
            // Step 5: Apply global CSS styling for terminal UI consistency
            if (getClass().getResource("/clientGUI/cssStyle/GlobalStyles.css") != null) { 
                scene.getStylesheets().add(getClass().getResource("/clientGUI/cssStyle/GlobalStyles.css").toExternalForm()); 
            } 

            // Step 6: Update stage properties and display
            stage.setTitle(title); 
            stage.setScene(scene); 
            stage.show(); 

        } catch (Exception e) { 
            e.printStackTrace(); 
        } 
        
    } 

    /**
     * Implementation of the ChatIF display method for server communication.
     * This allows the terminal login screen to process incoming server broadcasts if necessary.
     * @param message The message object received from the server.
     * @return None.
     */
    @Override 
    public void display(Object message) { 
        // Broadcast implementation placeholder
    } 
    
}