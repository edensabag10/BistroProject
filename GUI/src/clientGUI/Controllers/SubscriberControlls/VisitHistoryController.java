package clientGUI.Controllers.SubscriberControlls;

import java.util.ArrayList;
import common.Visit;
import client.ChatClient;
import common.ChatIF;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controller for the Visit History screen in the Bistro system.
 * This class manages the display of past dining sessions (visits) for a specific subscriber.
 * It implements ChatIF to receive asynchronous updates from the server.
 */
public class VisitHistoryController implements ChatIF {

    @FXML private TableView<Visit> visitsTable;
    @FXML private TableColumn<Visit, Long> codeCol;
    @FXML private TableColumn<Visit, Integer> tableCol;
    @FXML private TableColumn<Visit, Long> billCol;
    @FXML private TableColumn<Visit, String> timeCol;
    @FXML private TableColumn<Visit, String> statusCol;

    private ChatClient client;

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded. It sets up the table columns.
     * @return None.
     */
    @FXML
    public void initialize() {
        codeCol.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        tableCol.setCellValueFactory(new PropertyValueFactory<>("tableId"));
        billCol.setCellValueFactory(new PropertyValueFactory<>("billId"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    /**
     * Injects the ChatClient instance and sets this controller as the active UI handler.
     * @param client The active ChatClient instance.
     * @return None.
     */
    public void setClient(ChatClient client) {
        this.client = client;
        client.setUI(this);
    }

    /**
     * Handles the 'Close' button action. Closes the current history window.
     * @param event The ActionEvent triggered by clicking the button.
     * @return None.
     */
    @FXML
    void clickBack(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    /**
     * Requests the visit history for a specific subscriber from the server.
     * @param userId The unique ID of the subscriber.
     * @return None.
     */
    public void loadVisitsForUser(int userId) {
        ArrayList<Object> msg = new ArrayList<>();
        msg.add("GET_VISITS_HISTORY");
        msg.add(userId);
        client.handleMessageFromClientUI(msg);
    }

    /**
     * Processes messages received from the server. Updates the TableView with visit data.
     * @param message The response message from the server (expected ArrayList).
     * @return None.
     */
    @Override
    public void display(Object message) {
        if (message instanceof ArrayList) {
            ArrayList<?> data = (ArrayList<?>) message;
            if ("VISIT_HISTORY".equals(data.get(0))) {
                ArrayList<Visit> visits = (ArrayList<Visit>) data.get(1);
                Platform.runLater(() -> {
                    if (visits == null || visits.isEmpty()) {
                        showNoHistoryAlert();
                        return;
                    }
                    visitsTable.getItems().clear();
                    visitsTable.getItems().addAll(visits);
                });
            }
        }
    }

    /**
     * Displays an information alert when no history records exist for the user.
     * @return None.
     */
    private void showNoHistoryAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Visit History");
        alert.setHeaderText(null);
        alert.setContentText("No visit history found.");
        alert.showAndWait();
    }
}