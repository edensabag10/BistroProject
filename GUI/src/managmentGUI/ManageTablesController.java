package managmentGUI;

import java.util.ArrayList;
import clientGUI.Controllers.MenuControlls.BaseMenuController;
import common.ServiceResponse;
import common.Table;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

/**
 * Controller class for the Table Management interface.
 * This class provides administrative functionality to view the restaurant's table layout, 
 * add new tables, update existing table capacities, and delete available tables.
 */
public class ManageTablesController extends BaseMenuController {

    @FXML private TableView<Table> tableTablesView;
    @FXML private TableColumn<Table, Integer> colTableId;
    @FXML private TableColumn<Table, Integer> colCapacity;
    @FXML private TableColumn<Table, String> colStatus;
    @FXML private TableColumn<Table, Void> colDeleteAction;
    @FXML private TextField txtNewTableCapacity;

    /**
     * Triggered when the communication client is ready. 
     * Sets this controller as the active UI listener and fetches the initial table list.
     * @return None.
     */
    @Override
    public void onClientReady() {
        if (client != null) {
            client.setUI(this); // Register this controller to receive server messages
            setupTableColumns(); // Configure table columns and cell factories
            refreshTableData();  // Initial data fetch from server
        }
    }
    
    /**
     * Configures the TableView columns, including data mapping and custom cell factories.
     * Injects 'Update' and 'Delete' buttons into the action column for each row.
     * @return None.
     */
    private void setupTableColumns() {
        // Standard data mapping
        colTableId.setCellValueFactory(new PropertyValueFactory<>("tableId"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        
        // Dynamic status mapping (Boolean to String)
        colStatus.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().isAvailable() ? "Available" : "Occupied"));

        // Custom cell factory to inject 'Update' and 'Delete' buttons into each row
        colDeleteAction.setCellFactory(param -> new TableCell<Table, Void>() {
            private final Button btnDelete = new Button("Delete");
            private final Button btnUpdate = new Button("Update");
            private final HBox pane = new HBox(btnUpdate, btnDelete);

            {
                pane.setSpacing(10);
                // Apply styling to buttons
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
                btnUpdate.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

                // --- Update Capacity Logic ---
                btnUpdate.setOnAction(e -> {
                    Table t = getTableView().getItems().get(getIndex());
                    
                    // Show input dialog to get new capacity
                    TextInputDialog dialog = new TextInputDialog(String.valueOf(t.getCapacity()));
                    dialog.setTitle("Update Capacity");
                    dialog.setHeaderText("Update capacity for Table #" + t.getTableId());
                    dialog.setContentText("Enter new capacity (must be greater than 0):");

                    // Validate: Must be a positive integer
                    dialog.showAndWait().ifPresent(val -> {
                        if (val.matches("\\d+") && Integer.parseInt(val) > 0) {
                            logToUI("Requesting update to capacity " + val + " for Table #" + t.getTableId());
                            ArrayList<Object> msg = new ArrayList<>();
                            msg.add("UPDATE_TABLE_CAPACITY");
                            msg.add(t.getTableId());
                            msg.add(Integer.parseInt(val));
                            client.handleMessageFromClientUI(msg);
                        } else {
                            logToUI("UPDATE FAILED: Invalid capacity input '" + val + "'");
                            new Alert(Alert.AlertType.ERROR, "Invalid Input: Capacity must be a number greater than 0!").show();
                        }
                    });
                });

                // --- Delete Table Logic ---
                btnDelete.setOnAction(e -> {
                    Table t = getTableView().getItems().get(getIndex());
                    
                    // Cannot delete tables that are currently occupied
                    if (!t.isAvailable()) {
                        logToUI("DELETE BLOCKED: Table #" + t.getTableId() + " is occupied.");
                        new Alert(Alert.AlertType.WARNING, "Cannot delete an occupied table!").show();
                        return;
                    }
                    
                    // Show confirmation dialog before proceeding
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete table #" + t.getTableId() + "?", ButtonType.YES, ButtonType.NO);
                    if (confirm.showAndWait().get() == ButtonType.YES) {
                        logToUI("Requesting deletion of Table #" + t.getTableId());
                        ArrayList<Object> msg = new ArrayList<>();
                        msg.add("DELETE_TABLE");
                        msg.add(t.getTableId());
                        client.handleMessageFromClientUI(msg);
                    }
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    /**
     * Transmits a request to the server to fetch the complete list of restaurant tables.
     * @return None.
     */
    private void refreshTableData() {
        ArrayList<Object> msg = new ArrayList<>();
        msg.add("GET_ALL_TABLES");
        client.handleMessageFromClientUI(msg);
    }

    /**
     * Handles the 'Add Table' action. Validates the capacity input and requests a new table creation.
     * @param event The ActionEvent triggered by the add button.
     * @return None.
     */
    @FXML
    void onAddTableClicked(ActionEvent event) {
        String input = txtNewTableCapacity.getText().trim();
        // Validation: Positive integer check
        if (input.matches("\\d+") && Integer.parseInt(input) > 0) {
            int capacity = Integer.parseInt(input);
            logToUI("Attempting to add new table with capacity: " + capacity);
            
            ArrayList<Object> msg = new ArrayList<>();
            msg.add("ADD_NEW_TABLE");
            msg.add(capacity);
            client.handleMessageFromClientUI(msg);
            txtNewTableCapacity.clear();
        } else {
            logToUI("ADD FAILED: Invalid capacity '" + input + "'");
            new Alert(Alert.AlertType.ERROR, "Invalid Input: Capacity must be a number greater than 0!").show();
        }
    }

    /**
     * Processes server responses, including table lists and execution status messages.
     * Automatically filters out Archive Table ID -1 to maintain UI integrity.
     * @param message The message object received from the server.
     * @return None.
     */
    @Override
    public void display(Object message) {
        if (message instanceof ArrayList) {
            ArrayList<?> list = (ArrayList<?>) message;
            if (!list.isEmpty() && list.get(0) instanceof Table) {

                // --- Filtering Archive Table (-1) ---
                // We filter out the table with ID -1 as it is only used for DB integrity (archives)
                ArrayList<Table> filtered = new ArrayList<>();
                for (Object o : list) {
                    Table t = (Table) o;
                    if (t.getTableId() != -1) filtered.add(t);
                }
                // Update UI on the JavaFX Application Thread
                Platform.runLater(() -> tableTablesView.setItems(FXCollections.observableArrayList(filtered)));
            }
        } else if (message instanceof ServiceResponse) {
            ServiceResponse res = (ServiceResponse) message;
            Platform.runLater(() -> {
                if (res.getStatus() == ServiceResponse.ServiceStatus.UPDATE_SUCCESS) {
                    // Show success alert and refresh the table view
                    Alert success = new Alert(Alert.AlertType.INFORMATION, res.getMessage());
                    success.setTitle("Success");
                    success.setHeaderText(null);
                    success.show();
                    refreshTableData();
                } else {
                    // Show error alert received from server
                    new Alert(Alert.AlertType.ERROR, res.getMessage()).show();
                }
            });
        }
    }
    
    /**
     * Helper method to output log messages to the main Dashboard's log area.
     * Dynamically looks up the '#txtLog' component in the current scene graph.
     * @param message The text string to log.
     * @return None.
     */
    private void logToUI(String message) {
        Platform.runLater(() -> {
            if (tableTablesView.getScene() != null) {
                TextArea txtLog = (TextArea) tableTablesView.getScene().lookup("#txtLog");
                if (txtLog != null) {
                    txtLog.appendText("> [Tables] " + message + "\n");
                }
            }
        });
    }
}