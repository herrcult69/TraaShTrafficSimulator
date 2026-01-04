import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Abstract base class for all renderable and interactive simulation objects.
 * Defines methods for rendering, highlighting, and hit detection.
 *
 * @author M A T^2 H Team
 * @version 2.0
 * @see Vehicle
 * @see Edge
 */
public abstract class Renderable {
    
    /**
     * Renders this object on the canvas.
     * 
     * @param g The graphics context to draw on
     * @param transform The coordinate transformation
     */
    public abstract void render(GraphicsContext g, CoordinateTransform transform);
    
    /**
     * Draws a visual highlight around this object.
     * 
     * @param g The graphics context to draw on
     * @param transform The coordinate transformation
     * @param color The highlight color
     */
    public abstract void highlight(GraphicsContext g, CoordinateTransform transform, Color color);
    
    /**
     * Checks if the given screen coordinates fall within this object's clickable area.
     * 
     * @param screenX The X coordinate in screen space
     * @param screenY The Y coordinate in screen space
     * @param transform The coordinate transformation
     * @return true if the point is within this object's bounds
     */
    public abstract boolean contains(double screenX, double screenY, CoordinateTransform transform);
}
