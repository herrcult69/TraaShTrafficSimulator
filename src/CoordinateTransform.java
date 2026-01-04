/**
 * Handles coordinate transformation between SUMO world coordinates and JavaFX screen coordinates.
 * Applies scale, zoom, offset, pan, and Y-axis inversion.
 *
 * @author M A T^2 H Team
 * @version 2.0
 * @see ViewManager
 */
public class CoordinateTransform {
    private double scale, offsetX, offsetY, zoom, panX, panY;
    private double canvasHeight;

    /**
     * Constructs a new coordinate transform.
     * 
     * @param canvasHeight The height of the canvas in pixels
     */
    public CoordinateTransform(double canvasHeight) {
        this.canvasHeight = canvasHeight;
        this.scale = 1.0;
        this.zoom = 1.0;
    }

    /**
     * Updates all transformation parameters.
     * 
     * @param scale Base scaling factor
     * @param offsetX Base X offset
     * @param offsetY Base Y offset
     * @param zoom User-controlled zoom level
     * @param panX Interactive pan offset in X
     * @param panY Interactive pan offset in Y
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
     * @param worldX The X coordinate in world space
     * @return The X coordinate in screen space
     */
    public double worldToScreenX(double worldX) {
        return (worldX * scale * zoom) + offsetX + panX;
    }

    /**
     * Converts a world Y coordinate to screen Y coordinate with Y-axis inversion.
     * 
     * @param worldY The Y coordinate in world space
     * @return The Y coordinate in screen space
     */
    public double worldToScreenY(double worldY) {
        return canvasHeight - ((worldY * scale * zoom) + offsetY + panY);
    }

    /**
     * Converts a world size to screen size.
     * 
     * @param worldSize The size in world space
     * @return The size in screen space
     */
    public double worldToScreenSize(double worldSize) {
        return worldSize * scale * zoom;
    }

    /**
     * Converts a screen X coordinate to world X coordinate.
     * 
     * @param screenX The X coordinate in screen space
     * @return The X coordinate in world space
     */
    public double screenToWorldX(double screenX) {
        return (screenX - offsetX - panX) / (scale * zoom);
    }

    /**
     * Converts a screen Y coordinate to world Y coordinate with Y-axis inversion.
     * 
     * @param screenY The Y coordinate in screen space
     * @return The Y coordinate in world space
     */
    public double screenToWorldY(double screenY) {
        return ((canvasHeight - screenY) - offsetY - panY) / (scale * zoom);
    }

    /**
     * Converts a screen size to world size.
     * 
     * @param screenSize The size in screen space
     * @return The size in world space
     */
    public double screenToWorldSize(double screenSize) {
        return screenSize / (scale * zoom);
    }
}