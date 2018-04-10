package kresdl.xpanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

/**
 * Container for multiple X panels.
 */
@SuppressWarnings("serial")
public class CompositeXPanel extends Container implements X {

    private final Set<AbstractXPanel> children = new HashSet<>();

    /**
     * Constructs an empty CXPanel
     */
    public CompositeXPanel() {
        super();
    }

    @Override
    public boolean lock() {
        if (isLocked()) {
            return false;
        }
        children.stream().forEach(AbstractXPanel::lock);
        return true;
    }

    @Override
    public void unlock() {
        children.stream().forEach(AbstractXPanel::unlock);
    }

    @Override
    public boolean isLocked() {
        for (AbstractXPanel p : children) {
            if (p.isLocked()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void drawImage() {
        children.stream().forEach(AbstractXPanel::drawImage);
    }

    @Override
    public void redraw() {
        drawImage();
        swapBuffers();
        repaint();
    }

    @Override
    public void swapBuffers() {
        children.stream().forEach(AbstractXPanel::swapBuffers);
    }

    @Override
    public void removeAll() {
        children.clear();
        super.removeAll();
    }

    @Override
    public void remove(Component c) {
        if (c instanceof AbstractXPanel) {
            children.remove((AbstractXPanel) c);
        }
        super.remove(c);
    }

    @Override
    public Component add(Component c) {
        if (c instanceof AbstractXPanel) {
            children.add((AbstractXPanel) c);
        }
        return super.add(c);
    }
}
