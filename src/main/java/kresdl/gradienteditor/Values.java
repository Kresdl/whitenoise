package kresdl.gradienteditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import kresdl.utilities.Gradient;
import kresdl.utilities.Misc;
import kresdl.utilities.Mouse;
import kresdl.xpanel.XPanel;

final class Values extends XPanel {

    public static final long serialVersionUID = 1L;

    private final GradientEditor editor;
    private final Mouse m = new Mouse(this);
    private byte[] array;

    Values(GradientEditor e, int w) {
        super(w, 1, BufferedImage.TYPE_3BYTE_BGR);
        editor = e;

        m.onPress(0, e::newKey);
        m.onClick(0, e::newColor);
        m.onDrag(0, e::moveColor);
    }

    double getKey(MouseEvent e) {
        return Misc.sat((double) e.getPoint().x / getWidth());
    }

    public byte[] getArray() {
        return array;
    }

    @Override
    public void drawImage(BufferedImage img, Rectangle r) {
        
        int w = img.getWidth();
        double d = 1.0d / w;
        array = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        Gradient g = editor.getGradient();
        double p = 0;
        int t = 0;
        for (int x = 0; x < w; x++) {
            Color c = g.get(p);
            array[t++] = (byte) c.getBlue();
            array[t++] = (byte) c.getGreen();
            array[t++] = (byte) c.getRed();
            p += d;
        }
    }
}
