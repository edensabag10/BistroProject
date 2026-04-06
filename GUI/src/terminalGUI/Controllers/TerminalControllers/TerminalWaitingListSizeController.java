package terminalGUI.Controllers.TerminalControllers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Map;

import client.ChatClient;
import common.ChatIF;
import common.LoginSource;
import common.ServiceResponse;
import common.ServiceResponse.ServiceStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the Waiting List size selection screen at the service terminal.
 * This class handles the input for the number of diners, validates the request, 
 * and processes server responses for either immediate table assignment or 
 * entry into the restaurant's waiting list.
 */
public class TerminalWaitingListSizeController implements ChatIF {

    /** Reference to the active terminal client connection for network communication. */
    private ChatClient client;
    
    /** Input field for the user to specify the number of diners in the group. */
    @FXML
    private TextField txtDiners; 
    
    /** Label used to display validation errors or server-side rejection messages. */
    @FXML
    private Label lblError; 

    /**
     * Injects the persistent ChatClient instance and registers this controller 
     * as the active UI listener for server messages.
     * @param client The active ChatClient instance.
     * @return None.
     */
    public void setClient(ChatClient client) {
        this.client = client;
        if (client != null) {
            client.setUI(this);
        }
    }

    /**
     * Navigates the UI back to the primary Terminal Menu screen.
     * @param event The ActionEvent triggered by the back button.
     * @return None.
     */
    @FXML
    private void backToTerminal(ActionEvent event) {
        try {
            // Load the terminal menu FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/Terminal/TerminalMenuFrame.fxml"));
            Parent root = loader.load();

            // Pass the client reference to the terminal menu controller
            Object controller = loader.getController();
            if (controller instanceof TerminalMenuController) {
                ((TerminalMenuController) controller).setClient(this.client);
            }

            // Replace the current scene
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load terminal menu");
        }
    }

    /**
     * Validates the diner count input and transmits a JOIN_WAITING_LIST request 
     * to the server if the input is valid.
     * @param event The ActionEvent triggered by the continue button.
     * @return None.
     */
    @FXML
    private void handleContinue(ActionEvent event) {
        int diners;

        // Input validation: empty field check
        if (txtDiners.getText().isEmpty()) {
            showError("Please enter number of diners");
            return;
        }

        // Input validation: numeric value verification
        try {
            diners = Integer.parseInt(txtDiners.getText());
        } catch (NumberFormatException e) {
            showError("Number of diners must be a number");
            return;
        }

        // Input validation: positive integer constraint
        if (diners <= 0) {
            showError("Number of diners must be positive");
            return;
        }

        // Build protocol message for the server controller
        ArrayList<Object> msg = new ArrayList<>();
        msg.add("JOIN_WAITING_LIST"); 
        msg.add(diners);              

        // Send request to server via the communication pipe
        if (client != null) {
            client.handleMessageFromClientUI(msg);
        } else {
            showError("No server connection");
        }
    }
    
    /**
     * Updates the error label with the specified message and makes it visible.
     * @param msg The error message to display.
     * @return None.
     */
    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
    }

    /**
     * Receives messages from the server. Transfers execution to the JavaFX 
     * application thread to safely update UI components.
     * @param message The message object received from the server.
     * @return None.
     */
    @Override
    public void display(Object message) {
        Platform.runLater(() -> handleServerMessage(message));
    }
    
    /**
     * Orchestrates the response logic based on the server's ServiceResponse.
     * Handles errors (CLOSED, ALREADY_IN_LIST) and success modes (IMMEDIATE vs WAITING).
     * 
     * @param message The response object to be parsed.
     * @return None.
     */
    @SuppressWarnings("unchecked")
    private void handleServerMessage(Object message) {

        // Validate message type to prevent casting exceptions
        if (!(message instanceof ServiceResponse)) {
            return;
        }

        ServiceResponse response = (ServiceResponse) message;

        // Handle logical rejections or system errors
        if (response.getStatus() == ServiceStatus.INTERNAL_ERROR) {
            String code = response.getData().toString();

            if ("ALREADY_IN_LIST".equals(code)) {
                showError("You are already on the waiting list.");
            }
            else if ("RESTAURANT_CLOSED".equals(code)) {
                showError("The restaurant is currently closed – you cannot join the waiting list.");
            }
            else {
                showError("Error: " + code);
            }

            return;
        }

        // Terminate if the server did not report a successful update
        if (response.getStatus() != ServiceStatus.UPDATE_SUCCESS) {
            return;
        }

        Object data = response.getData();

        // Mode 1: Immediate entry (Table found without waiting)
        if (data instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) data;

            if ("IMMEDIATE".equals(map.get("mode"))) {
                long confirmationCode = ((Number) map.get("confirmationCode")).longValue();
                int tableId = ((Number) map.get("tableId")).intValue();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Table Available");
                alert.setHeaderText(null);
                alert.setContentText(
                    "A table is available – you can enter now.\n\n" +
                    "Table number: " + tableId + "\n" +
                    "Confirmation code: " + confirmationCode);
                alert.showAndWait();
                return;
            }
        }

        // Mode 2: Standard waiting list addition
        String confirmationCode = data.toString();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Waiting List");
        alert.setHeaderText(null);
        alert.setContentText(
            "You have been added to the waiting list.\n\n" +
            "Confirmation code: " + confirmationCode);
        alert.showAndWait();
    }
}