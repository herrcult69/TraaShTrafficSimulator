import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel for filtering vehicles by type.
 * Provides checkboxes to show/hide different vehicle types.
 *
 * @author M A T^2 H Team
 * @version 2.0
 */
public class VehicleFilterPanel {
    private VBox panel;
    private Map<String, CheckBox> typeCheckBoxes;
    private CheckBox speedFilterCheckBox;
    private Runnable onBackPressed;
    
    /**
     * Constructs a new vehicle filter panel.
     * 
     * @param onBackPressed Callback when back button is pressed
     */
    public VehicleFilterPanel(Runnable onBackPressed) {
        this.onBackPressed = onBackPressed;
        this.typeCheckBoxes = new HashMap<>();
        createPanel();
    }
    
    private void createPanel() {
        panel = new VBox(10);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: " + UIStyles.BG_PRIMARY + ";");
        
        // Title
        Label titleLabel = new Label("VEHICLE FILTERS");
        titleLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: white;");
        
        // Back button
        Button backBtn = UIStyles.createStyledButton("← Back");
        backBtn.setOnAction(e -> {
            if (onBackPressed != null) {
                onBackPressed.run();
            }
        });
        
        // Section label
        Label filterLabel = new Label("―――VEHICLE TYPES―――");
        filterLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        
        // Create checkboxes for each vehicle type
        CheckBox carCheckBox = createTypeCheckBox("Cars", "car");
        CheckBox truckCheckBox = createTypeCheckBox("Trucks", "truck");
        CheckBox busCheckBox = createTypeCheckBox("Buses", "bus");
        CheckBox motoCheckBox = createTypeCheckBox("Motorcycles", "moto");
        CheckBox emergencyCheckBox = createTypeCheckBox("Emergency", "emergency");
        
        // Store references
        typeCheckBoxes.put("car", carCheckBox);
        typeCheckBoxes.put("truck", truckCheckBox);
        typeCheckBoxes.put("bus", busCheckBox);
        typeCheckBoxes.put("moto", motoCheckBox);
        typeCheckBoxes.put("emergency", emergencyCheckBox);
        
        // Speed filter section
        Label speedLabel = new Label("―――SPEED FILTER―――");
        speedLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        
        speedFilterCheckBox = new CheckBox("Show Speed Colors");
        speedFilterCheckBox.setSelected(false); // Off by default
        speedFilterCheckBox.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-family: 'Monospace'; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: bold;"
        );
        
        Label speedInfoLabel = new Label("Green=Slow, Yellow=Medium, Red=Fast");
        speedInfoLabel.setStyle(
            "-fx-text-fill: #778DA9; " +
            "-fx-font-family: 'Monospace'; " +
            "-fx-font-size: 10px;"
        );
        
        // Add all to panel
        panel.getChildren().addAll(
            titleLabel,
            backBtn,
            filterLabel,
            carCheckBox,
            truckCheckBox,
            busCheckBox,
            motoCheckBox,
            emergencyCheckBox,
            new Separator(),
            speedLabel,
            speedFilterCheckBox,
            speedInfoLabel
        );
    }
    
    private CheckBox createTypeCheckBox(String label, String type) {
        CheckBox checkBox = new CheckBox(label);
        checkBox.setSelected(true); // All enabled by default
        checkBox.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-family: 'Monospace'; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: bold;"
        );
        return checkBox;
    }
    
    /**
     * Checks if a vehicle type should be rendered.
     * 
     * @param type Vehicle type (car, truck, bus, moto, emergency)
     * @return true if the type should be visible
     */
    public boolean isTypeVisible(String type) {
        CheckBox checkBox = typeCheckBoxes.get(type);
        return checkBox != null && checkBox.isSelected();
    }
    
    /**
     * Checks if speed filter is enabled.
     * 
     * @return true if speed colors should be shown
     */
    public boolean isSpeedFilterEnabled() {
        return speedFilterCheckBox != null && speedFilterCheckBox.isSelected();
    }
    
    /**
     * Returns the panel component.
     * 
     * @return The VBox panel
     */
    public VBox getPanel() {
        return panel;
    }
}
