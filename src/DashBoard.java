import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Real-time dashboard displaying simulation metrics and traffic statistics.
 * Shows simulation time, active vehicle count, average speed, vehicle type breakdown,
 * and a live chart of average speed over the last 60 seconds.
 *
 * @author M A T^2 H Team
 * @version 2.0 
 * @see ControlPanel
 * @see TrafficSimulatorApp
 */
public class DashBoard extends VBox {
    // Metric labels
    private Label simTimeLabel;
    private Label activeVehiclesLabel;
    private Label avgSpeedLabel;

    // Vehicle type breakdown
    private Label carsLabel;
    private Label trucksLabel;
    private Label busesLabel;
    private Label motorcyclesLabel;
    private Label emergencyLabel;

    // Real-time chart
    private LineChart<Number, Number> speedChart;
    private XYChart.Series<Number, Number> speedSeries;
    private int maxDataPoints = 120; // 120 points at 2 updates/sec = 60 seconds

    /**
     * Constructs a new dashboard with all metric labels and chart initialized.
     */
    public DashBoard() {
        super(12);
        setPadding(new Insets(15, 10, 0, 10));
        setStyle("-fx-background-color: #0D1B2A;");
        setAlignment(Pos.CENTER);
        setMaxWidth(280);

        initializeComponents();
    }

    /**
     * Initializes all dashboard UI components including labels, vehicle type breakdown, and speed chart.
     */
    private void initializeComponents() {
        Label title = createHeaderLabel("―――DASHBOARD―――");
        Label simSection = createSectionLabel("Simulation");
        simTimeLabel = createDataLabel("Time: 0.0s");
        activeVehiclesLabel = createDataLabel("Active: 0");

        Label trafficSection = createSectionLabel("Traffic Stats");
        avgSpeedLabel = createDataLabel("Avg Speed: 0.0 m/s");

        Label vehicleSection = createSectionLabel("Vehicle Types");
        carsLabel = createDataLabel("Cars: 0");
        trucksLabel = createDataLabel("Trucks: 0");
        busesLabel = createDataLabel("Buses: 0");
        motorcyclesLabel = createDataLabel("Motorcycles: 0");
        emergencyLabel = createDataLabel("Emergency: 0");

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time (s)");
        yAxis.setLabel("Speed (m/s)");
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(60);
        xAxis.setTickUnit(15);
        yAxis.setAutoRanging(true);

        speedChart = new LineChart<>(xAxis, yAxis);
        speedChart.setTitle("Average Speed");
        speedChart.setLegendVisible(false);
        speedChart.setPrefHeight(180);
        speedChart.setMaxWidth(280);
        speedChart.setCreateSymbols(false);
        speedChart.setAnimated(false);
        speedChart.setStyle("-fx-background-color: #0D1B2A; -fx-title-side: top; -fx-text-fill: #778DA9;-fx-font-weight: bold; -fx-font-size: 10;");

        speedSeries = new XYChart.Series<>();
        speedChart.getData().add(speedSeries);

        getChildren().addAll(title,
                simSection, simTimeLabel, activeVehiclesLabel,
                trafficSection, avgSpeedLabel,
                vehicleSection, carsLabel, trucksLabel, busesLabel, motorcyclesLabel, emergencyLabel,
                speedChart);
    }

    /**
     * Creates a styled header label for the dashboard.
     * 
     * @param text The header text
     * @return A formatted header label
     */
    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        label.setTextFill(Color.web("white"));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    /**
     * Creates a styled section label for grouping dashboard metrics.
     * 
     * @param text The section name
     * @return A formatted section label
     */
    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        label.setTextFill(Color.web("white"));
        label.setPadding(new Insets(10, 0, 5, 0));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    /**
     * Creates a styled data label for displaying metric values.
     * 
     * @param text The initial data text
     * @return A formatted data label
     */
    private Label createDataLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Monospace", 12));
        label.setTextFill(Color.web("#778DA9"));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    /**
     * Updates all dashboard metrics with new simulation data.
     * Refreshes labels, adds data point to chart, and maintains sliding window.
     * 
     * @param data The current dashboard data
     */
    public void update(DashBoardData data) {
        simTimeLabel.setText(String.format("Time: %.1fs", data.simTime));
        activeVehiclesLabel.setText("Active: " + data.activeVehicles);
        avgSpeedLabel.setText(String.format("Avg Speed: %.2f m/s", data.avgSpeed));

        carsLabel.setText("Cars: " + data.carCount);
        trucksLabel.setText("Trucks: " + data.truckCount);
        busesLabel.setText("Buses: " + data.busCount);
        motorcyclesLabel.setText("Motorcycles: " + data.motorcycleCount);
        emergencyLabel.setText("Emergency: " + data.emergencyCount);

        speedSeries.getData().add(new XYChart.Data<>(data.simTime, data.avgSpeed));
        if (speedSeries.getData().size() > maxDataPoints) {
            speedSeries.getData().remove(0);
        }

        // Update x-axis bounds to create a sliding window effect
        NumberAxis xAxis = (NumberAxis) speedChart.getXAxis();
        if (data.simTime > 60) {
            xAxis.setLowerBound(data.simTime - 60);
            xAxis.setUpperBound(data.simTime);
        }
    }

    /**
     * Data container for dashboard metrics.
     * Holds simulation time, vehicle counts by type, and average speed.
     */
    public static class DashBoardData {
        /** Simulation time in seconds */
        public double simTime;
        /** Total number of active vehicles */
        public int activeVehicles;
        /** Average speed of all vehicles in m/s */
        public double avgSpeed;
        /** Number of cars */
        public int carCount;
        /** Number of trucks */
        public int truckCount;
        /** Number of buses */
        public int busCount;
        /** Number of motorcycles */
        public int motorcycleCount;
        /** Number of emergency vehicles */
        public int emergencyCount;
    }
}
