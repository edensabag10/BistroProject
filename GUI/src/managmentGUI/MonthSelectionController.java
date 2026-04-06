package managmentGUI;

import java.io.IOException;
import java.util.ArrayList;

import client.ChatClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

/**
 * Controller class for the Month Selection interface.
 * This class allows the manager to select a specific month and request 
 * statistical reports (Time and Subscriber reports) from the server via the OCSF client.
 */
public class MonthSelectionController {

    /** Reference to the OCSF client for network communication. */
    private ChatClient client;

    @FXML
    private ComboBox<String> monthCombo;

    @FXML
    private Button timeRepBtn;

    @FXML
    private Button subRepBtn;

    @FXML
    private Button btnback;

    /**
     * Injects the active network client into this controller. 
     * Part of the "Pipe" architecture used to maintain the server connection across windows.
     * @param client The active ChatClient instance.
     * @return None.
     */
    public void setClient(ChatClient client) {
        this.client = client;
    }

    /**
     * Closes the current month selection window and returns to the dashboard.
     * @param event The ActionEvent triggered by the back button.
     * @return None.
     */
    @FXML
    void closeWin(ActionEvent event) {
        Stage stage = (Stage) btnback.getScene().getWindow();
        stage.close();
    }

    /**
     * Validates input and transmits a request to generate a Time Report for the selected month.
     * Requests data for Restaurant ID 1 (standard project configuration).
     * @param event The ActionEvent triggered by the generate time report button.
     * @return None.
     */
    @FXML
    void generateTimeReports(ActionEvent event) {

        String selectedMonth = monthCombo.getValue();

        if (selectedMonth == null) {
            showPopup("Error", "Please select month first");
            return;
        }

        ArrayList<Object> message = new ArrayList<>();

        message.add("GET_TIME_REPORTS");
        message.add(1);                 // Fixed Restaurant ID
        message.add(selectedMonth);     // Selected month payload

        if (client != null) {
            client.handleMessageFromClientUI(message);
        }
    }

    /**
     * Validates input and transmits a request to generate a Subscriber Activity Report.
     * Uses direct server transmission via the client's sendToServer method.
     * @param event The ActionEvent triggered by the generate subscriber report button.
     * @return None.
     */
    @FXML
    void generateSubscriberReports(ActionEvent event) {
        String selectedMonth = monthCombo.getValue();
        if (selectedMonth == null) {
            return;
        }

        ArrayList<Object> message = new ArrayList<>();
        message.add("GET_SUBSCRIBER_REPORTS");
        message.add(selectedMonth);

        try {
            if (client != null) {
                client.sendToServer(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays a standardized information popup to the user for validation or feedback.
     * @param title The title of the alert window.
     * @param text  The content message to be displayed.
     * @return None.
     */
    private void showPopup(String title, String text) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.show();
    }
}