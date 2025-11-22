public class CoordinateTransform {
    private double scale, offsetX, offsetY, zoom, panX, panY;
    private double canvasHeight;
    
    public CoordinateTransform(double canvasHeight) {
        this.canvasHeight = canvasHeight;
        this.scale = 1.0;
        this.zoom = 1.0;
    }
    
    public void updateTransform(double scale, double offsetX, double offsetY, double zoom, double panX, double panY) {
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.zoom = zoom;
        this.panX = panX;
        this.panY = panY;
    }
    
    public double worldToScreenX(double worldX) {
        return (worldX * scale * zoom) + offsetX + panX;
    }
    
    public double worldToScreenY(double worldY) {
        return canvasHeight - ((worldY * scale * zoom) + offsetY + panY);
    }
    
    public double worldToScreenSize(double worldSize) {
        return worldSize * scale * zoom;
    }
    
    public double screenToWorldX(double screenX) {
        return (screenX - offsetX - panX) / (scale * zoom);
    }
    
    public double screenToWorldY(double screenY) {
        return ((canvasHeight - screenY) - offsetY - panY) / (scale * zoom);
    }
    
    public double screenToWorldSize(double screenSize) {
        return screenSize / (scale * zoom);
    }
}