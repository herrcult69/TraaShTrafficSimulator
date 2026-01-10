import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.List;

/**
 * Panel for monitoring and displaying congestion hotspots in the traffic network.
 * 
 * <p>Features:
 * <ul>
 *   <li>Real-time display of top congestion hotspots</li>
 *   <li>Toggle button to show/hide congestion overlays</li>
 *   <li>Color-coded severity indicators</li>
 *   <li>Detailed metrics for each hotspot (speed, density, duration)</li>
 *   <li>Total congestion count</li>
 * </ul>
 *
 * @author M A T^2 H Team
 * @version 2.0 
 * @see CongestionHotspot
 * @see TrafficManager
 */
public class CongestionMonitorPanel extends VBox {
    private Label titleLabel;
    private Label totalCongestionsLabel;
    private Button toggleOverlayButton;
    private VBox hotspotsContainer;
    private ScrollPane scrollPane;
    private boolean overlayEnabled;
    private Runnable onBackPressed;
    
    /**
     * Constructs a new congestion monitor panel.
     */
    public CongestionMonitorPanel() {
        super(10);
        setPadding(new Insets(15, 10, 10, 10));
        setStyle("-fx-background-color: #0D1B2A;");
        setAlignment(Pos.TOP_CENTER);
        setMaxWidth(320);
        this.overlayEnabled = false;
        this.onBackPressed = null;
        
        initializeComponents();
    }
    
    /**
     * Sets the back button callback.
     * 
     * @param onBackPressed Callback when back button is pressed
     */
    public void setOnBackPressed(Runnable onBackPressed) {
        this.onBackPressed = onBackPressed;
        // Reinitialize to add back button
        getChildren().clear();
        initializeComponents();
    }
    
    /**
     * Initializes all UI components.
     */
    private void initializeComponents() {
        // Title
        titleLabel = createHeaderLabel("CONGESTION MONITOR");
        
        // Back button (if callback is set)
        if (onBackPressed != null) {
            Button backBtn = new Button("← Back");
            backBtn.setStyle(
                "-fx-background-color: #1B263B; " +
                "-fx-text-fill: white; " +
                "-fx-font-family: 'Monospace'; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 12px; " +
                "-fx-padding: 8 16; " +
                "-fx-cursor: hand;"
            );
            backBtn.setMaxWidth(Double.MAX_VALUE);
            backBtn.setOnAction(e -> onBackPressed.run());
            getChildren().add(backBtn);
        }
        
        // Total congestions count
        totalCongestionsLabel = createDataLabel("Active Hotspots: 0");
        
        // Toggle overlay button
        toggleOverlayButton = new Button("Show Overlay");
        toggleOverlayButton.setStyle(
            "-fx-background-color: #415A77; " +
            "-fx-text-fill: white; " +
            "-fx-font-family: 'Monospace'; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 12px; " +
            "-fx-padding: 8 16; " +
            "-fx-cursor: hand;"
        );
        toggleOverlayButton.setMaxWidth(Double.MAX_VALUE);
        
        // Hotspots list container
        hotspotsContainer = new VBox(8);
        hotspotsContainer.setAlignment(Pos.TOP_CENTER);
        hotspotsContainer.setPadding(new Insets(5, 0, 5, 0));
        
        // Scroll pane for hotspots
        scrollPane = new ScrollPane(hotspotsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(400);
        scrollPane.setStyle("-fx-background: #0D1B2A; -fx-background-color: #0D1B2A;");
        scrollPane.setPadding(new Insets(5));
        
        // Add all components
        getChildren().addAll(
            titleLabel,
            totalCongestionsLabel,
            toggleOverlayButton,
            createSectionLabel("Top Hotspots"),
            scrollPane
        );
    }
    
    /**
     * Updates the panel with current congestion hotspot data.
     * 
     * @param hotspots List of congestion hotspots to display (typically top N)
     * @param totalCount Total number of congested edges
     */
    public void updateHotspots(List<CongestionHotspot> hotspots, int totalCount) {
        // Update total count
        totalCongestionsLabel.setText(String.format("Active Hotspots: %d", totalCount));
        
        // Clear and rebuild hotspot list
        hotspotsContainer.getChildren().clear();
        
        if (hotspots.isEmpty()) {
            Label noDataLabel = createDataLabel("No congestion detected");
            noDataLabel.setStyle("-fx-text-fill: #50C878; -fx-font-style: italic;");
            hotspotsContainer.getChildren().add(noDataLabel);
        } else {
            for (int i = 0; i < hotspots.size(); i++) {
                CongestionHotspot hotspot = hotspots.get(i);
                VBox hotspotCard = createHotspotCard(i + 1, hotspot);
                hotspotsContainer.getChildren().add(hotspotCard);
            }
        }
    }
    
    /**
     * Creates a visual card for a congestion hotspot.
     * 
     * @param rank The ranking of this hotspot (1 = worst)
     * @param hotspot The congestion hotspot data
     * @return A VBox containing the formatted hotspot information
     */
    private VBox createHotspotCard(int rank, CongestionHotspot hotspot) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(8));
        card.setStyle(
            "-fx-background-color: #1B263B; " +
            "-fx-border-color: " + toHexString(hotspot.getSeverityColor()) + "; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5;"
        );
        card.setMaxWidth(Double.MAX_VALUE);
        
        // Rank and severity header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label rankLabel = new Label("#" + rank);
        rankLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        rankLabel.setTextFill(Color.WHITE);
        
        Label severityLabel = new Label(hotspot.getSeverityDescription());
        severityLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        severityLabel.setTextFill(hotspot.getSeverityColor());
        
        header.getChildren().addAll(rankLabel, severityLabel);
        
        // Edge ID
        Label edgeLabel = new Label("Edge: " + hotspot.getEdgeId());
        edgeLabel.setFont(Font.font("Monospace", 11));
        edgeLabel.setTextFill(Color.web("#E0E1DD"));
        
        // Metrics
        Label scoreLabel = new Label(String.format("Score: %.1f/100", hotspot.getCongestionScore()));
        scoreLabel.setFont(Font.font("Monospace", 10));
        scoreLabel.setTextFill(Color.web("#778DA9"));
        
        Label speedLabel = new Label(String.format("Speed: %.1f m/s (%.1f km/h)", 
                                                   hotspot.getAvgSpeed(), 
                                                   hotspot.getAvgSpeed() * 3.6));
        speedLabel.setFont(Font.font("Monospace", 10));
        speedLabel.setTextFill(Color.web("#778DA9"));
        
        Label densityLabel = new Label(String.format("Density: %.1f veh/km (%d vehicles)", 
                                                     hotspot.getDensity(),
                                                     hotspot.getEdge().getVehicleCount()));
        densityLabel.setFont(Font.font("Monospace", 10));
        densityLabel.setTextFill(Color.web("#778DA9"));
        
        // Duration
        long durationSeconds = hotspot.getCongestionDuration() / 1000;
        Label durationLabel = new Label(String.format("Duration: %d:%02d", 
                                                      durationSeconds / 60, 
                                                      durationSeconds % 60));
        durationLabel.setFont(Font.font("Monospace", 10));
        durationLabel.setTextFill(Color.web("#778DA9"));
        
        card.getChildren().addAll(
            header, 
            edgeLabel, 
            scoreLabel, 
            speedLabel, 
            densityLabel, 
            durationLabel
        );
        
        return card;
    }
    
    /**
     * Converts a JavaFX Color to hex string for CSS.
     * 
     * @param color The color to convert
     * @return Hex string (e.g., "#FF5733")
     */
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255)
        );
    }
    
    /**
     * Sets the action to perform when the overlay toggle button is clicked.
     * 
     * @param action The action to perform
     */
    public void setOnToggleOverlay(Runnable action) {
        toggleOverlayButton.setOnAction(e -> {
            overlayEnabled = !overlayEnabled;
            toggleOverlayButton.setText(overlayEnabled ? "Hide Overlay" : "Show Overlay");
            toggleOverlayButton.setStyle(
                "-fx-background-color: " + (overlayEnabled ? "#E63946" : "#415A77") + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-family: 'Monospace'; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 12px; " +
                "-fx-padding: 8 16; " +
                "-fx-cursor: hand;"
            );
            action.run();
        });
    }
    
    /**
     * Creates a styled header label.
     * 
     * @param text The header text
     * @return A formatted header label
     */
    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        label.setTextFill(Color.web("white"));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
    
    /**
     * Creates a styled section label.
     * 
     * @param text The section name
     * @return A formatted section label
     */
    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        label.setTextFill(Color.web("#E0E1DD"));
        label.setPadding(new Insets(8, 0, 4, 0));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
    
    /**
     * Creates a styled data label.
     * 
     * @param text The initial data text
     * @return A formatted data label
     */
    private Label createDataLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Monospace", 11));
        label.setTextFill(Color.web("#778DA9"));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
    
    /**
     * Returns whether the overlay is currently enabled.
     * 
     * @return true if overlay is enabled
     */
    public boolean isOverlayEnabled() {
        return overlayEnabled;
    }
}
