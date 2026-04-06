package clientGUI.Controllers.SubscriberControlls; // Defining the package for subscriber controllers

import javafx.scene.control.Alert; // Importing Alert class for UI popups
import javafx.scene.control.Button; // Importing Button component
import javafx.scene.control.TextField; // Importing TextField component
import java.util.ArrayList; // Importing ArrayList for dynamic message lists
import client.ChatClient; // Importing the ChatClient for communication
import common.ChatIF; // Importing the interface for server responses
import javafx.fxml.FXML; // Importing FXML annotation for UI injection
import javafx.application.Platform; // Importing Platform for UI thread management
import javafx.event.ActionEvent; // Importing ActionEvent for button triggers
import javafx.scene.Node; // Importing Node for UI hierarchy access
import javafx.stage.Stage; // Importing Stage for window management

/**
 * Controller for editing subscriber profile details in the Bistro system.
 * Manages input validation and transmits update requests to the server.
 */
public class EditSubscriberDetailsController implements ChatIF { 

    /** Reference to the active network client. */
    private ChatClient client;
    
    /** Unique identifier for the logged-in user. */
    private int userId;
    
    // --- FXML UI Fields ---
    
    @FXML private TextField txtUsername;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    // --- Configuration Methods ---

    /**
     * Injects the network client and registers this controller as the UI listener.
     * @param client The active ChatClient instance.
     * @return None.
     */
    public void setClient(ChatClient client) { 
        this.client = client; 
        client.setUI(this); 
    } 

    /**
     * Sets the user ID for identifying which subscriber to update.
     * @param userId The unique identifier of the user.
     * @return None.
     */
    public void setUserId(int userId) { 
        this.userId = userId; 
    } 
    
    // --- UI Action Handlers ---

    /**
     * Closes the current edit window without saving changes.
     * @param event The ActionEvent triggered by the cancel button.
     * @return None.
     */
    @FXML 
    private void clickCancel(ActionEvent event) { 
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
        stage.close(); 
    } 
    
    /**
     * Validates input fields and sends an update request to the server.
     * @param event The ActionEvent triggered by the save button.
     * @return None.
     */
    @FXML 
    private void clickSave(ActionEvent event) { 

        if (client == null) { 
            System.out.println("Error: client is null"); 
            return; 
        } 

        String username = txtUsername.getText().trim(); 
        String phone = txtPhone.getText().trim(); 
        String email = txtEmail.getText().trim(); 
        
        // --- Input Validation ---
        if (!username.isEmpty() && username.length() > 10) {
            showAlert("Invalid Username", "Username must contain up to 10 characters.", Alert.AlertType.WARNING);
            return;
        }

        if (!phone.isEmpty() && !phone.matches("\\d{10}")) {
            showAlert("Invalid Phone Number", "Phone number must contain exactly 10 digits.", Alert.AlertType.WARNING);
            return;
        }

        if (!email.isEmpty() && !email.contains("@")) {
            showAlert("Invalid Email", "Email address must contain '@'.", Alert.AlertType.WARNING);
            return;
        }

        boolean isAllEmpty = (username.isEmpty() && phone.isEmpty() && email.isEmpty()); 
        
        if (isAllEmpty) { 
            showAlert("No Changes", "Please enter at least one field to update.", Alert.AlertType.INFORMATION); 
            return; 
        } 

        ArrayList<Object> msg = new ArrayList<>(); 
        msg.add("UPDATE_SUBSCRIBER_DETAILS"); 
        msg.add(userId); 
        msg.add(username.isEmpty() ? null : username); 
        msg.add(phone.isEmpty() ? null : phone); 
        msg.add(email.isEmpty() ? null : email); 

        client.handleMessageFromClientUI(msg); 
    } 

    /**
     * Processes incoming feedback from the server on the JavaFX thread.
     * @param message The response object from the server.
     * @return None.
     */
    @Override 
    public void display(Object message) { 

        Platform.runLater(() -> { 

            if (message instanceof ArrayList<?>) { 
                ArrayList<?> data = (ArrayList<?>) message; 
                String command = data.get(0).toString(); 

                switch (command) { 
                    case "EDIT_DETAILS_RESULT": 
                        String result = data.get(1).toString(); 
                        switch (result) { 
                            case "SUCCESS": 
                                showAlert("Profile Updated", "Your personal details were updated successfully.", Alert.AlertType.INFORMATION); 
                                closeWindow(); 
                                break; 
                            case "NO_CHANGES": 
                                showAlert("No Changes", "No details were updated.", Alert.AlertType.INFORMATION); 
                                break; 
                            default: 
                                break; 
                        } 
                        break; 
                    default: 
                        break; 
                } 
            } 
            else if (message instanceof String && "ERROR_EDITING_DETAILS".equals(message)) { 
                showAlert("Error", "An error occurred while updating your details.", Alert.AlertType.ERROR); 
            } 
        }); 
    } 
    
    /**
     * Reusable utility for displaying JavaFX Alerts to the user.
     * @param title   The title of the alert window.
     * @param content The message body of the alert.
     * @param type    The AlertType (Error, Warning, Information).
     * @return None.
     */
    private void showAlert(String title, String content, Alert.AlertType type) { 
        Alert alert = new Alert(type); 
        alert.setTitle(title); 
        alert.setHeaderText(null); 
        alert.setContentText(content); 
        alert.showAndWait(); 
    } 
    
    /**
     * Closes the active window stage.
     * @return None.
     */
    private void closeWindow() { 
        Stage stage = (Stage) btnSave.getScene().getWindow(); 
        stage.close(); 
    } 
}