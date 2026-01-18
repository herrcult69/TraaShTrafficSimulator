import javafx.scene.canvas.Canvas;

/**
 * Manages view transformations including zoom, pan, rotation, and coordinate scaling.
 * Handles automatic network fitting and interactive view manipulation.
 * 
 * @author M A T^2 H Team
 * @version 2.0
 * @see CoordinateTransform
 */
public class ViewManager {
    private Canvas canvas;
    private CoordinateTransform transform;
    private NetworkParser.NetworkData network;

    // View transformation state
    private double scale = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double zoom = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private double rotationAngle = 0.0; // Rotation angle in radians

    // Mouse drag state
    private double dragStartX, dragStartY, dragStartPanX, dragStartPanY;

    /**
     * Constructs a view manager.
     * 
     * @param canvas Canvas to manage
     * @param transform Coordinate transform
     * @param network Network data with bounds
     */
    public ViewManager(Canvas canvas, CoordinateTransform transform, NetworkParser.NetworkData network) {
        this.canvas = canvas;
        this.transform = transform;
        this.network = network;
    }

    /** Resets view to fit entire network. */
    public void resetView() {
        double margin = 50;
        double netW = network.maxX - network.minX;
        double netH = network.maxY - network.minY;

        if (netW == 0 || netH == 0) {
            scale = 1.0;
            offsetX = offsetY = 400;
            updateTransform();
            return;
        }

        // Calculate scale to fit network with margins, maintain aspect ratio
        scale = Math.min((canvas.getWidth() - 2 * margin) / netW,
                (canvas.getHeight() - 2 * margin) / netH);

        // Center network on canvas
        offsetX = (canvas.getWidth() - netW * scale) / 2 - network.minX * scale;
        offsetY = (canvas.getHeight() - netH * scale) / 2 - network.minY * scale;

        // Reset user modifications
        zoom = 1.0;
        panX = panY = 0.0;
        rotationAngle = 0.0;
        updateTransform();
    }

    /**
     * Zooms the view centered on the canvas center.
     * 
     * @param factor The zoom factor
     */
    public void zoomToCenter(double factor) {
        double centerX = canvas.getWidth() / 2.0;
        double centerY = canvas.getHeight() / 2.0;
        zoomToPoint(factor, centerX, centerY);
    }

    /**
     * Zooms the view centered on a specific screen point.
     * 
     * @param factor  The zoom factor
     * @param targetX The X coordinate to zoom toward
     * @param targetY The Y coordinate to zoom toward
     */
    public void zoomToPoint(double factor, double targetX, double targetY) {
        double worldX = transform.screenToWorldX(targetX);
        double worldY = transform.screenToWorldY(targetY);

        // Apply new zoom level
        zoom = Math.max(0.1, Math.min(10.0, zoom * factor));
        updateTransform();

        // Calculate where that world point appears now
        double newScreenX = transform.worldToScreenX(worldX);
        double newScreenY = transform.worldToScreenY(worldY);

        // Adjust pan to keep the world point under the cursor
        panX += (targetX - newScreenX);
        panY -= (targetY - newScreenY);

        updateTransform();
    }

    /**
     * Begins a panning operation.
     * 
     * @param screenX The initial X coordinate
     * @param screenY The initial Y coordinate
     */
    public void startPan(double screenX, double screenY) {
        dragStartX = screenX;
        dragStartY = screenY;
        dragStartPanX = panX;
        dragStartPanY = panY;
    }

    /**
     * Updates the pan offsets during a drag operation.
     * Accounts for rotation so that panning always follows the mouse direction.
     * 
     * @param screenX The current X coordinate
     * @param screenY The current Y coordinate
     */
    public void updatePan(double screenX, double screenY) {
        // Calculate the raw delta in screen coordinates
        double deltaX = screenX - dragStartX;
        double deltaY = screenY - dragStartY;
        
        // When the map is rotated, we need to rotate the pan delta by the inverse
        // of the rotation angle so that panning follows the mouse direction
        if (rotationAngle != 0.0) {
            double cos = Math.cos(-rotationAngle);
            double sin = Math.sin(-rotationAngle);
            double rotatedDeltaX = deltaX * cos - deltaY * sin;
            double rotatedDeltaY = deltaX * sin + deltaY * cos;
            deltaX = rotatedDeltaX;
            deltaY = rotatedDeltaY;
        }
        
        panX = dragStartPanX + deltaX;
        panY = dragStartPanY - deltaY;
        updateTransform();
    }

    /**
     * Updates the coordinate transform with the current view state.
     */
    public void updateTransform() {
        transform.setCanvasDimensions(canvas.getWidth(), canvas.getHeight());
        transform.updateTransform(scale, offsetX, offsetY, zoom, panX, panY, rotationAngle);
    }

    /**
     * Rotates the view by a specified angle.
     * 
     * @param angleDegrees The angle to rotate by in degrees (positive = clockwise)
     */
    public void rotate(double angleDegrees) {
        rotationAngle += Math.toRadians(angleDegrees);
        // Normalize angle to [-2π, 2π] range
        while (rotationAngle > 2 * Math.PI) rotationAngle -= 2 * Math.PI;
        while (rotationAngle < -2 * Math.PI) rotationAngle += 2 * Math.PI;
        updateTransform();
    }

    /**
     * Sets the rotation angle to a specific value.
     * 
     * @param angleDegrees The absolute rotation angle in degrees
     */
    public void setRotation(double angleDegrees) {
        rotationAngle = Math.toRadians(angleDegrees);
        updateTransform();
    }

    /**
     * Gets the current rotation angle in degrees.
     * 
     * @return The rotation angle in degrees
     */
    public double getRotationDegrees() {
        return Math.toDegrees(rotationAngle);
    }

    /**
     * Resets only the rotation to 0 degrees.
     */
    public void resetRotation() {
        rotationAngle = 0.0;
        updateTransform();
    }

    /**
     * Sets a new coordinate transform reference.
     * 
     * @param transform The new coordinate transform
     */
    public void setTransform(CoordinateTransform transform) {
        this.transform = transform;
    }

    /**
     * Returns the current coordinate transform.
     * 
     * @return The coordinate transform
     */
    public CoordinateTransform getTransform() {
        return transform;
    }
}
