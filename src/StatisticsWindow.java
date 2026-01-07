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
    private BarChart<String, Number> vehicleCountChart;
    private XYChart.Series<String, Number> vehicleCountSeries;
    
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
        VBox root = new VBox(0);
        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: white;");
        
        // Create the charts and stress label
        speedChart = createSpeedChart();
        vehicleCountChart = createVehicleCountChart();
        stressLevelLabel = createStressLevelLabel();
        
        // Make charts grow to fill available space
        VBox.setVgrow(speedChart, javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(vehicleCountChart, javafx.scene.layout.Priority.ALWAYS);
        
        root.getChildren().addAll(speedChart, vehicleCountChart, stressLevelLabel);
        
        Scene scene = new Scene(root, 1200, 900);
        setScene(scene);
    }
    
    private LineChart<Number, Number> createSpeedChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time (s)");
        yAxis.setLabel("Speed (m/s)");
        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(true);
        
        // Bold black axis styling
        xAxis.setStyle("-fx-tick-label-font-size: 12px; -fx-font-weight: bold; -fx-tick-label-fill: black;");
        yAxis.setStyle("-fx-tick-label-font-size: 12px; -fx-font-weight: bold; -fx-tick-label-fill: black;");
        
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Average Speed Over Time");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        
        chart.setStyle(
            "-fx-background-color: white;" +
            "-fx-padding: 20px;"
        );
        
        // Apply additional CSS styling
        chart.lookup(".chart-title").setStyle(
            "-fx-font-size: 20px;" +
            "-fx-font-weight: 900;" +
            "-fx-font-family: 'Arial';" +
            "-fx-text-fill: black;"
        );
        
        chart.getStylesheets().add("data:text/css," +
            ".chart-plot-background { -fx-background-color: white; }" +
            ".chart-vertical-grid-lines { -fx-stroke: #E0E0E0; }" +
            ".chart-horizontal-grid-lines { -fx-stroke: #E0E0E0; }" +
            ".chart-vertical-zero-line { -fx-stroke: black; -fx-stroke-width: 1.5px; }" +
            ".chart-horizontal-zero-line { -fx-stroke: black; -fx-stroke-width: 1.5px; }" +
            ".axis { -fx-stroke: black; -fx-stroke-width: 2px; }" +
            ".axis-label { -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: Arial; -fx-text-fill: black; }" +
            ".chart-series-line { -fx-stroke: #E67E22; -fx-stroke-width: 3px; }"
        );
        
        speedSeries = new XYChart.Series<>();
        speedSeries.setName("Average Speed");
        chart.getData().add(speedSeries);
        
        return chart;
    }
    private BarChart<String, Number> createVehicleCountChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Vehicle Type");
        yAxis.setLabel("Vehicle Count");
        yAxis.setAutoRanging(false);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickUnit(10);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(50);
        
        // Bold black axis styling
        xAxis.setStyle("-fx-tick-label-font-size: 12px; -fx-font-weight: bold; -fx-tick-label-fill: black;");
        yAxis.setStyle("-fx-tick-label-font-size: 12px; -fx-font-weight: bold; -fx-tick-label-fill: black;");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Current Vehicle Count by Type");
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCategoryGap(30);
        chart.setBarGap(5);

        //Create single series with all vehicle types
        vehicleCountSeries = new XYChart.Series<>();
        vehicleCountSeries.getData().add(new XYChart.Data<>("Cars", 0));
        vehicleCountSeries.getData().add(new XYChart.Data<>("Trucks", 0));
        vehicleCountSeries.getData().add(new XYChart.Data<>("Buses", 0));
        vehicleCountSeries.getData().add(new XYChart.Data<>("Motorcycles", 0));
        vehicleCountSeries.getData().add(new XYChart.Data<>("Emergency", 0));

        chart.getData().add(vehicleCountSeries);

        chart.setStyle(
            "-fx-background-color: white;" +
            "-fx-padding: 20px;"
        );
        
        // Apply additional CSS styling
        chart.lookup(".chart-title").setStyle(
            "-fx-font-size: 20px;" +
            "-fx-font-weight: 900;" +
            "-fx-font-family: 'Arial';" +
            "-fx-text-fill: black;"
        );
        
        chart.getStylesheets().add("data:text/css," +
            ".chart-bar { -fx-bar-fill: #3498DB; }" +
            ".chart-plot-background { -fx-background-color: white; }" +
            ".chart-vertical-grid-lines { -fx-stroke: #E0E0E0; }" +
            ".chart-horizontal-grid-lines { -fx-stroke: #E0E0E0; }" +
            ".axis { -fx-stroke: black; -fx-stroke-width: 2px; }" +
            ".axis-label { -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: Arial; -fx-text-fill: black; }"
        );

        return chart;

    }
    
    private Label createStressLevelLabel() {
        Label label = new Label("Current Stress Level: 0");
        label.setStyle(
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Arial';" +
            "-fx-text-fill: black;" +
            "-fx-padding: 20px;" +
            "-fx-background-color: white;"
        );
        label.setPrefHeight(80);
        label.setAlignment(javafx.geometry.Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
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
        double avgSpeed = 0;
        if (speeds.isEmpty()) {
            avgSpeed = 0.0;
        } else {
            double sum = 0.0;
            for (Double speed : speeds.values()) {
                sum += speed.doubleValue();
            }
            avgSpeed = sum / speeds.size();
        }
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
        Map<String, Integer> counts = runner.getVehicleCountsByType();
        int cars = counts.getOrDefault("car", 0);
        int trucks = counts.getOrDefault("truck", 0);
        int buses = counts.getOrDefault("bus", 0);
        int motos = counts.getOrDefault("moto", 0);
        int emergency = counts.getOrDefault("emergency", 0);
        
        vehicleCountSeries.getData().get(0).setYValue(cars);
        vehicleCountSeries.getData().get(1).setYValue(trucks);
        vehicleCountSeries.getData().get(2).setYValue(buses);
        vehicleCountSeries.getData().get(3).setYValue(motos);
        vehicleCountSeries.getData().get(4).setYValue(emergency);
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
        stressLevelLabel.setStyle(
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Arial';" +
            "-fx-text-fill: " + color + ";" +
            "-fx-padding: 20px;" +
            "-fx-background-color: white;"
        );
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
    
    // /**
    //  * Pauses the automatic chart updates.
    //  */
    // public void pauseUpdates() {
    //     if (updateTimeline != null) {
    //         updateTimeline.pause();
    //     }
    // }
    
    // /**
    //  * Resumes the automatic chart updates.
    //  */
    // public void resumeUpdates() {
    //     if (updateTimeline != null) {
    //         updateTimeline.play();
    //     }
    // }
}