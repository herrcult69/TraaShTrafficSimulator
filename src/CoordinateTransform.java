/**
 * Handles coordinate transformation between SUMO world coordinates and JavaFX
 * screen coordinates.
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
     * Constructs a coordinate transform.
     * 
     * @param canvasHeight Canvas height in pixels
     */
    public CoordinateTransform(double canvasHeight) {
        this.canvasHeight = canvasHeight;
        this.scale = 1.0;
        this.zoom = 1.0;
    }

    /**
     * Updates transformation parameters.
     * 
     * @param scale Base scaling factor
     * @param offsetX Base X offset
     * @param offsetY Base Y offset
     * @param zoom User zoom level
     * @param panX Pan offset in X
     * @param panY Pan offset in Y
     */
    public void updateTransform(double scale, double offsetX, double offsetY, double zoom, double panX, double panY) {
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.zoom = zoom;
        this.panX = panX;
        this.panY = panY;
    }

    /** Converts world X to screen X. */
    public double worldToScreenX(double worldX) {
        return (worldX * scale * zoom) + offsetX + panX;
    }

    /** Converts world Y to screen Y (with Y-axis inversion). */
    public double worldToScreenY(double worldY) {
        return canvasHeight - ((worldY * scale * zoom) + offsetY + panY);
    }

    /** Converts world size to screen size. */
    public double worldToScreenSize(double worldSize) {
        return worldSize * scale * zoom;
    }

    /** Converts screen X to world X. */
    public double screenToWorldX(double screenX) {
        return (screenX - offsetX - panX) / (scale * zoom);
    }

    /** Converts screen Y to world Y (with Y-axis inversion). */
    public double screenToWorldY(double screenY) {
        return ((canvasHeight - screenY) - offsetY - panY) / (scale * zoom);
    }

    /** Converts screen size to world size. */
    public double screenToWorldSize(double screenSize) {
        return screenSize / (scale * zoom);
    }
}