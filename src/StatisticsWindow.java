import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
// import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.Map;
import java.util.List;

/**
 * Statistics window displaying real-time traffic analysis charts.
 * Shows two graphs (Average Speed, Vehicle Count) and a Traffic Stress Level indicator.
 */
public class StatisticsWindow extends Stage {
    
    private SimulationRunner runner;
    private TrafficManager trafficManager;
    
    // Chart 1: Average Speed
    private LineChart<Number, Number> speedChart;
    private XYChart.Series<Number, Number> speedSeries;
    
    // Chart 2: Vehicle Count by Type
    private AreaChart<Number, Number> vehicleCountChart;
    private XYChart.Series<Number, Number> carSeries;
    private XYChart.Series<Number, Number> truckSeries;
    private XYChart.Series<Number, Number> busSeries;
    private XYChart.Series<Number, Number> motoSeries;
    private XYChart.Series<Number, Number> emergencySeries;
    
    // Chart 3: Travel Time Distribution
    private BarChart<String, Number> travelTimeChart;
    private XYChart.Series<String, Number> travelTimeSeries;

    // Chart 4: Distance Travelled Distribution
    private BarChart<String, Number> distanceTravelChart;
    private XYChart.Series<String, Number> distanceTravelSeries;
    
    // Stress Level Display
    private Label stressLevelLabel;
    
    private Timeline updateTimeline;
    private int maxDataPoints = 240;  // Show last 120 seconds (at 2 updates/sec)
    
    /**
     * Creates a new statistics window.
     */
    public StatisticsWindow(SimulationRunner runner, TrafficManager trafficManager) {
        this.runner = runner;
        this.trafficManager = trafficManager;
        
        setTitle("Live Traffic Statistics");
        createLayout();
        startUpdates();
    }
    
    private void createLayout() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #F5F5F5;");
        
        // Create the charts and stress label
        speedChart = createSpeedChart();
        vehicleCountChart = createVehicleCountChart();
        travelTimeChart = createTravelTimeChart();
        distanceTravelChart = createDistanceTravelChart();
        stressLevelLabel = createStressLevelLabel();
        
        root.getChildren().addAll(speedChart, vehicleCountChart, travelTimeChart, distanceTravelChart, stressLevelLabel);
        
        // Wrap in ScrollPane for scrollability
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #F5F5F5;");
        
        Scene scene = new Scene(scrollPane, 900, 800);
        setScene(scene);
    }
    
    private LineChart<Number, Number> createSpeedChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time (s)");
        yAxis.setLabel("Speed (m/s)");
        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(true);
        
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Average Speed Over Time");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setPrefHeight(220);
        chart.setStyle(".chart-legend-item-symbol { -fx-background-radius: 0; -fx-border-width: 0; -fx-padding: 5px; }");
        
        speedSeries = new XYChart.Series<>();
        speedSeries.setName("Average Speed");
        chart.getData().add(speedSeries);
        
        return chart;
    }
    
    private AreaChart<Number, Number> createVehicleCountChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time (s)");
        yAxis.setLabel("Vehicle Count");
        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(true);
        
        AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setTitle("Vehicle Count by Type");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setPrefHeight(220);
        chart.setStyle(".chart-legend-item-symbol { -fx-background-radius: 0; -fx-border-width: 0; -fx-padding: 5px; } .chart-legend-item { -fx-padding: 5px 15px; }");
        
        // Create series for each vehicle type
        carSeries = new XYChart.Series<>();
        carSeries.setName("Cars");
        truckSeries = new XYChart.Series<>();
        truckSeries.setName("Trucks");
        busSeries = new XYChart.Series<>();
        busSeries.setName("Buses");
        motoSeries = new XYChart.Series<>();
        motoSeries.setName("Motorcycles");
        emergencySeries = new XYChart.Series<>();
        emergencySeries.setName("Emergency");
        
        chart.getData().add(carSeries);
        chart.getData().add(emergencySeries);
        chart.getData().add(busSeries);
        chart.getData().add(motoSeries);
        chart.getData().add(truckSeries);
        
        return chart;
    }
    
    private BarChart<String, Number> createTravelTimeChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Travel Time (seconds)");
        yAxis.setLabel("Number of Vehicles");
        
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Travel Time Distribution");
        chart.setAnimated(false);
        chart.setPrefHeight(250);
        chart.setLegendVisible(false);
        chart.setStyle(".chart-bar { -fx-bar-fill: #3498DB; }");
        
        travelTimeSeries = new XYChart.Series<>();
        travelTimeSeries.setName("Travel Times");
        
        // Initialize bins (0-30s, 30-60s, 60-90s, 90-120s, 120-150s, 150+s)
        travelTimeSeries.getData().add(new XYChart.Data<>("0-30", 0));
        travelTimeSeries.getData().add(new XYChart.Data<>("30-60", 0));
        travelTimeSeries.getData().add(new XYChart.Data<>("60-90", 0));
        travelTimeSeries.getData().add(new XYChart.Data<>("90-120", 0));
        travelTimeSeries.getData().add(new XYChart.Data<>("120-150", 0));
        travelTimeSeries.getData().add(new XYChart.Data<>("150+", 0));
        
        chart.getData().add(travelTimeSeries);
        
        return chart;
    }

    private BarChart<String, Number> createDistanceTravelChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Distance Traveled (meters)");
        yAxis.setLabel("Number of Vehicles");
        
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Distance Traveled Distribution");
        chart.setAnimated(false);
        chart.setPrefHeight(250);
        chart.setLegendVisible(false);
        chart.setStyle(".chart-bar { -fx-bar-fill: #E67E22; }");
        
        distanceTravelSeries = new XYChart.Series<>();
        distanceTravelSeries.setName("Distance Traveled");
        
        // Initialize bins with more realistic distances (0-200m, 200-400m, 400-600m, 600-800m, 800-1000m, 1000m+)
        distanceTravelSeries.getData().add(new XYChart.Data<>("0-200", 0));
        distanceTravelSeries.getData().add(new XYChart.Data<>("200-400", 0));
        distanceTravelSeries.getData().add(new XYChart.Data<>("400-600", 0));
        distanceTravelSeries.getData().add(new XYChart.Data<>("600-800", 0));
        distanceTravelSeries.getData().add(new XYChart.Data<>("800-1000", 0));
        distanceTravelSeries.getData().add(new XYChart.Data<>("1000+", 0));
        
        chart.getData().add(distanceTravelSeries);
        
        return chart;
    }
    
    private Label createStressLevelLabel() {
        Label label = new Label("Current Stress Level: 0");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2C3E50; -fx-padding: 20px; -fx-background-color: white; -fx-border-color: #BDC3C7; -fx-border-width: 2px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        label.setPrefHeight(100);
        return label;
    }
    
    private void startUpdates() {
        updateTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> updateCharts()));
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
        updateTimeline.play();
    }
    
    private void updateCharts() {
        double simTime = runner.getSimulationTime();
        Map<String, Double> speeds = runner.getVehicleSpeeds();
        
        // Update Chart 1: Average Speed
        double avgSpeed = speeds.isEmpty() ? 0.0 : 
            speeds.values().stream().mapToDouble(Double::doubleValue).sum() / speeds.size();
        
        speedSeries.getData().add(new XYChart.Data<>(simTime, avgSpeed));
        if (speedSeries.getData().size() > maxDataPoints) {
            speedSeries.getData().remove(0);
        }
        
        // Update x-axis sliding window for speed chart
        NumberAxis speedXAxis = (NumberAxis) speedChart.getXAxis();
        double windowSize = 120; // Show last 120 seconds
        double lowerBound = Math.max(0, simTime - windowSize);
        double upperBound = lowerBound + windowSize;
        speedXAxis.setLowerBound(lowerBound);
        speedXAxis.setUpperBound(upperBound);
        speedXAxis.setTickUnit(30);
        
        
        // Update Chart 2: Vehicle Counts
        int cars = 0, trucks = 0, buses = 0, motos = 0, emergency = 0;
        for (String id : speeds.keySet()) {
            if (id.startsWith("car")) cars++;
            else if (id.startsWith("truck")) trucks++;
            else if (id.startsWith("bus")) buses++;
            else if (id.startsWith("moto")) motos++;
            else if (id.startsWith("ambu")) emergency++;
        }
        
        carSeries.getData().add(new XYChart.Data<>(simTime, cars));
        truckSeries.getData().add(new XYChart.Data<>(simTime, trucks));
        busSeries.getData().add(new XYChart.Data<>(simTime, buses));
        motoSeries.getData().add(new XYChart.Data<>(simTime, motos));
        emergencySeries.getData().add(new XYChart.Data<>(simTime, emergency));
        
        // Limit data points
        if (carSeries.getData().size() > maxDataPoints) {
            carSeries.getData().remove(0);
            truckSeries.getData().remove(0);
            busSeries.getData().remove(0);
            motoSeries.getData().remove(0);
            emergencySeries.getData().remove(0);
        }
        
        // Update x-axis sliding window for vehicle count chart
        NumberAxis vehicleXAxis = (NumberAxis) vehicleCountChart.getXAxis();
        windowSize = 120; // Show last 120 seconds
        lowerBound = Math.max(0, simTime - windowSize);
        upperBound = lowerBound + windowSize;
        vehicleXAxis.setLowerBound(lowerBound);
        vehicleXAxis.setUpperBound(upperBound);
        vehicleXAxis.setTickUnit(30);
        
        // Update Chart 3: Travel Time Distribution
        updateTravelTimeDistribution();
        
        // Update Chart 4: Distance Travelled Distribution
        updateDistanceTravelDistribution();

        // Update Stress Level
        updateStressLevel();
    }
    
    private void updateStressLevel() {
        int stressLevel = calculateCurrentStress();
        stressLevelLabel.setText("Current Stress Level: " + stressLevel);
        
        // Change color based on stress level
        String color;
        if (stressLevel < 30) {
            color = "#27AE60"; // Green
        } else if (stressLevel < 60) {
            color = "#F39C12"; // Orange
        } else {
            color = "#E74C3C"; // Red
        }
        stressLevelLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-padding: 20px; -fx-background-color: white; -fx-border-color: #BDC3C7; -fx-border-width: 2px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
    }
    
    private int calculateCurrentStress() {
        Map<String, Double> speeds = runner.getVehicleSpeeds();
        
        int totalVehicles = speeds.size();
        double avgSpeed = speeds.isEmpty() ? 0.0 : 
            speeds.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        // Stress level: 0-100 based on vehicle count and speeds
        // Base stress from vehicle count (0-50 points)
        int vehicleStress = Math.min(50, (int)(totalVehicles * 1.25));
        
        // Speed stress (0-50 points) - lower speed = higher stress
        int speedStress = 0;
        if (avgSpeed < 8.0) {
            speedStress = (int)((8.0 - avgSpeed) / 8.0 * 50);
        }
        
        int stressLevel = Math.min(100, vehicleStress + speedStress);
        return stressLevel;
    }
    
    private void updateTravelTimeDistribution() {
        List<Double> travelTimes = trafficManager.getCompletedTravelTimes();
        
        // Count vehicles in each time bin
        int[] bins = new int[6]; // 0-30, 30-60, 60-90, 90-120, 120-150, 150+
        
        for (Double time : travelTimes) {
            if (time < 30) {
                bins[0]++;
            } else if (time < 60) {
                bins[1]++;
            } else if (time < 90) {
                bins[2]++;
            } else if (time < 120) {
                bins[3]++;
            } else if (time < 150) {
                bins[4]++;
            } else {
                bins[5]++;
            }
        }
        
        // Update chart data
        travelTimeSeries.getData().get(0).setYValue(bins[0]);
        travelTimeSeries.getData().get(1).setYValue(bins[1]);
        travelTimeSeries.getData().get(2).setYValue(bins[2]);
        travelTimeSeries.getData().get(3).setYValue(bins[3]);
        travelTimeSeries.getData().get(4).setYValue(bins[4]);
        travelTimeSeries.getData().get(5).setYValue(bins[5]);
    }
    
    private void updateDistanceTravelDistribution() {
        List<Double> distances = trafficManager.getCompletedTravelDistances();
        
        // Count vehicles in each distance bin (0-200, 200-400, 400-600, 600-800, 800-1000, 1000+)
        int[] bins = new int[6];
        
        for (Double distance : distances) {
            if (distance < 200) {
                bins[0]++;
            } else if (distance < 400) {
                bins[1]++;
            } else if (distance < 600) {
                bins[2]++;
            } else if (distance < 800) {
                bins[3]++;
            } else if (distance < 1000) {
                bins[4]++;
            } else {
                bins[5]++;
            }
        }
        
        // Update chart data
        distanceTravelSeries.getData().get(0).setYValue(bins[0]);
        distanceTravelSeries.getData().get(1).setYValue(bins[1]);
        distanceTravelSeries.getData().get(2).setYValue(bins[2]);
        distanceTravelSeries.getData().get(3).setYValue(bins[3]);
        distanceTravelSeries.getData().get(4).setYValue(bins[4]);
        distanceTravelSeries.getData().get(5).setYValue(bins[5]);
    }
    
}