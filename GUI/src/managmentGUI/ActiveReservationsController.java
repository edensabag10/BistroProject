package managmentGUI;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleObjectProperty;
import java.util.ArrayList;

/**
 * Controller class for the Active Reservations management view.
 * This class handles the display of current restaurant reservations for staff members,
 * utilizing a TableView that maps raw Object arrays to descriptive columns.
 */
public class ActiveReservationsController {

    /** The table component for displaying active reservations. */
    @FXML private TableView<Object[]> activeReservationsTable;

    /** Individual columns for reservation attributes. */
    @FXML private TableColumn<Object[], Object> colCode, colDate, colGuests, colPhone, colStatus;

    /**
     * Initializes the TableView columns by mapping specific array indices to table cells.
     * The mapping follows the SQL query results order:
     * 0: Confirmation Code, 1: Date/Time, 2: Number of Guests, 3: Phone, 4: Status.
     * @return None.
     */
    public void initialize() {
        colCode.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()[0]));
        colDate.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()[1]));
        colGuests.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()[2]));
        colPhone.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()[3]));
        colStatus.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()[4]));
    }

    /**
     * Updates the TableView content with provided reservation data.
     * @param data An ArrayList of Object arrays representing reservation records from the DB.
     * @return None.
     */
    public void setTableData(ArrayList<Object[]> data) {
        activeReservationsTable.setItems(FXCollections.observableArrayList(data));
    }
}