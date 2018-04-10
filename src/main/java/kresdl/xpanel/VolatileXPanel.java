package kresdl.xpanel;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.VolatileImage;

/**
 * Panel backed with two volatile images for double buffering purposes. Back
 * buffer is accessed through user implemented method drawImage(VolatileImage
 * img).
 */
@SuppressWarnings("serial")
abstract public class VolatileXPanel extends AbstractXPanel {

    private VolatileImage img1, img2;

    /**
     * @param w width in pixels
     * @param h height in pixels
     */
    public VolatileXPanel(int w, int h) {
        super(w, h);
        GraphicsConfiguration gc = getGC();
        img1 = gc.createCompatibleVolatileImage(w, h, Transparency.TRANSLUCENT);
        img2 = gc.createCompatibleVolatileImage(w, h, Transparency.TRANSLUCENT);
    }

    @Override
    public void redraw() {
        drawImage();
        swapBuffers();
        repaint();
    }

    @Override
    public void drawImage() {
        roll();
        Graphics2D g = img1.createGraphics();
        drawImage(g);
        g.dispose();
    }

    @Override
    public void redraw(Rectangle r) {
        drawImage(r);
        swapBuffers();
        repaint(r);
    }

    @Override
    public void drawImage(Rectangle r) {
        roll();
        Graphics2D g = img1.createGraphics();
        g.setClip(r.x, r.y, r.width, r.height);
        drawImage(g);
        g.dispose();
    }

    /**
     * Draw image. User implementation of drawing to back buffer image.
     *
     * @param g back buffer graphics context
     */
    abstract public void drawImage(Graphics2D g);

    @Override
    public void swapBuffers() {
        VolatileImage t2 = img1;
        img1 = img2;
        img2 = t2;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Insets s = getInsets();
        int w = getWidth();
        int h = getHeight();
        g2.setComposite(AlphaComposite.SrcOver);
        g2.drawImage(img2, s.left, s.top, w - s.right - s.left, h - s.bottom - s.top, null);
    }

    private void roll() {
        GraphicsConfiguration gc = getGC();
        do {
            if (img1.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
                img1 = gc.createCompatibleVolatileImage(d.width, d.height, Transparency.TRANSLUCENT);
            }
        } while (img1.contentsLost());
    }

    private GraphicsConfiguration getGC() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }
}
