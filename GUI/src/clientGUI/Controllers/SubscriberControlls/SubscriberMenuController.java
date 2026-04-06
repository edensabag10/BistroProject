package clientGUI.Controllers.SubscriberControlls; // Define the package for subscriber dashboard controllers

import java.io.IOException; // Import for handling input/output exceptions during FXML loading
import client.ChatClient; // Import the main communication client
import clientGUI.Controllers.ICustomerActions; // Import the interface for customer-related actions
import clientGUI.Controllers.RemoteLoginController; // Import the portal login controller reference
import clientGUI.Controllers.MenuControlls.BaseMenuController; // Import the parent controller for shared logic
import clientGUI.Controllers.MenuControlls.ExitWaitingListHelper; // Import helper for waiting list removal
import javafx.application.Platform; // Import for running tasks on the JavaFX thread
import javafx.event.ActionEvent; // Import for handling button click events
import javafx.fxml.FXML; // Import for FXML field injection
import javafx.fxml.FXMLLoader; // Import for loading FXML layout files
import javafx.scene.Node; // Import for generic UI node elements
import javafx.scene.Parent; // Import for the root of the UI graph
import javafx.scene.Scene; // Import for stage scene management
import javafx.scene.control.Button; // Import for button UI components
import javafx.scene.control.TextArea; // Import for multi-line text display
import javafx.stage.Stage; // Import for managing window stages

/**
 * The SubscriberMenuController acts as the primary dashboard for authenticated subscribers in the Bistro system.
 * It manages navigation to all subscriber-specific features while maintaining session state.
 */
public class SubscriberMenuController extends BaseMenuController implements ICustomerActions { 

    // FXML injected buttons for the subscriber dashboard
    @FXML private Button btnNewRes; 
    @FXML private Button btnPayBill; 
    @FXML private Button btnViewRes; 
    @FXML private Button btnExitWait; 
    @FXML private Button btnReservationHistory;
    @FXML private Button btnVisitHistory; 
    @FXML private Button btnEditProfile; 
    @FXML private Button btnLogout; 
    @FXML private Button btnBack;   
    
    // FXML injected text area for system feedback logs
    @FXML private TextArea txtLog; 

    // --- 1. Initialization & UI Display ---

    /**
     * Executes automatically when the controller is ready and session data is injected.
     * Displays a welcome message and configures UI visibility based on user role.
     * @return None.
     */
    @Override 
    public void onClientReady() { 
        appendLog("Welcome back! Subscriber ID: " + userId); 
        
        if (actingAsSubscriber && btnBack != null) 
        {
            btnBack.setVisible(true);
        }
    } 

    // --- 2. Reservation & Billing Actions (ICustomerActions) ---

    /**
     * Navigates to the New Reservation screen.
     * @param event The ActionEvent triggered by the button click.
     * @return None.
     */
    @FXML 
    void clickNewRes(ActionEvent event) { 
        createNewReservation(client, event, userType, userId); 
    } 
    
    /**
     * Navigates to the Bill Payment screen.
     * @param event The ActionEvent triggered by the button click.
     * @return None.
     */
    @FXML 
    void clickPayBill(ActionEvent event) { 
        payBill(client, event, userType, userId); 
    } 
    
    /**
     * Navigates to the View Reservations screen.
     * @param event The ActionEvent triggered by the button click.
     * @return None.
     */
    @FXML 
    void clickViewRes(ActionEvent event) { 
        viewReservation(client, event, userType, userId); 
    } 

    // --- 3. Functional Modules ---

    /**
     * Triggers the waiting list removal process via the helper class.
     * @param event The ActionEvent triggered by the button click.
     * @return None.
     */
    @FXML 
    void clickExitWait(ActionEvent event) { 
        appendLog("Exit Waiting List triggered."); 
        ExitWaitingListHelper.requestLeaveWaitingList(this.client, this.userId); 
    } 
    
    /**
     * Opens the Reservation History window.
     * @param event The ActionEvent triggered by the button click.
     * @return None.
     */
    @FXML 
    void clickReservationHistory(ActionEvent event) { 
        viewOrderHistory(client, userId); 
    } 
    /**
     * Opens the Visit History window.
     * @param event The ActionEvent triggered by the button click.
     * @return None.
     */
    @FXML
    void clickVisitHistory(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/SubscriberFXML/VisitHistoryFrame.fxml"));
            Parent root = loader.load();

            VisitHistoryController controller = loader.getController();
            controller.setClient(client);
            controller.loadVisitsForUser(userId);

            showNewWindow(root, "Visit History");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Opens the Profile Editing window.
     * @param event The ActionEvent triggered by the button click.
     * @return None.
     */
    @FXML 
    void clickEditProfile(ActionEvent event) { 
        editPersonalDetails(client, userId); 
    } 

    // --- 4. Navigation Implementations with Dependency Injection (The Pipe) ---

    /**
     * Loads the Reservation History frame and injects required session data.
     * @param client The active network client.
     * @param userId The unique identifier of the subscriber.
     * @return None.
     */
    @Override 
    public void viewOrderHistory(ChatClient client, int userId) { 
        try { 
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/SubscriberFXML/ReservationsHistoryFrame.fxml")); 
            Parent root = loader.load(); 

            Object controller = loader.getController(); 
            
            if (controller instanceof BaseMenuController) { 
                ((BaseMenuController) controller).setClient(client, userType, userId); 
            } else if (controller instanceof ReservationHistoryController) { 
                ((ReservationHistoryController) controller).setClient(client); 
                ((ReservationHistoryController) controller).loadReservationsForUser(userId); 
            } 
            
            showNewWindow(root, "Order History"); 
        } catch (IOException e) { 
            e.printStackTrace(); 
        } 
    } 
    
    /**
     * Loads the Edit Profile frame and injects required session data.
     * @param client The active network client.
     * @param userId The unique identifier of the subscriber.
     * @return None.
     */
    @Override 
    public void editPersonalDetails(ChatClient client, int userId) { 
        try { 
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/SubscriberFXML/EditSubscriberDetailsFrame.fxml")); 
            Parent root = loader.load(); 

            Object controller = loader.getController(); 
            
            if (controller instanceof BaseMenuController) { 
                ((BaseMenuController) controller).setClient(client, userType, userId); 
            } else if (controller instanceof EditSubscriberDetailsController) { 
                ((EditSubscriberDetailsController) controller).setClient(client); 
                ((EditSubscriberDetailsController) controller).setUserId(userId); 
            } 

            showNewWindow(root, "Edit Profile"); 
        } catch (IOException e) { 
            e.printStackTrace(); 
        } 
    } 

    /**
     * Internal helper to create and display a new Stage.
     * @param root  The root node of the new window.
     * @param title The title to be displayed in the window header.
     * @return None.
     */
    private void showNewWindow(Parent root, String title) { 
        Stage stage = new Stage(); 
        stage.setTitle(title); 
        stage.setScene(new Scene(root)); 
        stage.show(); 
    } 

    // --- 5. Session Termination (Logout) ---

    /**
     * Logs the user out and returns to the primary login portal.
     * @param event The ActionEvent triggered by the logout button.
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
            appendLog("Error during logout: " + e.getMessage()); 
        } 
    } 
    
    /**
     * Returns staff users (Manager/Representative) back to their respective dashboards.
     * @param event The ActionEvent triggered by the back button.
     * @return None.
     */
    @FXML
    void clickBack(ActionEvent event) {
        try {
            String fxmlPath;

            if ("Manager".equalsIgnoreCase(originalUserType)) {
                fxmlPath = "/managmentGUI/ManagerDashboard.fxml";
            } else {
                fxmlPath = "/managmentGUI/RepresentativeDashboard.fxml";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof BaseMenuController) {
                BaseMenuController base = (BaseMenuController) controller;
                base.setClient(client, originalUserType, userId);
                base.setActingAsSubscriber(false); 
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            appendLog("Navigation Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- 6. Server Communication Handling ---

    /**
     * Processes server responses and updates the UI feedback logs.
     * @param message The response message object from the server.
     * @return None.
     */
    @Override 
    public void display(Object message) { 
        if (message != null) { 
            appendLog(message.toString()); 
        } 
        
        if (message instanceof String) { 
            String response = (String) message; 
            
            boolean isWaitResponse = (response.startsWith("CANCEL_WAITING") || response.equals("NOT_ON_WAITING_LIST")); 
            
            if (isWaitResponse) { 
                ExitWaitingListHelper.handleServerResponse(response); 
            } 
        } 
    } 

    /**
     * Appends text to the GUI log area in a thread-safe manner using Platform.runLater.
     * @param message The string message to be displayed in the log.
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