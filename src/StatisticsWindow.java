import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
// import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.Map;

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
        stressLevelLabel = createStressLevelLabel();
        
        root.getChildren().addAll(speedChart, vehicleCountChart, stressLevelLabel);
        
        Scene scene = new Scene(root, 900, 800);
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
    
    
}