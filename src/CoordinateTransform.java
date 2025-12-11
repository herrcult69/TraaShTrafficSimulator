/**
 * Handles coordinate transformation between SUMO world coordinates and JavaFX screen coordinates.
 * 
 * <p>This class manages the bidirectional conversion between:
 * <ul>
 *   <li>SUMO world space: Y-axis points up, origin at network center, units in meters</li>
 *   <li>JavaFX screen space: Y-axis points down, origin at top-left, units in pixels</li>
 * </ul>
 * 
 * <p>The transformation pipeline applies:</p>
 * <ol>
 *   <li>Base scale: Initial fit-to-canvas scaling</li>
 *   <li>User zoom: Interactive zoom factor</li>
 *   <li>Base offset: Centering on canvas</li>
 *   <li>Pan offset: Interactive panning</li>
 *   <li>Y-axis flip: Convert from Y-up (SUMO) to Y-down (JavaFX)</li>
 * </ol>
 *
 * @author M A T^2 H Team
 * @version 2.0 
 * @see ViewManager
 * @see TrafficSimulatorApp
 */
public class CoordinateTransform {
    private double scale, offsetX, offsetY, zoom, panX, panY;
    private double canvasHeight;

    /**
     * Constructs a new coordinate transform.
     * 
     * @param canvasHeight The height of the canvas in pixels, used for Y-axis inversion
     */
    public CoordinateTransform(double canvasHeight) {
        this.canvasHeight = canvasHeight;
        this.scale = 1.0;
        this.zoom = 1.0;
    }

    /**
     * Updates all transformation parameters.
     * 
     * @param scale Base scaling factor from world to screen coordinates
     * @param offsetX Base X offset for centering on canvas
     * @param offsetY Base Y offset for centering on canvas
     * @param zoom User-controlled zoom level (1.0 = no zoom)
     * @param panX Interactive pan offset in X direction
     * @param panY Interactive pan offset in Y direction
     */
    public void updateTransform(double scale, double offsetX, double offsetY, double zoom, double panX, double panY) {
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.zoom = zoom;
        this.panX = panX;
        this.panY = panY;
    }

    /**
     * Converts a world X coordinate to screen X coordinate.
     * 
     * @param worldX The X coordinate in SUMO world space (meters)
     * @return The X coordinate in screen space (pixels)
     */
    public double worldToScreenX(double worldX) {
        return (worldX * scale * zoom) + offsetX + panX;
    }

    /**
     * Converts a world Y coordinate to screen Y coordinate with Y-axis inversion.
     * 
     * @param worldY The Y coordinate in SUMO world space (meters, Y-up)
     * @return The Y coordinate in screen space (pixels, Y-down)
     */
    public double worldToScreenY(double worldY) {
        return canvasHeight - ((worldY * scale * zoom) + offsetY + panY);
    }

    /**
     * Converts a world size (distance/length) to screen size.
     * 
     * @param worldSize The size in world space (meters)
     * @return The size in screen space (pixels)
     */
    public double worldToScreenSize(double worldSize) {
        return worldSize * scale * zoom;
    }

    /**
     * Converts a screen X coordinate to world X coordinate.
     * 
     * @param screenX The X coordinate in screen space (pixels)
     * @return The X coordinate in SUMO world space (meters)
     */
    public double screenToWorldX(double screenX) {
        return (screenX - offsetX - panX) / (scale * zoom);
    }

    /**
     * Converts a screen Y coordinate to world Y coordinate with Y-axis inversion.
     * 
     * @param screenY The Y coordinate in screen space (pixels, Y-down)
     * @return The Y coordinate in SUMO world space (meters, Y-up)
     */
    public double screenToWorldY(double screenY) {
        return ((canvasHeight - screenY) - offsetY - panY) / (scale * zoom);
    }

    /**
     * Converts a screen size to world size (distance/length).
     * 
     * @param screenSize The size in screen space (pixels)
     * @return The size in world space (meters)
     */
    public double screenToWorldSize(double screenSize) {
        return screenSize / (scale * zoom);
    }
}