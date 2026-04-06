package clientGUI.Controllers.OccasionalControlls; // Define the package for occasional user controllers

import java.util.ArrayList; // Import for dynamic array list management
import client.ChatClient; // Import the main client communication class
import clientGUI.Controllers.ICustomerActions; // Import the interface for customer-specific actions
import clientGUI.Controllers.RemoteLoginController; // Import reference to the login controller
import clientGUI.Controllers.MenuControlls.BaseMenuController; // Import the base controller for inheritance
import clientGUI.Controllers.MenuControlls.ExitWaitingListHelper; // Import helper for waiting list logic
import common.ChatIF; // Import the communication interface
import javafx.application.Platform; // Import for UI thread safety
import javafx.event.ActionEvent; // Import for handling UI action events
import javafx.fxml.FXML; // Import for FXML field injection
import javafx.fxml.FXMLLoader; // Import for loading FXML layout files
import javafx.scene.Node; // Import for generic UI node elements
import javafx.scene.Parent; // Import for root UI elements
import javafx.scene.Scene; // Import for stage scene management
import javafx.scene.control.Alert; // Import for showing alert dialogs
import javafx.scene.control.Alert.AlertType; // Import for alert types
import javafx.scene.control.Button; // Import for button components
import javafx.scene.control.ButtonType; // Import for alert button types
import javafx.scene.control.TextArea; // Import for multi-line text display
import javafx.stage.Stage; // Import for window management

/**
 * The OccasionalMenuController serves as the main dashboard for Guest (Occasional) users in the Bistro system.
 * It manages navigation to reservations, payments, and provides an interface for leaving the waiting list.
 */
public class OccasionalMenuController extends BaseMenuController implements ICustomerActions { 

    // FXML injected buttons for guest dashboard navigation
    @FXML private Button btnNewReservation; 
    @FXML private Button btnPayBill; 
    @FXML private Button btnViewReservation; 
    @FXML private Button btnLogout; 
    @FXML private Button btnExitWaitingList; 
    
    // Console-style log area for providing real-time feedback
    @FXML private TextArea txtLog; 

    // --- 1. Initialization & UI Setup ---

    /**
     * Executes automatically when the controller is ready and session data is injected.
     * Logs the activation of the guest portal for the specific user ID.
     * @return None.
     */
    @Override 
    public void onClientReady() { 
        appendLog("Guest Portal Active. Welcome, User ID: " + userId); 
    } 

    // --- 2. Reservation & Billing Action Handlers ---

    /**
     * Navigates the guest user to the New Reservation screen.
     * @param event The ActionEvent triggered by clicking the new reservation button.
     * @return None.
     */
    @FXML 
    void clickNewReservation(ActionEvent event) { 
        createNewReservation(client, event, userType, userId); 
    } 
    
    /**
     * Navigates the guest user to the Pay Bill (verification) screen.
     * @param event The ActionEvent triggered by clicking the pay bill button.
     * @return None.
     */
    @FXML 
    void clickPayBill(ActionEvent event) { 
        payBill(client, event, userType, userId); 
    } 
    
    /**
     * Navigates the guest user to the View Reservation screen to see active bookings.
     * @param event The ActionEvent triggered by clicking the view reservation button.
     * @return None.
     */
    @FXML 
    void clickViewReservation(ActionEvent event) { 
        viewReservation(client, event, userType, userId); 
    } 

    /**
     * Initiates the process to remove the guest from the restaurant's waiting list.
     * @param event The ActionEvent triggered by clicking the exit waiting list button.
     * @return None.
     */
    @FXML 
    void clickExitWaitingList(ActionEvent event) { 
        appendLog("Exit Waiting List triggered."); 
        ExitWaitingListHelper.requestLeaveWaitingList(this.client, this.userId); 
    } 
  
    // --- 3. Session Termination (Logout) ---

    /**
     * Logs the guest out and returns them to the primary Remote Login portal.
     * @param event The ActionEvent triggered by clicking the logout button.
     * @return None.
     */
    @FXML 
    void clickLogout(ActionEvent event) { 
        try { 
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/RemoteLoginFrame.fxml")); 
            Parent root = loader.load(); 
            
            Object nextController = loader.getController(); 
            
            if (nextController instanceof BaseMenuController) { 
                ((BaseMenuController) nextController).setClient(client, null, 0); 
            } 
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); 
            stage.setScene(new Scene(root)); 
            stage.show(); 
            
        } catch (Exception e) { 
            e.printStackTrace(); 
            appendLog("Logout Error: Unable to return to portal."); 
        } 
    } 

    // --- 4. ICustomerActions Stubs (Role-based exclusions) ---

    /**
     * Implementation stub for viewing order history.
     * Note: Occasional guests do not have stored history in this system.
     * @param client The active network client.
     * @param userId The unique user ID.
     * @return None.
     */
    @Override 
    public void viewOrderHistory(ChatClient client, int userId) { 
    } 
    
    /**
     * Implementation stub for editing personal profile details.
     * Note: Profile management is disabled for guest accounts.
     * @param client The active network client.
     * @param userId The unique user ID.
     * @return None.
     */
    @Override 
    public void editPersonalDetails(ChatClient client, int userId) { 
    } 

    // --- 5. Server Communication (ChatIF) ---

    /**
     * Processes incoming server responses and delegates waiting list logic to the helper class.
     * @param message The message object received from the server.
     * @return None.
     */
    @Override 
    public void display(Object message) { 
        
        if (message != null) { 
            appendLog(message.toString()); 
        } 
        
        if (message instanceof String) { 
            String response = (String) message; 
            
            boolean isWaitingListResponse = (response.startsWith("CANCEL_WAITING") || response.equals("NOT_ON_WAITING_LIST")); 
            
            if (isWaitingListResponse) { 
                ExitWaitingListHelper.handleServerResponse(response); 
            } 
        } 
    } 

    /**
     * Appends text messages to the UI log area in a thread-safe manner using Platform.runLater.
     * @param message The string message to append to the log.
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