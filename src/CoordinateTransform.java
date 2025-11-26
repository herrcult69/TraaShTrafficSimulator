/**
 * Handles coordinate transformations between SUMO world space (Y-up, meters)
 * and JavaFX screen space (Y-down, pixels). Applies scaling and offsets.
 */
public class CoordinateTransform {
    private double scale, offsetX, offsetY;
    private double canvasHeight;

    public CoordinateTransform(double canvasHeight) {
        this.canvasHeight = canvasHeight;
        this.scale = 1.0;
    }

    public void updateTransform(double scale, double offsetX, double offsetY) {
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public double worldToScreenX(double worldX) {
        return (worldX * scale) + offsetX;
    }

    public double worldToScreenY(double worldY) {
        return canvasHeight - ((worldY * scale) + offsetY);
    }

    public double worldToScreenSize(double worldSize) {
        return worldSize * scale;
    }
}