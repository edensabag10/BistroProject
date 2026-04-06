package managmentGUI;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import common.Visit;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller class for the "Current Diners" management view in the Bistro system.
 * This class handles the display of active visits (diners currently in the restaurant)
 * by mapping Visit data objects to a JavaFX TableView.
 */
public class CurrentDinersController implements Initializable {

    /** Table component for displaying current diners. */
    @FXML private TableView<Visit> dinersTable;
    
    /** Column for the reservation confirmation code. */
    @FXML private TableColumn<Visit, String> colCode;
    
    /** Column for the assigned table ID. */
    @FXML private TableColumn<Visit, Integer> colTable;
    
    /** Column for the visit start time. */
    @FXML private TableColumn<Visit, String> colTime;

    /**
     * Initializes the TableView columns by linking them to the properties of the Visit class.
     * @param location  The location used to resolve relative paths for the root object.
     * @param resources The resources used to localize the root object.
     * @return Void.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colTable.setCellValueFactory(new PropertyValueFactory<>("tableId"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("startTime"));
    }
    
    /**
     * Populates the table with a list of active visits provided by the management dashboard.
     * @param visitsList An ArrayList of Visit objects representing current diners.
     * @return Void.
     */
    public void setTableData(ArrayList<Visit> visitsList) {
        if (dinersTable != null && visitsList != null) {
            dinersTable.setItems(FXCollections.observableArrayList(visitsList));
        }
    }
}