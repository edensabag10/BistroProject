package managmentGUI;

import java.util.ArrayList;
import common.Subscriber;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller class for the Subscribers List management view.
 * This class handles the display of all registered subscribers in a TableView,
 * allowing staff members to view user IDs, subscriber IDs, usernames, and unique QR codes.
 */
public class SubscribersListController {

    /** The table component used to display subscriber records. */
    @FXML private TableView<Subscriber> tableSubscribers;

    /** Column for the system user ID. */
    @FXML private TableColumn<Subscriber, Integer> colUserId;

    /** Column for the unique subscriber identifier. */
    @FXML private TableColumn<Subscriber, Integer> colSubId;

    /** Column for the subscriber's username. */
    @FXML private TableColumn<Subscriber, String> colUsername;

    /** Column for the subscriber's personalized QR code identifier. */
    @FXML private TableColumn<Subscriber, String> colQrCode;

    /**
     * Initializes the table columns by mapping UI columns to the properties 
     * defined in the Subscriber entity class.
     * @return None.
     */
    
    @FXML
    public void initialize() {
        // Mapping TableColumns to Subscriber class fields
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colSubId.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colQrCode.setCellValueFactory(new PropertyValueFactory<>("qrCode"));
    }

    /**
     * Populates the TableView with data received from the server.
     * Converts the standard ArrayList into an ObservableList for JavaFX rendering.
     * @param list An ArrayList containing Subscriber objects to be displayed.
     * @return None.
     */
    public void setTableData(ArrayList<Subscriber> list) {
        tableSubscribers.setItems(FXCollections.observableArrayList(list));
    }
}