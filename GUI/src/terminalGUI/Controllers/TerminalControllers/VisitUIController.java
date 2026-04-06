package terminalGUI.Controllers.TerminalControllers;

import java.util.ArrayList;

import client.ChatClient;
import common.ChatIF;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller class for the Arrival Terminal interface.
 * This class facilitates the check-in process for customers upon arrival, 
 * validating confirmation codes and providing real-time status updates regarding 
 * table availability through active server polling.
 */
public class VisitUIController implements ChatIF {
	
    /** Persistent network client used for server communication. */
    private ChatClient client;
    
    /** Timer used to poll the server for status updates when a table is not immediately ready. */
    private Timeline statusTimer;

    // FXML injected UI components
    @FXML private TextField txtCode; 
    @FXML private Button btnVerify; 
    @FXML private Button btnBack; 
    @FXML private Label lblStatus; 
    
    /**
     * Injects the shared ChatClient instance into the controller.
     * @param client The active network client instance.
     * @return None.
     */
    public void setClient(ChatClient client) {
        this.client = client;
    }


    /**
     * Handles the "I'm Here" verification process. Validates the input format 
     * and transmits the arrival request to the server.
     * @param event The ActionEvent triggered by the verification button.
     * @return None.
     */
    @FXML
    void onVerifyClicked(ActionEvent event) {
        String codeStr = txtCode.getText().trim();

        // 1. Validation: check if empty
        if (codeStr.isEmpty()) {
            showAlert("Warning", "Please enter your confirmation code.", AlertType.WARNING);
            return;
        }

        try {
            // 2. Format validation: parse to long
            long code = Long.parseLong(codeStr);
            
            // 3. Prepare the message for the server [Command, Data]
            ArrayList<Object> message = new ArrayList<>();
            message.add("PROCESS_TERMINAL_ARRIVAL");
            message.add(code);
            
            // 4. Send request to server
            client.handleMessageFromClientUI(message);
            lblStatus.setText("Verifying arrival...");

        } catch (NumberFormatException e) {
            showAlert("Error", "Code must be numeric.", AlertType.ERROR);
        }
    }

    /**
     * Processes specific response strings from the server. 
     * Handles logic for invalid codes, early arrivals, and table readiness.
     * @param response The response string received from the server.
     * @return None.
     */
    private void handleServerResponse(String response) {
        lblStatus.setText(""); // Clear waiting message

        if (response.equals("INVALID_CODE")) {
            showAlert("Arrival Failed", "Invalid confirmation code or booking not confirmed.", AlertType.ERROR);
        } 
        else if (response.equals("TOO_EARLY")) {
            showAlert("Too Early", "You arrived more than 15 minutes early. Please come back later.", AlertType.INFORMATION);
        } 
        else if (response.equals("TABLE_NOT_READY_WAIT")) {
            showAlert("Welcome", "Your table is not ready yet. Please wait, we will notify you via SMS.", AlertType.INFORMATION);
            startPollingForStatus(Long.parseLong(txtCode.getText().trim()));
        } 
        else if (response.startsWith("SUCCESS_TABLE_")) {
            String tableId = response.split("_")[2]; // Extract table number from the string
            showAlert("Welcome!", "Your table is ready! Please proceed to Table #" + tableId, AlertType.CONFIRMATION);
        } 
        else if (response.equals("DATABASE_ERROR")) {
            showAlert("System Error", "Communication with database failed. Please see the host.", AlertType.ERROR);
        }
    }
    
    /**
     * Initiates a background Timeline that polls the server every 5 seconds 
     * to check if the assigned table has become available.
     * @param code The reservation confirmation code to poll for.
     * @return None.
     */
    private void startPollingForStatus(long code) {
        if (statusTimer != null) statusTimer.stop();

        statusTimer = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            // Prepare request to check status
            ArrayList<Object> message = new ArrayList<>();
            message.add("CHECK_STATUS_UPDATE");
            message.add(code);
            
            // Send to server (the response will be handled in the display() method)
            client.handleMessageFromClientUI(message);
        }));
        
        statusTimer.setCycleCount(Timeline.INDEFINITE);
        statusTimer.play();
    }
    
    /**
     * Handles server messages on the UI thread. 
     * Specifically manages polling updates and notifies the user when the status changes to "NOTIFIED".
     * 
     * @param message The message object received from the server.
     * @return None.
     */
    @Override
    public void display(Object message) {
        if (message instanceof String) {
            String response = (String) message;
            
            Platform.runLater(() -> {
                if (response.equals("NOTIFIED")) {
                    if (statusTimer != null) statusTimer.stop();
                    showTableReadyPopup();
                } 
                else if (response.equals("WAITING_AT_RESTAURANT") || response.equals("WAITING")) {
                    // Silence polling updates that maintain current state
                }
                else {
                    handleServerResponse(response);
                }
            });
        }
    }

    /**
     * Displays a success popup when a table is ready and stops the active polling timer.
     * @return None.
     */
    private void showTableReadyPopup() {
        if (statusTimer != null) statusTimer.stop();

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Table Ready!");
        alert.setHeaderText("Good news!");
        alert.setContentText("Your table is now ready! Please enter your code again to get your table number.");
        alert.showAndWait();
    }
    
    /**
     * Utility method to display a standardized JavaFX Alert dialog.
     * @param title   The window title of the alert.
     * @param content The main body text of the alert.
     * @param type    The visual style of the alert (Error, Warning, Information).
     * @return None.
     */
    private void showAlert(String title, String content, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Navigates the user back to the primary Terminal Menu.
     * @param event The ActionEvent from the back button click.
     * @return None.
     */
    @FXML
    void onBackClicked(ActionEvent event) {
    	
    	try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/Terminal/TerminalMenuFrame.fxml"));
            Parent root = loader.load();

            TerminalMenuController controller = loader.getController();
            controller.setClient(client);

            Stage stage = (Stage) btnBack.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Bistro - Service Terminal");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load menu screen.", AlertType.ERROR);
        }

    }
    
    /**
     * Registers this controller instance as the active UI for the ChatClient 
     * upon successful connection initialization.
     * @return None.
     */
    public void onClientReady() {
        if (client != null) {
            client.setUI(this); 
        }
    }
    
}