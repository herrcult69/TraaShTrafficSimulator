import javafx.scene.control.Button;

/**
 * Centralized UI styling constants and helper methods
 * Ensures consistent look and feel across all panels
 */
public class UIStyles {
    // Color palette
    public static final String BG_PRIMARY = "#0D1B2A";
    public static final String BG_SECONDARY = "#1B263B";
    public static final String BG_BUTTON = "#1B263B";
    public static final String BG_BUTTON_HOVER = "#415A77";
    public static final String BG_BUTTON_ACCENT = "#415A77";
    public static final String BG_BUTTON_ACCENT_HOVER = "#778DA9";
    
    public static final String TEXT_PRIMARY = "white";
    public static final String TEXT_SECONDARY = "#778DA9";
    public static final String TEXT_SUCCESS = "#4CAF50";
    public static final String TEXT_WARNING = "#FFA726";
    public static final String TEXT_ERROR = "#EF5350";
    
    // Text styles
    public static final String TITLE_STYLE = 
        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16;";
    
    public static final String LABEL_STYLE = 
        "-fx-text-fill: white; -fx-font-size: 12;";
    
    public static final String LABEL_SECONDARY_STYLE = 
        "-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 11;";
    
    public static final String INFO_LABEL_STYLE = 
        "-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-weight: bold; -fx-font-size: 11;";
    
    // Button styles
    private static final String BASE_BUTTON_STYLE = 
        "-fx-background-color: " + BG_BUTTON + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 12; " +
        "-fx-padding: 8;";
    
    private static final String BASE_BUTTON_HOVER_STYLE = 
        "-fx-background-color: " + BG_BUTTON_HOVER + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 12; " +
        "-fx-padding: 8;";
    
    private static final String ACCENT_BUTTON_STYLE = 
        "-fx-background-color: " + BG_BUTTON_ACCENT + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 12; " +
        "-fx-padding: 8; " +
        "-fx-font-weight: bold;";
    
    private static final String ACCENT_BUTTON_HOVER_STYLE = 
        "-fx-background-color: " + BG_BUTTON_ACCENT_HOVER + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 12; " +
        "-fx-padding: 8; " +
        "-fx-font-weight: bold;";
    
    // Input field styles
    public static final String INPUT_FIELD_STYLE = 
        "-fx-background-color: " + BG_SECONDARY + "; " +
        "-fx-text-fill: white;";
    
    public static final String COMBO_BOX_STYLE = 
        "-fx-background-color: " + BG_SECONDARY + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 12; " +
        "-fx-opacity: 1.0;";
    
    public static final String MONOSPACE_FIELD_STYLE = 
        "-fx-background-color: " + BG_SECONDARY + "; " +
        "-fx-text-fill: #00ff00; " +
        "-fx-font-family: monospace; " +
        "-fx-font-size: 12;";
    
    // Info box style
    public static final String INFO_BOX_STYLE = 
        "-fx-background-color: " + BG_SECONDARY + "; " +
        "-fx-padding: 10; " +
        "-fx-background-radius: 5;";
    
    /**
     * Create a standard styled button with hover effect
     * @param text Button text
     * @return Configured button with standard styling
     */
    public static Button createStyledButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(250);
        btn.setStyle(BASE_BUTTON_STYLE);
        btn.setOnMouseEntered(e -> btn.setStyle(BASE_BUTTON_HOVER_STYLE));
        btn.setOnMouseExited(e -> btn.setStyle(BASE_BUTTON_STYLE));
        return btn;
    }
    
    /**
     * Create an accent styled button (for primary actions)
     * @param text Button text
     * @return Configured button with accent styling
     */
    public static Button createAccentButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(250);
        btn.setStyle(ACCENT_BUTTON_STYLE);
        btn.setOnMouseEntered(e -> btn.setStyle(ACCENT_BUTTON_HOVER_STYLE));
        btn.setOnMouseExited(e -> btn.setStyle(ACCENT_BUTTON_STYLE));
        return btn;
    }
    
    /**
     * Apply standard button styling to an existing button
     * @param btn Button to style
     */
    public static void applyStandardButtonStyle(Button btn) {
        btn.setStyle(BASE_BUTTON_STYLE);
        btn.setOnMouseEntered(e -> btn.setStyle(BASE_BUTTON_HOVER_STYLE));
        btn.setOnMouseExited(e -> btn.setStyle(BASE_BUTTON_STYLE));
    }
    
    /**
     * Apply accent button styling to an existing button
     * @param btn Button to style
     */
    public static void applyAccentButtonStyle(Button btn) {
        btn.setStyle(ACCENT_BUTTON_STYLE);
        btn.setOnMouseEntered(e -> btn.setStyle(ACCENT_BUTTON_HOVER_STYLE));
        btn.setOnMouseExited(e -> btn.setStyle(ACCENT_BUTTON_STYLE));
    }
}
