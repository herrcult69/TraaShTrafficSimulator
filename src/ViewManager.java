import javafx.scene.canvas.Canvas;

/**
 * Manages view transformations: zoom, pan, and coordinate system
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
    
    public ViewManager(Canvas canvas, CoordinateTransform transform, NetworkParser.NetworkData network) {
        this.canvas = canvas;
        this.transform = transform;
        this.network = network;
    }
    
    /** Fit entire network to canvas with margins and reset zoom/pan */
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
    
    /** Zoom to screen center (for buttons) */
    public void zoomToCenter(double factor) {
        double centerX = canvas.getWidth() / 2.0;
        double centerY = canvas.getHeight() / 2.0;
        zoomToPoint(factor, centerX, centerY);
    }
    
    /** Zoom to specific point (for scroll wheel at cursor) */
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
    
    /** Start panning operation */
    public void startPan(double screenX, double screenY) {
        dragStartX = screenX;
        dragStartY = screenY;
        dragStartPanX = panX;
        dragStartPanY = panY;
    }
    
    /** Update panning based on mouse drag */
    public void updatePan(double screenX, double screenY) {
        panX = dragStartPanX + screenX - dragStartX;
        panY = dragStartPanY - (screenY - dragStartY);
        updateTransform();
    }
    
    /** Update the coordinate transform with current view state */
    public void updateTransform() {
        transform.updateTransform(scale, offsetX, offsetY, zoom, panX, panY);
    }
    
    /** Update transform reference (needed when canvas resizes) */
    public void setTransform(CoordinateTransform transform) {
        this.transform = transform;
    }
    
    /** Get current transform */
    public CoordinateTransform getTransform() {
        return transform;
    }
}
