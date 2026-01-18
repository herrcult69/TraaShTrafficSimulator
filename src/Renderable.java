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
     * @param g Graphics context
     * @param transform Coordinate transform
     */
    public abstract void render(GraphicsContext g, CoordinateTransform transform);

    /**
     * Draws a visual highlight around this object.
     * 
     * @param g Graphics context
     * @param transform Coordinate transform
     * @param color Highlight color
     */
    public abstract void highlight(GraphicsContext g, CoordinateTransform transform, Color color);

    /**
     * Checks if screen coordinates fall within this object.
     * 
     * @param screenX X coordinate in screen space
     * @param screenY Y coordinate in screen space
     * @param transform Coordinate transform
     * @return True if point is within bounds
     */
    public abstract boolean contains(double screenX, double screenY, CoordinateTransform transform);
}
