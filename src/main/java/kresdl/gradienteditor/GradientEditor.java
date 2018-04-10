package kresdl.gradienteditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import javax.swing.BoxLayout;

import kresdl.utilities.Gradient;
import kresdl.xpanel.CompositeXPanel;

public class GradientEditor extends CompositeXPanel {

    public static final long serialVersionUID = 1L;
    public static final String GRADIENT_PROPERTY = "gradient";

    private final Values values;
    private final Keys keys;
    private Gradient masterGradient;
    private final Gradient gradient;
    private double key;
    private int width;

    /**
     * Creates a gradient editor
     *
     * @param w width in pixels
     * @param gradient initializing gradient
     * @return gradient editor
     */
    public static GradientEditor create(int w, Gradient gradient) {
        GradientEditor e = new GradientEditor(w, gradient);
        e.init(w);

        e.lock();
        new Thread(() -> {
            e.redraw();
            e.unlock();
        }).start();

        return e;
    }

    private GradientEditor(int w, Gradient gr) {
        width = w;
        masterGradient = gr != null
                ? new Gradient(gr)
                : new Gradient(Color.black, null, Color.white);

        gradient = new Gradient(masterGradient);

        values = new Values(this, w);
        keys = new Keys(this, w + 8);

        Dimension v = new Dimension(w, 16);
        values.setPreferredSize(v);
        values.setMaximumSize(v);
        keys.setPreferredSize(new Dimension(w + 8, 8));
    }

    private void init(int width) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(values);
        add(keys);
        setPreferredSize(new Dimension(width + 8, 24));
    }

    void newKey(MouseEvent e) {
        if (lock()) {
            double k = values.getKey(e);
            new Thread(() -> {
                gradient.set(k, gradient.get(k));
                key = k;
                redraw();
                setGradient(gradient);
                unlock();
            }).start();
        }
    }

    void moveColor(MouseEvent e) {
        if ((key > 0.0d) && (key < 1.0d)) {
            if (lock()) {
                double k = values.getKey(e);
                if (!gradient.contains(k)) {
                    new Thread(() -> {
                        gradient.set(k, gradient.delete(key));
                        key = k;
                        redraw();
                        setGradient(gradient);
                        unlock();
                    }).start();
                    return;
                }
                unlock();
            }
        }
    }

    void newColor(MouseEvent e) {
        chooseColor(values.getKey(e), true);
    }

    void activeKey(MouseEvent e) {
        key = cloneGradient().getClosest(keys.getKey(e)).key;
    }

    void deleteEntry(MouseEvent e) {
        if (lock()) {
            double k = gradient.getClosest(keys.getKey(e)).key;
            if (k >= 0.0d && k != 1.0d) {
                new Thread(() -> {
                    gradient.delete(k);
                    redraw();
                    setGradient(gradient);
                    unlock();
                }).start();
                return;
            }
            unlock();
        }
    }

    void changeColor(MouseEvent e) {
        chooseColor(key, false);
    }

    void changePos(MouseEvent e) {
        if (key >= 0.0d && key != 1.0d) {
            if (lock()) {
                double k = keys.getKey(e);
                if (!gradient.contains(k)) {
                    new Thread(() -> {
                        gradient.set(k, gradient.delete(key));
                        key = k;
                        redraw();
                        setGradient(gradient);
                        unlock();
                    }).start();
                    return;
                }
                unlock();
            }
        }
    }

    synchronized Gradient getGradient() {
        return gradient;
    }

    public synchronized void setGradient(Gradient g) {
        masterGradient = g;
        firePropertyChange(GRADIENT_PROPERTY, null, g);
    }

    /**
     * Get gradient copy.
     *
     * @return gradient copy
     */
    public synchronized Gradient cloneGradient() {
        return new Gradient(masterGradient);
    }

    private void chooseColor(double key, boolean add) {
        Color old = gradient.get(key);

        Color color = Palette.show(values, -80, 16, old, e -> {
            Color c = (Color) e.getNewValue();
            if (c != null) {
                if (lock()) {
                    new Thread(() -> {
                        gradient.set(key, c);
                        redraw();
                        setGradient(gradient);
                        unlock();
                    }).start();
                }
            }
        });

        if (color == null) {
            lock();
            new Thread(() -> {
                if (add) {
                    gradient.delete(key);
                } else {
                    gradient.set(key, old);
                }
                redraw();
                setGradient(gradient);
                unlock();
            }).start();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width + 8, 24);
    }
    
    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public byte[] getArray() {
        return values.getArray();
    }
    
    public int getArrayOffset(double p) {
        byte[] array = values.getArray();
        return (int) (p * (array.length / 3 - 1)) * 3;
    }
}
