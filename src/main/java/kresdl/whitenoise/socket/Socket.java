package kresdl.whitenoise.socket;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;
import kresdl.utilities.Mouse;
import kresdl.whitenoise.node.Node;
import kresdl.whitenoise.node.composite.Composite;

@SuppressWarnings("serial")
public abstract class Socket extends JPanel {

    private final Mouse mouse = new Mouse(this);
    private final Node node;
    double[] data;
    private final static Map<Out, In> CONNECTIONS = new HashMap<>();

    public Socket(Node node) {
        this.node = node;
        setOpaque(false);
        setPreferredSize(new Dimension(16, 16));
        mouse.onPress(0, this::snap);
        mouse.onPress(2, this::unsnap);
    }

    private void snap(MouseEvent e) {
        if (!Node.isBusy()) {
            Socket c = Node.getActiveSocket();
            if (c != null) {
                if ((c.getClass() != getClass()) && (c.getNode() != node)) {
                    Set<Composite> s = new HashSet<>();
                    if (c.isLinked()) {
                        addTop(s, c.unlink().getNode());
                    }
                    if (isLinked()) {
                        addTop(s, unlink().getNode());
                    }
                    link();
                    addTop(s, node);
                    Node.validateNodes();
                    s.stream().forEach(Composite::fireStructureChange);
                    Node.setActiveSocket(null);
                    node.getMain().repaint();
                    return;
                }
                Node.setActiveSocket(null);
                c.repaint();
                return;
            }
            Node.setActiveSocket(this);
            repaint();
        }
    }

    private void addTop(Set<Composite> c, Node n) {
        Composite t = n.getTop();
        if (t != null) {
            c.add(t);
        }
    }

    private void unsnap(MouseEvent e) {
        if (!Node.isBusy()) {
            if (isLinked()) {
                In in = unlink();
                Node.validateNodes();
                Composite c = in.getNode().getTop();
                if (c != null) {
                    c.fireStructureChange();
                }
                node.getMain().repaint();
            }
        }
    }

    public Node getNode() {
        return node;
    }

    public static Map<Out, In> getConnections() {
        return CONNECTIONS;
    }

    public static void addConnection(Out out, In in) {
        CONNECTIONS.put(out, in);
    }

    public void link() {
        Socket a = Node.getActiveSocket();
        Out out;
        In in;
        if (a instanceof In) {
            out = (Out) this;
            in = (In) a;
        } else {
            out = (Out) a;
            in = (In) this;
        }
        out.setLink(in);
        CONNECTIONS.put(out, in);
    }

    public In unlink() {
        Out out;
        In in;
        if (this instanceof In) {
            in = (In) this;
            out = in.getLink();
        } else {
            out = (Out) this;
            in = out.getLink();
        }
        out.setLink(null);
        CONNECTIONS.remove(out);
        return in;
    }

    abstract public boolean isLinked();

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (Node.getActiveSocket() == this) {
            g2.setColor(Color.yellow);
        } else if (isLinked()) {
            g2.setColor(Color.white);
        } else {
            g2.setColor(Color.lightGray);
        }
        g2.fillOval(1, 1, 14, 14);
        g2.setColor(Color.black);
        g2.drawOval(1, 1, 14, 14);
    }
}
