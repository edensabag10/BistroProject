package managmentGUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import common.ServiceResponse;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

/**
 * Controller for the Manager Dashboard in the Bistro system.
 * Inherits core functionality from RepresentativeDashboardController and adds 
 * administrative features such as complex reporting and graphical data analysis.
 */
public class ManagerDashboardController extends RepresentativeDashboardController {

    /**
     * Triggered when the client is fully initialized. Enables manager-specific tools.
     * @return None.
     */
    @Override
    public void onClientReady() {
        super.onClientReady();
        appendLog("Manager Mode Active: Additional reporting tools enabled.");
    }
    
    /**
     * Opens the month selection window for generating management reports.
     * @param event The ActionEvent triggered by the report button.
     * @return None.
     */
    @FXML
    public void openMonthSelection(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/managmentGUI/ActionsFXML/monthSelection.fxml")
                );
            Parent root = loader.load();
            
            MonthSelectionController ctrl = loader.getController();
            ctrl.setClient(this.client); 
            
            Stage stage = new Stage();
            stage.setTitle("Month Selection");
            stage.setScene(new Scene(root));
            stage.show();
            
        } catch (Exception e) {
            System.out.println("Failed to open month screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Processes incoming server messages. Specifically handles report data results 
     * while delegating standard visit data to the parent controller.
     * @param message The response object from the server.
     * @return None.
     */
    @Override
    public void display(Object message) {
        // Initial check: Verify if the message is a structured response list
        if (message instanceof ArrayList) {
            ArrayList<Object> responseList = (ArrayList<Object>) message;

            // Validate that the list is not empty and begins with a String command header
            if (!responseList.isEmpty() && responseList.get(0) instanceof String) {
                String header = (String) responseList.get(0);

                // 1. Handling Time and Delay report data
                if ("REPORT_TIME_DATA_SUCCESS".equals(header)) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> data = (List<Map<String, Object>>) responseList.get(1);
                    Platform.runLater(() -> {
                        if (data == null || data.isEmpty()) {
                            showErrorAlert("No Data Found", "No time and delay records were found for the selected month.");
                        } else {
                            appendLog("Time Report Data Received Successfully.");
                            showGraph(data);
                        }
                    });
                    return; 
                } 
                
                // 2. Handling Subscriber and Waiting List activity reports
                else if ("RECEIVE_SUBSCRIBER_REPORTS".equals(header)) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> subData = (List<Map<String, Object>>) responseList.get(1);
                    Platform.runLater(() -> {
                        if (subData == null || subData.isEmpty()) {
                            showErrorAlert("No Data Found", "No subscriber activity found for the selected month.");
                        } else {
                            appendLog("Subscriber Report Data Received Successfully.");
                            showSubGraph(subData);
                        }
                    });
                    return; 
                }
                
                // 3. Handling specific error messages returned by the reporting engine
                else if ("REPORT_ERROR".equals(header)) {
                    String errorMsg = (String) responseList.get(1);
                    Platform.runLater(() -> {
                        showErrorAlert("System Error", "Error: " + errorMsg);
                    });
                    return; 
                }
            }
        }

        /**
         * If the message is not a report header, forward it to the 
         * RepresentativeDashboardController for standard TableView handling.
         */
        super.display(message); 
    }
    
    /**
     * Initializes and displays the Time and Delay statistical graph.
     * @param reportData A list containing the mapped time statistics.
     * @return None.
     */
    public void showGraph(List<Map<String, Object>> reportData) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/managmentGUI/ActionsFXML/TimeReportGraph.fxml"));
                Parent root = loader.load();
                TimeReportGraphController graphCtrl = loader.getController();
                graphCtrl.initData(reportData);
                
                Stage stage = new Stage();
                stage.setTitle("Time & Delays Report");
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Initializes and displays the Subscriber Activity statistical graph.
     * @param reportData A list containing subscriber and waiting list records.
     * @return None.
     */
    public void showSubGraph(List<Map<String, Object>> reportData) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/managmentGUI/ActionsFXML/SubscriberReportGraph.fxml"));
                Parent root = loader.load();
                
                SubReportGraphController subGraphCtrl = loader.getController();
                subGraphCtrl.initData(reportData);
                
                Stage stage = new Stage();
                stage.setTitle("Subscriber Activity Report");
                stage.setScene(new Scene(root));
                stage.show();
                
            } catch (IOException e) {
                appendLog("Error loading Subscriber Graph FXML.");
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Internal helper to display standardized information popups.
     * @param title   The alert window title.
     * @param content The alert body message.
     * @return None.
     */
    private void showErrorAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}