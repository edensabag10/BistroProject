package managmentGUI;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;

/**
 * Controller class for visualizing Time and Stay Duration reports.
 * This class manages a complex dashboard containing a BarChart for average delays 
 * and stay durations, alongside PieCharts for analyzing arrival and departure peak hours.
 */
public class TimeReportGraphController {

    /** Bar chart component for comparing delays and durations across days. */
    @FXML private BarChart<String, Number> timeBarChart;

    /** Horizontal axis representing the days of the month. */
    @FXML private CategoryAxis xAxis;

    /** Vertical axis representing numeric time values (minutes/hours). */
    @FXML private NumberAxis yAxis;

    /** Pie chart component visualizing the distribution of arrival hours. */
    @FXML private PieChart arrivalPieChart;

    /** Pie chart component visualizing the distribution of departure hours. */
    @FXML private PieChart departurePieChart;

    /**
     * Processes raw report data and populates the multi-chart visualization dashboard.
     * Iterates through the data to build statistical series for the bar chart 
     * and frequency maps for the pie charts.
     * * @param reportData A list of maps containing keys for "date", "delay", "duration", 
     * "arr_hour", and "dep_hour".
     * @return None.
     */
    
    public void initData(List<Map<String, Object>> reportData) {
        // 1. Initialize Bar Chart series for delays and stay duration
        XYChart.Series<String, Number> delaySeries = new XYChart.Series<>();
        delaySeries.setName("Avg Delay");
        XYChart.Series<String, Number> durationSeries = new XYChart.Series<>();
        durationSeries.setName("Avg Stay Duration");

        // 2. Prepare frequency maps for arrival and departure distributions
        Map<Integer, Integer> arrivalCounts = new HashMap<>();
        Map<Integer, Integer> departureCounts = new HashMap<>();

        for (Map<String, Object> entry : reportData) {
            // Process bar chart daily data
            String day = entry.get("date").toString();
            // Extract only the day part from the date string
            day = day.substring(day.lastIndexOf("-") + 1);
            delaySeries.getData().add(new XYChart.Data<>(day, (Number) entry.get("delay")));
            durationSeries.getData().add(new XYChart.Data<>(day, (Number) entry.get("duration")));

            // Process pie chart hourly frequency data
            int arrH = (int) entry.get("arr_hour");
            int depH = (int) entry.get("dep_hour");
            arrivalCounts.put(arrH, arrivalCounts.getOrDefault(arrH, 0) + 1);
            departureCounts.put(depH, departureCounts.getOrDefault(depH, 0) + 1);
        }

        // Render data to the Bar Chart
        timeBarChart.getData().clear();
        timeBarChart.getData().addAll(delaySeries, durationSeries);

        // Render data to the Arrival and Departure Pie Charts
        fillPieChart(arrivalPieChart, arrivalCounts);
        fillPieChart(departurePieChart, departureCounts);
    }

    /**
     * Internal helper method to populate a PieChart from a frequency map.
     * Maps hours to chart data slices.
     * * @param chart  The PieChart component to be populated.
     * @param counts A map containing the frequency of events per hour.
     * @return None.
     */
    private void fillPieChart(PieChart chart, Map<Integer, Integer> counts) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        counts.forEach((hour, count) -> {
            // Format hour as "HH:00" for slice labels
            pieData.add(new PieChart.Data(hour + ":00", count));
        });
        chart.setData(pieData);
    }
}