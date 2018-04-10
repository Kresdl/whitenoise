package kresdl.xpanel;

import java.awt.Dimension;
import javax.swing.JPanel;

@SuppressWarnings("serial")
abstract class AbstractXPanel extends JPanel implements X, XComponent {

    Dimension d = new Dimension();
    private volatile boolean locked;

    AbstractXPanel(int w, int h) {
        super();
        d.width = w;
        d.height = h;
    }

    @Override
    public boolean lock() {
        if (!locked) {
            locked = true;
            return true;
        }
        return false;
    }

    @Override
    public void unlock() {
        locked = false;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }
    
    /**
     * Get backing image size
     * @return image size
     */
    
    public Dimension getImageSize() {
        return d;
    }
}
