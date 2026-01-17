/**
 * Handles coordinate transformation between SUMO world coordinates and JavaFX
 * screen coordinates.
 * Applies scale, zoom, offset, pan, rotation, and Y-axis inversion.
 *
 * @author M A T^2 H Team
 * @version 2.0
 * @see ViewManager
 */
public class CoordinateTransform {
    private double scale, offsetX, offsetY, zoom, panX, panY;
    private double canvasHeight;
    private double canvasWidth;
    private double rotationAngle = 0.0; // Rotation angle in radians

    /**
     * Constructs a coordinate transform.
     * 
     * @param canvasHeight Canvas height in pixels
     */
    public CoordinateTransform(double canvasHeight) {
        this.canvasHeight = canvasHeight;
        this.canvasWidth = canvasHeight; // Default, will be updated
        this.scale = 1.0;
        this.zoom = 1.0;
        this.rotationAngle = 0.0;
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

    /**
     * Updates transformation parameters including rotation.
     * 
     * @param scale Base scaling factor
     * @param offsetX Base X offset
     * @param offsetY Base Y offset
     * @param zoom User zoom level
     * @param panX Pan offset in X
     * @param panY Pan offset in Y
     * @param rotationAngle Rotation angle in radians
     */
    public void updateTransform(double scale, double offsetX, double offsetY, double zoom, double panX, double panY, double rotationAngle) {
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.zoom = zoom;
        this.panX = panX;
        this.panY = panY;
        this.rotationAngle = rotationAngle;
    }

    /**
     * Sets the canvas dimensions for rotation calculations.
     * 
     * @param width Canvas width in pixels
     * @param height Canvas height in pixels
     */
    public void setCanvasDimensions(double width, double height) {
        this.canvasWidth = width;
        this.canvasHeight = height;
    }

    /** Converts world X to screen X. */
    public double worldToScreenX(double worldX) {
        return (worldX * scale * zoom) + offsetX + panX;
    }

    /** Converts world Y to screen Y (with Y-axis inversion). */
    public double worldToScreenY(double worldY) {
        return canvasHeight - ((worldY * scale * zoom) + offsetY + panY);
    }

    /**
     * Converts world coordinates to screen coordinates with rotation applied.
     * Use this method when you need both X and Y transformed together for rotation.
     * 
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @return Array containing [screenX, screenY]
     */
    public double[] worldToScreen(double worldX, double worldY) {
        // First apply scale, zoom, offset, and pan (without rotation)
        double x = (worldX * scale * zoom) + offsetX + panX;
        double y = canvasHeight - ((worldY * scale * zoom) + offsetY + panY);
        
        // Apply rotation around canvas center
        if (rotationAngle != 0.0) {
            double centerX = canvasWidth / 2.0;
            double centerY = canvasHeight / 2.0;
            
            // Translate to origin (canvas center)
            double translatedX = x - centerX;
            double translatedY = y - centerY;
            
            // Apply rotation
            double cos = Math.cos(rotationAngle);
            double sin = Math.sin(rotationAngle);
            double rotatedX = translatedX * cos - translatedY * sin;
            double rotatedY = translatedX * sin + translatedY * cos;
            
            // Translate back
            x = rotatedX + centerX;
            y = rotatedY + centerY;
        }
        
        return new double[] { x, y };
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

    /**
     * Converts screen coordinates to world coordinates with rotation applied.
     * Use this for mouse picking when rotation is active.
     * 
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @return Array containing [worldX, worldY]
     */
    public double[] screenToWorld(double screenX, double screenY) {
        double x = screenX;
        double y = screenY;
        
        // First reverse the rotation around canvas center
        if (rotationAngle != 0.0) {
            double centerX = canvasWidth / 2.0;
            double centerY = canvasHeight / 2.0;
            
            // Translate to origin (canvas center)
            double translatedX = x - centerX;
            double translatedY = y - centerY;
            
            // Apply inverse rotation
            double cos = Math.cos(-rotationAngle);
            double sin = Math.sin(-rotationAngle);
            double rotatedX = translatedX * cos - translatedY * sin;
            double rotatedY = translatedX * sin + translatedY * cos;
            
            // Translate back
            x = rotatedX + centerX;
            y = rotatedY + centerY;
        }
        
        // Now apply inverse of scale, zoom, offset, and pan
        double worldX = (x - offsetX - panX) / (scale * zoom);
        double worldY = ((canvasHeight - y) - offsetY - panY) / (scale * zoom);
        
        return new double[] { worldX, worldY };
    }

    /** Converts screen size to world size. */
    public double screenToWorldSize(double screenSize) {
        return screenSize / (scale * zoom);
    }

    /**
     * Returns the current rotation angle in radians.
     * 
     * @return Rotation angle in radians
     */
    public double getRotationAngle() {
        return rotationAngle;
    }

    /**
     * Returns the current rotation angle in degrees.
     * 
     * @return Rotation angle in degrees
     */
    public double getRotationAngleDegrees() {
        return Math.toDegrees(rotationAngle);
    }
}