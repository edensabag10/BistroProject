package terminalGUI.Controllers.TerminalControllers;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.util.List;
import java.util.ArrayList;

import client.ChatClient;
import clientGUI.Controllers.RemoteLoginController;
import clientGUI.Controllers.MenuControlls.BaseMenuController;
import common.ChatIF;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

/**
 * Controller for the main Customer Service Terminal menu.
 * This class handles the primary navigation for walk-in customers and existing reservation 
 * holders, providing access to arrival confirmation, waiting list management, and 
 * reservation code recovery.
 * * @author Software Engineering Student
 * @version 1.0
 */
public class TerminalMenuController extends BaseMenuController implements ChatIF {

    // FXML Button Bindings
    @FXML
    private Button btnLostConfirmationCode;
    
    @FXML
    private Button btnManageReservation;

    @FXML
    private Button btnJoinWaitingList;

    @FXML
    private Button btnArrival;
   
    /**
     * Injects the shared ChatClient instance into the controller to maintain the network session.
     * @param client The active network client instance.
     * @return None.
     */
    public void setClient(ChatClient client) {
        this.client = client;
    }
    
    /**
     * Handles the "Lost Reservation Code" action. Sends a request to the server 
     * to retrieve all active codes associated with the current user's ID.
     * @param event The ActionEvent triggered by the button click.
     * @return None.
     */
    @FXML
    private void handleLostConfirmationCode(ActionEvent event) {
        ArrayList<Object> message = new ArrayList<>();
        message.add("CARD_READER_GET_CODES");
        message.add(String.valueOf(userId)); 

        if (client != null) {
            client.handleMessageFromClientUI(message);
        }
    }
    
    /**
     * Handles the "Manage Reservation" action. Navigates the user to the management 
     * frame where they can cancel or pay for their reservation.
     * @param event The ActionEvent triggered by the button click.
     * @return None.
     */
    @FXML
    private void handleManageReservation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/Terminal/ManageReservationFrame.fxml"));
            Parent root = loader.load();

            ManageReservationController controller = loader.getController();
            controller.setClient(this.client, "Terminal", -1); 

            Stage stage = (Stage) btnManageReservation.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Bistro - Manage Reservation");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Handles the "Join Waiting List" action. Navigates the user to the waiting list 
     * configuration screen to select group size and join the queue.
     * @param event The ActionEvent triggered by the button click.
     * @return None.
     */
    @FXML
    private void handleJoinWaitingList(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                    "/clientGUI/fxmlFiles/Terminal/TerminalWaitingListSizeFrame.fxml"
                )
            );

            Parent root = loader.load();

            TerminalWaitingListSizeController controller = loader.getController();
            controller.setClient(client);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Join Waiting List");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }    
    }
    
    /**
     * Handles the "I'm Here" (Arrival) action. Transitions to the code entry screen 
     * for reservation confirmation and table assignment.
     * @param event The ActionEvent triggered by the button click.
     * @return None.
     */
    @FXML
    private void handleArrival(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/fxmlFiles/Terminal/TerminalArrivalFrame.fxml"));
            Parent root = loader.load();

            VisitUIController controller = loader.getController();
            controller.setClient(client);
            controller.onClientReady(); 

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Bistro - Customer Arrival");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Logs the current session out from the terminal and returns to the terminal login portal.
     * @param event The ActionEvent triggered by the logout button.
     * @return None.
     */
    @FXML
    private void clickLogOut(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/clientGUI/fxmlFiles/TerminalLoginFrame.fxml")
            );
            Parent root = loader.load();

            Object nextController = loader.getController();
            if (nextController instanceof TerminalLoginController) {
                ((TerminalLoginController) nextController).setClient(client);
            }

            if (nextController instanceof ChatIF && client != null) {
                client.setUI((ChatIF) nextController);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);

            if (getClass().getResource("/clientGUI/cssStyle/GlobalStyles.css") != null) {
                scene.getStylesheets().add(
                    getClass().getResource("/clientGUI/cssStyle/GlobalStyles.css").toExternalForm()
                );
            }

            stage.setTitle("Bistro - Terminal Login");
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Receives and processes messages from the server.
     * Specifically handles the recovery of active reservation codes and displays 
     * them in a thread-safe UI alert.
     * @param message The server response object (expected to be a List of strings).
     * @return None.
     */
    @Override
    public void display(Object message) {
        if (message instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> codes = (List<String>) message;
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Code Recovery");
                alert.setHeaderText("Results for ID: " + userId);
                if (codes.isEmpty()) {
                    alert.setContentText("No active reservations found.");
                } else {
                    alert.setContentText("Your active codes:\n" + String.join("\n", codes));
                }
                alert.showAndWait();
            });
        }
    }

    /**
     * Internal utility method to display a standardized information alert.
     * @param title   The alert window title.
     * @param content The message body.
     * @return None.
     */
    private void showInformationAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}