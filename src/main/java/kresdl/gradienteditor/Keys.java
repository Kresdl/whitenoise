package kresdl.gradienteditor;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import kresdl.utilities.Gradient;
import kresdl.utilities.Misc;
import kresdl.utilities.Mouse;
import kresdl.xpanel.VolatileXPanel;

final class Keys extends VolatileXPanel {

    public static final long serialVersionUID = 1L;
    private static BufferedImage cursor;
    private final GradientEditor editor;
    private final Mouse m = new Mouse(this);

    static {
        try {
            cursor = ImageIO.read(Keys.class.getClassLoader().getResourceAsStream("pos.png"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    Keys(GradientEditor e, int w) {
        super(w, 8);
        setOpaque(false);
        editor = e;

        m.onPress(0, e::activeKey);
        m.onPress(2, e::deleteEntry);
        m.onClick(0, e::changeColor);
        m.onDrag(0, e::changePos);
    }

    double getKey(MouseEvent e) {
        return Misc.sat((double) e.getPoint().x / getWidth());
    }

    @Override
    public void drawImage(Graphics2D g2) {
        Gradient g = editor.getGradient();

        g2.setComposite(AlphaComposite.Clear);
        Dimension d = getImageSize();
        g2.fillRect(0, 0, d.width, d.height);        
        g2.setComposite(AlphaComposite.SrcOver);
        Set<Map.Entry<Double, Color>> s = g.entrySet();
        int w = getPreferredSize().width;

        s.forEach((e) -> {
            g2.drawImage(cursor, (int) (e.getKey() * (w - 8)), 0, null);
        });
    }    
}
