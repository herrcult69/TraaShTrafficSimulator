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

public class DashBoard extends VBox{
    // Metric labels
    private Label simTimeLabel;
    private Label totalVehiclesLabel;
    private Label activeVehiclesLabel;
    private Label avgSpeedLabel;
    private Label stoppedVehiclesLabel;
    private Label fpsLabel;

    // Vehicle type breakdown
    private Label carsLabel;
    private Label trucksLabel;
    private Label busesLabel;
    private Label motorcyclesLabel;

    // Real-time chart
    private LineChart<Number, Number> speedChart;
    private XYChart.Series<Number, Number> speedSeries;
    private int maxDataPoints = 600;

    public DashBoard(){
        super(12);
        setPadding(new Insets(15, 10, 0, 10));
        setStyle("-fx-background-color: #2b2b2b;");
        setAlignment(Pos.CENTER);
        setMaxWidth(280);

        initializeComponents();
    }

    private void initializeComponents() {
        Label title = createHeaderLabel("═══ DASHBOARD ═══");

        Label simSection = createSectionLabel("Simulation");
        simTimeLabel = createDataLabel("Time: 0.0s");
        totalVehiclesLabel = createDataLabel("Total: 0");
        activeVehiclesLabel = createDataLabel("Active: 0");

        Label trafficSection = createSectionLabel("Traffic Stats");
        avgSpeedLabel = createDataLabel("Avg Speed: 0.0 m/s");
        stoppedVehiclesLabel = createDataLabel("Stopped: 0");

        Label vehicleSection = createSectionLabel("Vehicle Types");
        carsLabel = createDataLabel("Cars: 0");
        trucksLabel = createDataLabel("Trucks: 0");
        busesLabel = createDataLabel("Buses: 0");
        motorcyclesLabel = createDataLabel("Motorcycles: 0");

        Label systemSection = createSectionLabel("System");
        fpsLabel = createDataLabel("FPS: 0");

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time (s)");
        yAxis.setLabel("Speed (m/s)");
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(60);
        xAxis.setTickUnit(10);

        speedChart = new LineChart<>(xAxis, yAxis);
        speedChart.setTitle("Average Speed");
        speedChart.setLegendVisible(false);
        speedChart.setPrefHeight(120);
        speedChart.setMaxWidth(280);
        speedChart.setCreateSymbols(false);
        speedChart.setAnimated(false);
        speedChart.setStyle("-fx-background-color: #2b2b2b; " +
                          "-fx-title-side: top; " +
                          ".chart-title { -fx-text-fill: white; }");

        speedSeries = new XYChart.Series<>();
        speedChart.getData().add(speedSeries);

        getChildren().addAll(title,
            simSection, simTimeLabel, totalVehiclesLabel, activeVehiclesLabel,
            trafficSection, avgSpeedLabel, stoppedVehiclesLabel,
            vehicleSection, carsLabel, trucksLabel, busesLabel, motorcyclesLabel,
            systemSection, fpsLabel,
            speedChart
        );
    }

    private Label createHeaderLabel(String text){
        Label label = new Label(text);
        label.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        label.setTextFill(Color.web("#ffffff"));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label createSectionLabel(String text){
        Label label = new Label(text);
        label.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        label.setTextFill(Color.web("#4a9eff"));
        label.setPadding(new Insets(10, 0, 5, 0));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label createDataLabel(String text){
        Label label = new Label(text);
        label.setFont(Font.font("Monospace", 12));
        label.setTextFill(Color.web("#cccccc"));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    public void update(DashBoardData data){
        simTimeLabel.setText(String.format("Time: %.1fs", data.simTime));
        totalVehiclesLabel.setText("Total: " + data.totalVehicles);
        activeVehiclesLabel.setText("Active: " + data.activeVehicles);
        avgSpeedLabel.setText(String.format("Avg Speed: %.2f m/s", data.avgSpeed));
        stoppedVehiclesLabel.setText("Stopped: " + data.stoppedVehicles);
        
        carsLabel.setText("Cars: " + data.carCount);
        trucksLabel.setText("Trucks: " + data.truckCount);
        busesLabel.setText("Buses: " + data.busCount);
        motorcyclesLabel.setText("Motorcycles: " + data.motorcycleCount);

        fpsLabel.setText(String.format("FPS: %.1f", data.fps));

        speedSeries.getData().add(new XYChart.Data<>(data.simTime, data.avgSpeed));
        if(speedSeries.getData().size() > maxDataPoints){
            speedSeries.getData().remove(0);
        }
        
        // Update x-axis bounds to create a sliding window effect
        NumberAxis xAxis = (NumberAxis) speedChart.getXAxis();
        if (data.simTime > 60) {
            xAxis.setLowerBound(data.simTime - 60);
            xAxis.setUpperBound(data.simTime);
        }
    }

    public static class DashBoardData {
        public double simTime;
        public int totalVehicles;
        public int activeVehicles;
        public double avgSpeed;
        public int stoppedVehicles;
        public int carCount;
        public int truckCount;
        public int busCount;
        public int motorcycleCount;
        public double fps;
    }
}
