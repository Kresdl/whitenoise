package kresdl.whitenoise.node.composite;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;
import kresdl.gradienteditor.GradientEditor;
import kresdl.utilities.Gradient;
import kresdl.whitenoise.node.Node;

@SuppressWarnings("serial")
public final class Controls extends Container {

    private final class Distribution extends JSlider {

        private Distribution(Node node) {
            setPreferredSize(new Dimension(100, 16));
            setMinimum(0);
            setMaximum(100);
            setAlignmentY(0.45f);
            setValue(0);
            setOpaque(false);
            setValue((int) (far * 10));

            addChangeListener(e -> {
                if (!Node.isBusy()) {
                    far = getValue() * 0.1d;
                    node.fireParameterChange();
                }
            });            
        }
    }

    private final GradientEditor ge;
    private double far;

    public Controls(Composite node, Gradient g, double distribution) {
        super();
        Gradient gradient = g != null
                ? g
                : new Gradient(Color.black, null, Color.white);
        far = distribution;

        ge = GradientEditor.create(200, gradient);
        ge.addPropertyChangeListener(GradientEditor.GRADIENT_PROPERTY, e -> {
            if (!Node.isBusy()) {
                node.fireParameterChange();
            }
        });

        Box dist = new Box(BoxLayout.X_AXIS);
        Distribution s = new Distribution(node);
        JLabel lb = new JLabel("Distribution:");
        dist.add(lb);
        dist.add(s);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(ge);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(dist);
    }

    public double getAlpha(double x) {
        x = Math.max(x, 0);
        double w = 1 + x * far;
        return Math.min(x / w, 1);
    }

    public GradientEditor getGradientEditor() {
        return ge;
    }

    public double getFar() {
        return far;
    }    
}
