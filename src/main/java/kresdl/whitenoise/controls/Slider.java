package kresdl.whitenoise.controls;

import java.awt.Dimension;
import java.io.Serializable;
import java.util.function.Supplier;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import kresdl.whitenoise.node.Node;
import kresdl.whitenoise.socket.In;

@SuppressWarnings("serial")
public final class Slider extends JSlider implements Supplier<Double>, Serializable, ChangeListener {

    Node node;
    In in;

    public static Slider crossRef(double value, double max, In in) {
        Slider s = new Slider(value, max, in);
        in.setDefault(s);
        return s;
    }
 
    private Slider(double value, double max, In in) {
        this(value, max, in.getNode());
        this.in = in;
    }
    
    public Slider(double value, double max, Node node) {
        super();
        this.node = node;

        setOpaque(false);
        setPreferredSize(new Dimension(90, 16));
        setMinimum(1);
        setMaximum((int) (max * 1000));
        setValue((int) (value * 1000));
        addChangeListener(this);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (!Node.isBusy()) {
            if ((in != null) && in.valid()) {
                return;
            }
            node.fireParameterChange();
        }
    }
    
    @Override
    public Double get() {
        return Double.valueOf(getValue()) / 1000;        
    }
}
