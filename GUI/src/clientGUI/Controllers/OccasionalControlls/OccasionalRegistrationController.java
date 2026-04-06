package clientGUI.Controllers.OccasionalControlls; // Define the package for occasional user controllers

import java.util.ArrayList; // Import ArrayList for message packaging
import client.ChatClient; // Import the main client communication class
import clientGUI.Controllers.MenuControlls.BaseMenuController; // Import the base controller for inheritance
import javafx.application.Platform; // Import for UI thread safety
import javafx.event.ActionEvent; // Import for handling button actions
import javafx.fxml.FXML; // Import for FXML injection
import javafx.fxml.FXMLLoader; // Import for loading new FXML scenes
import javafx.scene.Node; // Import for generic UI node elements
import javafx.scene.Parent; // Import for the root of the scene graph
import javafx.scene.Scene; // Import for managing the stage scene
import javafx.scene.control.TextArea; // Import for multi-line text display
import javafx.scene.control.TextField; // Import for text input fields
import javafx.stage.Stage; // Import for window management

/**
 * Controller class for the Occasional Customer Registration interface in the Bistro system.
 * This class handles guest user registration, including multi-format input validation 
 * for phone numbers and email addresses.
 */
public class OccasionalRegistrationController extends BaseMenuController { 

    // FXML injected UI components
    @FXML private TextField txtNewUser; 
    @FXML private TextField txtNewContact; 
    @FXML private TextArea txtLog; 

    /**
     * Executes automatically once the client and session data are ready.
     * Logs the initialization status and the identity of the current operator.
     * @return None.
     */
    @Override 
    public void onClientReady() { 
        appendLog("Ready for new guest registration."); 
        appendLog("Operator Identity: " + userType + " (ID: " + userId + ")"); 
    } 

    /**
     * Processes the registration form submission by validating inputs and sending data to the server.
     * Validation includes username length and contact format (10-digit phone or valid email).
     * @param event The ActionEvent triggered by the submit button.
     * @return None.
     */
    @FXML 
    void clickSubmitRegistration(ActionEvent event) { 
        String user = txtNewUser.getText().trim(); 
        String contact = txtNewContact.getText().trim(); 

        if (user.isEmpty() || contact.isEmpty()) { 
            appendLog("Error: All fields are required."); 
            return; 
        } 

        if (user.length() > 10) { 
            appendLog("Error: Username must be 10 characters or less."); 
            return; 
        } 

        char firstChar = contact.charAt(0); 
        
        if (Character.isDigit(firstChar)) { 
            if (contact.length() != 10 || !contact.matches("\\d+")) { 
                appendLog("Error: You started with a number. Phone must be exactly 10 digits."); 
                return; 
            } 
        } 
        else { 
            if (!contact.contains("@")) { 
                appendLog("Error: You started with a letter. Email must contain '@'."); 
                return; 
            } 
        } 

        if (client != null) { 
            appendLog("Sending registration request for: " + user); 
            ArrayList<String> message = new ArrayList<>(); 
            message.add("REGISTER_OCCASIONAL"); 
            message.add(user); 
            message.add(contact); 
            client.handleMessageFromClientUI(message); 
        } else { 
            appendLog("Fatal Error: No server connection!"); 
        } 
    } 

    /**
     * Processes messages received from the server and updates the UI feedback log.
     * @param message The response object from the server.
     * @return None.
     */
    @Override 
    public void display(Object message) { 
        Platform.runLater(() -> { 
            if (message != null) { 
                String response = message.toString(); 
                appendLog("Server Response: " + response); 

                switch (response) { 
                    case "REGISTRATION_SUCCESS": 
                        appendLog("SUCCESS: Account created! You can now go back and login."); 
                        txtNewUser.clear(); 
                        txtNewContact.clear(); 
                        break; 

                    default: 
                        break; 
                } 
            } 
        }); 
    } 

    /**
     * Navigates the user back to the Occasional login screen while preserving session data.
     * @param event The ActionEvent triggered by the back button.
     * @return None.
     */
    @FXML 
    void clickBack(ActionEvent event) { 
        try { 
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/OccasionalFXML/OccasionalLoginFrame.fxml")); 
            Parent root = loader.load(); 
            
            Object nextController = loader.getController(); 
            if (nextController instanceof BaseMenuController) { 
                ((BaseMenuController) nextController).setClient(client, userType, userId); 
            } 

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
            Scene scene = new Scene(root); 
            scene.getStylesheets().add(getClass().getResource("/clientGUI/cssStyle/GlobalStyles.css").toExternalForm()); 
            
            stage.setScene(scene); 
            stage.show(); 
        } catch (Exception e) { 
            e.printStackTrace(); 
            appendLog("Navigation Error: " + e.getMessage()); 
        } 
    } 

    /**
     * Updates the GUI log area in a thread-safe manner using Platform.runLater.
     * @param message The text message to append to the log.
     * @return None.
     */
    public void appendLog(String message) { 
        Platform.runLater(() -> { 
            if (txtLog != null) { 
                txtLog.appendText("> " + message + "\n"); 
            } 
        }); 
    } 
}