package terminalGUI.Controllers.TerminalControllers;

import java.util.ArrayList;

import client.ChatClient;
import clientGUI.Controllers.MenuControlls.BaseMenuController;
import clientGUI.Controllers.MenuControlls.PayBillEntryController;
import common.ChatIF;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

/**
 * Controller class for the Manage Reservation screen at the physical Service Terminal.
 * This class allows users to cancel active reservations, leave the waiting list, 
 * or proceed to payment by providing their unique confirmation codes.
 */
public class ManageReservationController extends BaseMenuController implements ChatIF {
        
    @FXML private Button btnCancel, btnExitWaiting, btnPay, btnBack;

    /**
     * Injects the communication client and registers this controller as the active 
     * UI for processing server responses.
     * @param client   The active ChatClient instance.
     * @param userType The role of the user (e.g., "Terminal").
     * @param userId   The unique identification of the user.
     * @return None.
     */
    @Override
    public void setClient(ChatClient client, String userType, int userId) {
        super.setClient(client, userType, userId);
        if (this.client != null) {
            this.client.setUI(this);
        }
    }

    /**
     * Initiates the reservation cancellation flow. Prompts the user for a confirmation 
     * code via a dialog and transmits the request to the server.
     * @param event The ActionEvent triggered by the cancel button.
     * @return None.
     */
    @FXML
    void onCancelReservation(ActionEvent event) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Cancel Reservation");
        dialog.setHeaderText("Reservation Cancellation");
        dialog.setContentText("Please enter your confirmation code:");

        dialog.showAndWait().ifPresent(codeStr -> {
            try {
                long code = Long.parseLong(codeStr.trim());

                ArrayList<Object> message = new ArrayList<>();
                message.add("CANCEL_RESERVATION");
                message.add(code);
                client.handleMessageFromClientUI(message);
                
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "The code must be a number.", AlertType.ERROR);
            }
        });
    }

    /**
     * Processes the request to exit the waiting list. Prompts the user for their 
     * waiting list code and sends the cancellation request to the server.
     * @param event The ActionEvent triggered by the exit waiting list button.
     * @return None.
     */
    @FXML
    void onExitWaitingList(ActionEvent event) {	
        // Creating the input dialog for the confirmation code
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Exit Waiting List");
        dialog.setHeaderText("Confirmation Code Required");
        dialog.setContentText("Please enter your Waiting List Confirmation Code:");

        dialog.showAndWait().ifPresent(codeStr -> {
            try {
                // Parse the input as a long
                long code = Long.parseLong(codeStr.trim());

                // Structure the message for the ServerController 
                ArrayList<Object> message = new ArrayList<>();
                message.add("CANCEL_WAITING_LIST_BY_CODE");
                message.add(code);

                // Send to server via ChatClient
                client.handleMessageFromClientUI(message);

            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "The code must be a numeric value.", AlertType.ERROR);
            }
        });
    }

    /**
     * Navigates the user to the payment entry screen. Sets the context to "Terminal" 
     * mode to ensure appropriate return navigation.
     * @param event The ActionEvent triggered by the pay button.
     * @return None.
     */
    @FXML
    void onPayReservation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/MenuFXML/PayBillEntryFrame.fxml"));
            Parent root = loader.load();

            PayBillEntryController controller = loader.getController();

            controller.setClient(this.client, "Terminal", -1); 

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Bistro - Pay Bill");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not load the payment screen.", AlertType.ERROR);
        }
    }

    /**
     * Returns the user to the main Terminal Menu screen.
     * @param event The ActionEvent triggered by the back button.
     * @return None.
     */
    @FXML
    void onBackClicked(ActionEvent event) {
        try {
            // Load the Terminal Menu screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/Terminal/TerminalMenuFrame.fxml"));
            Parent root = loader.load();

            // Inject ChatClient into the menu controller
            TerminalMenuController controller = loader.getController();
            controller.setClient(client);

            // Switch scene
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
     * Internal utility method to display standardized JavaFX Alert dialogs.
     * @param title   The title of the alert window.
     * @param content The message body to be displayed.
     * @param type    The AlertType defining the visual style (Success, Error, Warning).
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
     * Processes incoming string messages from the server on the JavaFX thread.
     * Provides feedback popups based on the success or failure of cancellations.
     * @param message The response object from the server.
     * @return None.
     */
    
    @Override
    public void display(Object message) {
        if (message instanceof String) {
            String response = (String) message;

            Platform.runLater(() -> {
                switch (response) {
                    case "CANCEL_SUCCESS":
                        showAlert("Success", "Your reservation has been canceled successfully.", AlertType.INFORMATION);
                        break;
                    case "CANCEL_FAILED":
                        showAlert("Failure", "Reservation not found or cannot be canceled.", AlertType.ERROR);
                        break;
                    case "CANCEL_WAITING_SUCCESS":
                        showAlert("Success", "You have been successfully removed from the waiting list.", AlertType.INFORMATION);
                        break;
                    case "NOT_ON_WAITING_LIST":
                        showAlert("Notice", "No active waiting list entry found for this code.", AlertType.WARNING);
                        break;
                    case "SERVER_ERROR":
                        showAlert("Error", "A server error occurred. Please try again later.", AlertType.ERROR);
                        break;

                    default:
                        System.out.println("[Terminal] Received message: " + response);
                        break;
                }
            });
        }
    }

}