package managmentGUI;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import common.WaitingListEntry;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller for the Waiting List sub-screen within the management dashboard.
 * This class handles the visualization of customers currently on the restaurant's 
 * waiting list, mapping data objects to a TableView for staff monitoring.
 */
public class WaitingListController implements Initializable {

    // --- FXML Components ---
    
    /** The main table component for displaying waiting list records. */
    @FXML private TableView<WaitingListEntry> waitingTable;
    
    /** Column for the unique confirmation code of the waiting entry. */
    @FXML private TableColumn<WaitingListEntry, Long> colCode;
    
    /** Column for the timestamp indicating when the customer joined the list. */
    @FXML private TableColumn<WaitingListEntry, String> colTime;
    
    /** Column for the user ID associated with the entry. */
    @FXML private TableColumn<WaitingListEntry, Integer> colUserId;

    /**
     * Initializes the table columns by mapping them to the specific properties 
     * defined in the WaitingListEntry DTO.
     * @param location  The location used to resolve relative paths for the root object.
     * @param resources The resources used to localize the root object.
     * @return None.
     */
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Mapping TableColumns to WaitingListEntry property names
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("entryTime"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
    }

    /**
     * Populates the UI table with a list of entries received from the server.
     * Validates that both the table component and the data list are initialized.
     * @param waitingList An ArrayList of WaitingListEntry objects to display.
     * @return None.
     */
    public void setTableData(ArrayList<WaitingListEntry> waitingList) {
        if (waitingTable != null && waitingList != null) {
            // Convert ArrayList to ObservableList and update the UI items
            waitingTable.setItems(FXCollections.observableArrayList(waitingList));
        }
    }
}