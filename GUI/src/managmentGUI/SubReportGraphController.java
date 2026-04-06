package managmentGUI;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import java.util.*;

/**
 * Controller class for visualizing Subscriber Activity reports.
 * This class handles the population of a BarChart to compare the number of 
 * successful reservations against waiting list entries over a specific period.
 */
public class SubReportGraphController {

    /** The BarChart component for displaying subscriber data. */
    @FXML private BarChart<String, Number> subBarChart;

    /** The horizontal axis representing the days of the month. */
    @FXML private CategoryAxis xAxis;

    /**
     * Processes raw report data and initializes the chart visualization.
     * Iterates through the provided data maps, extracts date components, 
     * and constructs two distinct data series for comparison.
     * * @param reportData A list of maps where each map contains "date", 
     * "reservations", and "waiting" count keys.
     * @return None.
     */
    
    public void initData(List<Map<String, Object>> reportData) {
        // Create a series to represent completed reservations
        XYChart.Series<String, Number> resSeries = new XYChart.Series<>();
        resSeries.setName("Reservations");

        // Create a series to represent waiting list entries
        XYChart.Series<String, Number> waitSeries = new XYChart.Series<>();
        waitSeries.setName("Waiting List");

        // Populate series by iterating through the report results
        for (Map<String, Object> entry : reportData) {
            String fullDate = entry.get("date").toString();
            
            // Extract the day portion from the date string (yyyy-MM-dd)
            String dayOnly = fullDate.substring(fullDate.lastIndexOf("-") + 1);

            // Add data points to respective series
            resSeries.getData().add(new XYChart.Data<>(dayOnly, (Number) entry.get("reservations")));
            waitSeries.getData().add(new XYChart.Data<>(dayOnly, (Number) entry.get("waiting")));
        }

        // Clear existing chart state and render the new datasets
        subBarChart.getData().clear();
        subBarChart.getData().addAll(resSeries, waitSeries);
    }
}