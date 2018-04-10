package kresdl.xpanel;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Panel backed with two buffered images for double buffering purposes. Back
 * buffer is accessed through user implemented method drawImage(BufferedImage
 * img).
 */
@SuppressWarnings("serial")
abstract public class XPanel extends AbstractXPanel {

    private BufferedImage img1, img2;

    /**
     * Constructs an XPanel with given width, height and image type.
     *
     * @param w width in pixels
     * @param h height in pixels
     * @param imageType image type
     */
    public XPanel(int w, int h, int imageType) {
        super(w, h);
        img1 = new BufferedImage(w, h, imageType);
        img2 = new BufferedImage(w, h, imageType);
    }

    @Override
    public void redraw() {
        drawImage();
        swapBuffers();
        repaint();
    }

    @Override
    public void drawImage() {
        drawImage(img1, null);
    }

    @Override
    public void redraw(Rectangle r) {
        drawImage(r);
        swapBuffers();
        repaint(r);
    }

    @Override
    public void drawImage(Rectangle r) {
        drawImage(img1, r);
    }

    /**
     * Draw image. User implementation of drawing to back buffer image.
     *
     * @param img Back buffer image
     * @param r clip bounds
     */
    abstract public void drawImage(BufferedImage img, Rectangle r);

    @Override
    public void swapBuffers() {
        BufferedImage t2 = img1;
        img1 = img2;
        img2 = t2;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth();
        int h = getHeight();
        Insets s = getInsets();
        g2.setComposite(AlphaComposite.SrcOver);
        g2.drawImage(img2, s.left, s.top, w - s.right - s.left, h - s.bottom - s.top, null);
    }
}
