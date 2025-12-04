import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

/**
 * Control panel for simulation and view controls
 */
public class ControlPanel {
    private VBox controlPanel;
    private ScrollPane scrollPane;
    private SimulationRunner runner;
    private ViewManager viewManager;
    
    public ControlPanel(SimulationRunner runner, ViewManager viewManager, DashBoard dashboard) {
        this.runner = runner;
        this.viewManager = viewManager;
        createPanel(dashboard);
    }
    
    private void createPanel(DashBoard dashboard) {
        controlPanel = new VBox(10);
        controlPanel.setAlignment(Pos.TOP_CENTER);
        
        // Add simulation controls
        addSimulationControls();
        
        // Add separator
        controlPanel.getChildren().add(new Separator());
        
        // Add view controls
        addViewControls();
        
        // Add dashboard
        controlPanel.getChildren().add(dashboard);
        
        // Style the panel
        controlPanel.setPadding(new Insets(10));
        controlPanel.setSpacing(8);
        controlPanel.setStyle("-fx-background-color: #2b2b2b;");
        controlPanel.setMinWidth(300);
        controlPanel.setMaxWidth(300);
        
        // Wrap in ScrollPane
        scrollPane = new ScrollPane(controlPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMinWidth(300);
        scrollPane.setMaxWidth(300);
        scrollPane.setStyle("-fx-background: #2b2b2b; -fx-background-color: #2b2b2b;");
    }
    
    private void addSimulationControls() {
        Label simLabel = new Label("=== SIMULATION ===");
        simLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        
        Button playBtn = createButton("Play");
        Button pauseBtn = createButton("Pause");
        Button stopBtn = createButton("Stop");
        
        playBtn.setOnAction(e -> {
            if (runner != null) {
                runner.resume();
                System.out.println("Simulation resumed");
            }
        });
        
        pauseBtn.setOnAction(e -> {
            if (runner != null) {
                runner.pause();
                System.out.println("Simulation paused");
            }
        });
        
        stopBtn.setOnAction(e -> {
            if (runner != null) {
                runner.stop();
            }
            System.out.println("Simulation stopped - Exiting application");
            Platform.exit();
            System.exit(0);
        });
        
        controlPanel.getChildren().addAll(simLabel, playBtn, pauseBtn, stopBtn);
    }
    
    private void addViewControls() {
        Label viewLabel = new Label("=== VIEW ===");
        viewLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        
        Button zoomIn = createButton("Zoom In");
        Button zoomOut = createButton("Zoom Out");
        Button reset = createButton("Reset View");
        
        zoomIn.setOnAction(e -> viewManager.zoomToCenter(1.2));
        zoomOut.setOnAction(e -> viewManager.zoomToCenter(0.8));
        reset.setOnAction(e -> viewManager.resetView());
        
        controlPanel.getChildren().addAll(viewLabel, zoomIn, zoomOut, reset);
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
