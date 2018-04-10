package kresdl.xpanel;

import java.awt.Rectangle;

/**
 * Intercace defining an XPanel unit.
 */
public interface XComponent {

    /**
     * Redraw. Convenience method for draw image, swap buffers and repaint.
     *
     * @param r clip bounds
     */
    public void redraw(Rectangle r);

    /**
     * Draw to back buffer.
     *
     * @param r clip bounds
     */
    public void drawImage(Rectangle r);
}
