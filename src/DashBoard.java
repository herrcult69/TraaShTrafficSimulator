import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * UI panel displaying simulation controls and dashboard metrics.
 * Provides interface for play/pause/stop controls and view management.
 */
public class DashBoard extends VBox {
    private ScrollPane scrollPane;

    public DashBoard() {
        super(10);
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #2b2b2b;");
        setAlignment(Pos.TOP_CENTER);
        setMinWidth(300);
        setMaxWidth(300);
        
        createMockupUI();
        
        // Wrap in ScrollPane
        scrollPane = new ScrollPane(this);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMinWidth(300);
        scrollPane.setMaxWidth(300);
        scrollPane.setStyle("-fx-background: #2b2b2b; -fx-background-color: #2b2b2b;");
    }
    
    private void createMockupUI() {
        // Simulation controls
        Label simLabel = createHeaderLabel("=== SIMULATION ===");
        Button playBtn = createButton("> Play");
        Button pauseBtn = createButton("| | Pause");
        Button stopBtn = createButton("# Stop");
        
        getChildren().addAll(simLabel, playBtn, pauseBtn, stopBtn);
        getChildren().add(new Separator());
        
        // View controls
        Label viewLabel = createHeaderLabel("=== VIEW ===");
        Button zoomIn = createButton("+ Zoom In");
        Button zoomOut = createButton("- Zoom Out");
        Button reset = createButton("@ Reset View");
        
        getChildren().addAll(viewLabel, zoomIn, zoomOut, reset);
        getChildren().add(new Separator());
        
        // Dashboard info
        Label dashLabel = createHeaderLabel("=== DASHBOARD ===");
        
        Label simSection = createSectionLabel("Simulation");
        Label simTimeLabel = createDataLabel("Time: ### s");
        Label activeVehiclesLabel = createDataLabel("Active: ###");
        
        Label trafficSection = createSectionLabel("Traffic Stats");
        Label avgSpeedLabel = createDataLabel("Avg Speed: ### m/s");
        
        Label vehicleSection = createSectionLabel("Vehicle Types");
        Label carsLabel = createDataLabel("Cars: ###");
        Label trucksLabel = createDataLabel("Trucks: ###");
        Label busesLabel = createDataLabel("Buses: ###");
        Label motorcyclesLabel = createDataLabel("Motorcycles: ###");
        Label emergencyLabel = createDataLabel("Emergency: ###");
        
        getChildren().addAll(dashLabel, simSection, simTimeLabel, activeVehiclesLabel,
                            trafficSection, avgSpeedLabel,
                            vehicleSection, carsLabel, trucksLabel, busesLabel, 
                            motorcyclesLabel, emergencyLabel);
    }
    
    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        label.setTextFill(Color.web("#ffffff"));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
    
    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        label.setTextFill(Color.web("#4a9eff"));
        label.setPadding(new Insets(10, 0, 5, 0));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
    
    private Label createDataLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Monospace", 12));
        label.setTextFill(Color.web("#cccccc"));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
    
    private Button createButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(250);
        
        String buttonStyle = "-fx-background-color: #3c3f41; -fx-text-fill: white; " +
                "-fx-font-size: 12; -fx-padding: 8;";
        String buttonHoverStyle = buttonStyle + "-fx-background-color: #4c4f51;";
        
        btn.setStyle(buttonStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(buttonHoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(buttonStyle));
        
        return btn;
    }
    
    public ScrollPane getScrollPane() {
        return scrollPane;
    }
}
