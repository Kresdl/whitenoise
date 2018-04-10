package kresdl.whitenoise.node.perlin;

import java.awt.Container;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import kresdl.whitenoise.node.Node;

@SuppressWarnings("serial")
final class Controls extends Container {

    private final Output noise;

    private final class Detail extends JSlider implements ChangeListener {

        private Detail(int w) {
            super();
            setOpaque(false);
            setPreferredSize(new Dimension(w, 16));
            setMinimum(1);
            setMaximum(Perlin.getRes() - 1);
            setAlignmentY(0.45f);
            setValue((int) (Math.sqrt(noise.getDetail() / 0.8d) * Perlin.getRes()));
            addChangeListener(this);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (!Node.isBusy()) {
                double d = (double) getValue() / Perlin.getRes();
                noise.setDetail(d * d * 0.8d);
                noise.getNode().fireChange();
            }
        }
    }

    private final class Power extends JSlider implements ChangeListener{

        private Power(int w) {
            super();
            setOpaque(false);
            setPreferredSize(new Dimension(w, 16));
            setMinimum(1);
            setMaximum(5 * Perlin.getRes() - 1);
            setAlignmentY(0.45f);
            setValue((int) (Math.sqrt(noise.getPower() / 0.8d) * Perlin.getRes()));
            addChangeListener(this);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (!Node.isBusy()) {
                double d = (double) getValue() / Perlin.getRes();
                noise.setPower(d * d * 0.8d);
                noise.getNode().fireChange();
            }
        }
    }

    private final class Factor extends JSlider implements ChangeListener {

        private Factor(int w) {
            super();
            setOpaque(false);
            setPreferredSize(new Dimension(w, 16));
            setMinimum(1);
            setMaximum(10 * Perlin.getRes() - 1);
            setAlignmentY(0.45f);
            setValue((int) (Math.sqrt(noise.getFactor() / 0.8d) * Perlin.getRes()));
            addChangeListener(this);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (!Node.isBusy()) {
                double d = (double) getValue() / Perlin.getRes();
                noise.setFactor(d * d * 0.8d);
                noise.getNode().fireChange();
            }
        }
    }

    Controls(Output n) {
        super();
        noise = n;
        Detail detail = new Detail(n.getPreferredSize().width);
        Power power = new Power(n.getPreferredSize().width);
        Factor factor = new Factor(n.getPreferredSize().width);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Box b1 = new Box(BoxLayout.X_AXIS);
        b1.add(new JLabel("s:"));
        b1.add(detail);
        Box b2 = new Box(BoxLayout.X_AXIS);
        b2.add(new JLabel("q:"));
        b2.add(factor);
        Box b3 = new Box(BoxLayout.X_AXIS);
        b3.add(new JLabel("p:"));
        b3.add(power);
        add(noise);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(b1);
        add(b2);
        add(b3);
    }

    Output getNoise() {
        return noise;
    }
}
