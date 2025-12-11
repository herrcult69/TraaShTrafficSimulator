import javafx.scene.canvas.Canvas;

/**
 * Manages view transformations including zoom, pan, and coordinate system scaling for the traffic network visualization.
 * 
 * <p>This class handles:
 * <ul>
 *   <li>Automatic fitting of the network to the canvas with proper margins</li>
 *   <li>Interactive zooming (mouse wheel or buttons) while maintaining the zoom point</li>
 *   <li>Interactive panning (mouse drag) for navigating the network</li>
 *   <li>Updating the coordinate transform based on current view state</li>
 * </ul>
 * 
 * <p>The view transformation consists of:
 * <ul>
 *   <li>Base scale: Calculated to fit the entire network in the canvas</li>
 *   <li>User zoom: Interactive zoom level (1.0 = no zoom, >1.0 = zoomed in)</li>
 *   <li>Base offset: Centers the network on the canvas</li>
 *   <li>Pan offset: User's interactive panning adjustment</li>
 * </ul>
 * 
 * @author M A T^2 H Team
 * @version 2.0
 * @see CoordinateTransform
 * @see TrafficSimulatorApp
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

    // Mouse drag state
    private double dragStartX, dragStartY, dragStartPanX, dragStartPanY;

    /**
     * Constructs a new view manager for the specified canvas and network.
     * 
     * @param canvas The canvas to manage the view for
     * @param transform The coordinate transform to update
     * @param network The network data containing bounds information
     */
    public ViewManager(Canvas canvas, CoordinateTransform transform, NetworkParser.NetworkData network) {
        this.canvas = canvas;
        this.transform = transform;
        this.network = network;
    }

    /**
     * Resets the view to fit the entire network on the canvas with margins.
     * Also resets user zoom and pan to default values.
     */
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
        updateTransform();
    }

    /**
     * Zooms the view in or out, centered on the canvas center.
     * Used by zoom buttons in the control panel.
     * 
     * @param factor The zoom factor (&gt;1.0 to zoom in, &lt;1.0 to zoom out)
     */
    public void zoomToCenter(double factor) {
        double centerX = canvas.getWidth() / 2.0;
        double centerY = canvas.getHeight() / 2.0;
        zoomToPoint(factor, centerX, centerY);
    }

    /**
     * Zooms the view in or out, centered on a specific screen point.
     * Used by scroll wheel zooming to zoom toward/away from the cursor.
     * Adjusts the pan to keep the world point under the cursor stationary.
     * 
     * @param factor The zoom factor (&gt;1.0 to zoom in, &lt;1.0 to zoom out)
     * @param targetX The X coordinate in screen space to zoom toward
     * @param targetY The Y coordinate in screen space to zoom toward
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
     * Begins a panning operation by recording the initial mouse position and current pan offsets.
     * 
     * @param screenX The initial X coordinate in screen space
     * @param screenY The initial Y coordinate in screen space
     */
    public void startPan(double screenX, double screenY) {
        dragStartX = screenX;
        dragStartY = screenY;
        dragStartPanX = panX;
        dragStartPanY = panY;
    }

    /**
     * Updates the pan offsets based on the current mouse position during a drag operation.
     * 
     * @param screenX The current X coordinate in screen space
     * @param screenY The current Y coordinate in screen space
     */
    public void updatePan(double screenX, double screenY) {
        panX = dragStartPanX + screenX - dragStartX;
        panY = dragStartPanY - (screenY - dragStartY);
        updateTransform();
    }

    /**
     * Updates the coordinate transform with the current view state.
     * Must be called after any change to scale, offset, zoom, or pan values.
     */
    public void updateTransform() {
        transform.updateTransform(scale, offsetX, offsetY, zoom, panX, panY);
    }

    /**
     * Sets a new coordinate transform reference.
     * Needed when the canvas is resized and a new transform is created.
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
