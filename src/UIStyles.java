import javafx.scene.control.Button;

/**
 * Centralized UI styling constants and helper methods.
 * Provides consistent color palette and styled button factory methods.
 *
 * @author M A T^2 H Team
 * @version 2.0
 * @see ControlPanel
 */
public class UIStyles {
    /** Primary background color for main panels (dark navy blue) */
    public static final String BG_PRIMARY = "#0D1B2A";

    /**
     * Secondary background color for input fields and nested elements (lighter
     * navy)
     */
    public static final String BG_SECONDARY = "#1B263B";

    /** Default button background color */
    public static final String BG_BUTTON = "#1B263B";

    /** Button background color on hover */
    public static final String BG_BUTTON_HOVER = "#415A77";

    /** Accent button background color for primary actions */
    public static final String BG_BUTTON_ACCENT = "#415A77";

    /** Accent button background color on hover */
    public static final String BG_BUTTON_ACCENT_HOVER = "#778DA9";

    /** Primary text color (white) */
    public static final String TEXT_PRIMARY = "white";

    /** Secondary text color for less prominent information (gray-blue) */
    public static final String TEXT_SECONDARY = "#778DA9";

    /** Success message text color (green) */
    public static final String TEXT_SUCCESS = "#4CAF50";

    /** Warning message text color (orange) */
    public static final String TEXT_WARNING = "#FFA726";

    /** Error message text color (red) */
    public static final String TEXT_ERROR = "#EF5350";

    // Text styles
    /** CSS style for panel titles (white, bold, 16px) */
    public static final String TITLE_STYLE = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16;";

    /** CSS style for primary labels (white, 12px) */
    public static final String LABEL_STYLE = "-fx-text-fill: white; -fx-font-size: 12;";

    /** CSS style for secondary labels with reduced emphasis (gray-blue, 11px) */
    public static final String LABEL_SECONDARY_STYLE = "-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 11;";

    /** CSS style for informational labels (gray-blue, bold, 11px) */
    public static final String INFO_LABEL_STYLE = "-fx-text-fill: " + TEXT_SECONDARY
            + "; -fx-font-weight: bold; -fx-font-size: 11;";

    // Button styles
    private static final String BASE_BUTTON_STYLE = "-fx-background-color: " + BG_BUTTON + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-padding: 8;";

    private static final String BASE_BUTTON_HOVER_STYLE = "-fx-background-color: " + BG_BUTTON_HOVER + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-padding: 8;";

    private static final String ACCENT_BUTTON_STYLE = "-fx-background-color: " + BG_BUTTON_ACCENT + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-padding: 8; " +
            "-fx-font-weight: bold;";

    private static final String ACCENT_BUTTON_HOVER_STYLE = "-fx-background-color: " + BG_BUTTON_ACCENT_HOVER + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-padding: 8; " +
            "-fx-font-weight: bold;";

    // Input field styles
    /** CSS style for text input fields */
    public static final String INPUT_FIELD_STYLE = "-fx-background-color: " + BG_SECONDARY + "; " +
            "-fx-text-fill: white;";

    /** CSS style for combo boxes (dropdown menus) */
    public static final String COMBO_BOX_STYLE = "-fx-background-color: " + BG_SECONDARY + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-opacity: 1.0;";

    /**
     * CSS style for monospace text fields with terminal-like appearance (green
     * text)
     */
    public static final String MONOSPACE_FIELD_STYLE = "-fx-background-color: " + BG_SECONDARY + "; " +
            "-fx-text-fill: #00ff00; " +
            "-fx-font-family: monospace; " +
            "-fx-font-size: 12;";

    /** CSS style for information boxes with rounded corners */
    public static final String INFO_BOX_STYLE = "-fx-background-color: " + BG_SECONDARY + "; " +
            "-fx-padding: 10; " +
            "-fx-background-radius: 5;";

    /**
     * Creates a standard button with hover effect.
     * 
     * @param text Button label
     * @return Styled button
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
     * Creates an accent button for primary actions.
     * 
     * @param text Button label
     * @return Styled button
     */
    public static Button createAccentButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(250);
        btn.setStyle(ACCENT_BUTTON_STYLE);
        btn.setOnMouseEntered(e -> btn.setStyle(ACCENT_BUTTON_HOVER_STYLE));
        btn.setOnMouseExited(e -> btn.setStyle(ACCENT_BUTTON_STYLE));
        return btn;
    }

    /** Applies standard button styling. */
    public static void applyStandardButtonStyle(Button btn) {
        btn.setStyle(BASE_BUTTON_STYLE);
        btn.setOnMouseEntered(e -> btn.setStyle(BASE_BUTTON_HOVER_STYLE));
        btn.setOnMouseExited(e -> btn.setStyle(BASE_BUTTON_STYLE));
    }

    /** Applies accent button styling. */
    public static void applyAccentButtonStyle(Button btn) {
        btn.setStyle(ACCENT_BUTTON_STYLE);
        btn.setOnMouseEntered(e -> btn.setStyle(ACCENT_BUTTON_HOVER_STYLE));
        btn.setOnMouseExited(e -> btn.setStyle(ACCENT_BUTTON_STYLE));
    }
}
