package kresdl.whitenoise.node;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import kresdl.utilities.Mouse;
import kresdl.whitenoise.Main;
import kresdl.whitenoise.node.composite.Composite;
import kresdl.whitenoise.node.perlin.Perlin;
import kresdl.whitenoise.socket.In;
import kresdl.whitenoise.socket.Out;
import kresdl.whitenoise.socket.Socket;

@SuppressWarnings("serial")
public abstract class Node extends Container {

    public static abstract class Info implements Serializable {

        public int x, y;

        public Info(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    protected int x, y;
    private JLabel name;
    private final Main main;
    protected In[] in;
    protected Component[] controls;
    protected Out out;
    private boolean valid;
    private final Mouse mouse = new Mouse(this);
    private final JPopupMenu popup = new JPopupMenu();
    private static Socket activeSocket;

    public Node(String name, int x, int y, Main main) {
        super();
        if (name != null) {
            this.name = new JLabel(name);
        }
        this.x = x;
        this.y = y;
        this.main = main;
    }

    public void delete() {
        if (activeSocket != null && activeSocket.getNode() == Node.this) {
            activeSocket = null;
        }
        unlink();
        main.removeNode(Node.this);
        validateNodes();
        Composite c = getTop();
        if (c != null) {
            c.fireStructureChange();
        }
    }

    private void popup(MouseEvent e) {
        popup.show(this, e.getX(), e.getY());
    }

    private void release(MouseEvent e) {
        x = Math.max(getX(), 0);
        y = Math.max(getY(), 0);
    }

    private void drag(MouseEvent e) {
        Point m = mouse.getMovement();
        x += m.x;
        y += m.y;
        setLocation(Math.max(x, 0), Math.max(y, 0));
        updateBounds();
        main.updateSize();
        main.repaint();
    }

    protected void init() {
        setLocation(new Point(x, y));
        
        popup.add(new JMenuItem(new AbstractAction("Delete") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!Node.isBusy()) {
                    delete();
                }
            }
        }));
        
        add(popup);
        
        mouse.onDrag(0, this::drag);
        mouse.onRelease(0, this::release);
        mouse.onPress(2, this::popup);

        GridBagLayout g = new GridBagLayout();
        setLayout(g);
        GridBagConstraints b = new GridBagConstraints();
        b.gridx = 1;
        b.gridy = 0;
        b.gridwidth = 1;
        b.gridheight = 1;
        b.insets = new Insets(0, 8, 0, 8);
        add(name, b);
        b.insets = new Insets(0, 0, 0, 0);

        if (in != null) {
            b.gridx = 0;
            add(in[0], b);
            for (int i = 1; i < in.length; i++) {
                b.gridy = i;
                add(in[i], b);
            }
        }

        if (controls != null) {
            b.gridx = 1;
            for (int i = 0; i < controls.length; i++) {
                if (controls[i] != null) {
                    b.gridy = i + 1;
                    add(controls[i], b);
                }
            }
        }

        if (out != null) {
            b.gridx = 2;
            b.gridy = 0;
            add(out, b);
        }

        updateBounds();
    }

    public void updateBounds() {
        Dimension size = getPreferredSize();
        setBounds(getX(), getY(), size.width, size.height);
        revalidate();
    }

    boolean hasData() {
        return Stream.of(in).allMatch(In::hasData);
    }

    public Main getMain() {
        return main;
    }

    In[] getIn() {
        return in;
    }

    Out getOut() {
        return out;
    }

    protected Set<Perlin> getRoots(Set<Perlin> p) {
        if (!(this instanceof Perlin)) {
            if (in != null) {
                Stream.of(in).filter(In::isLinked).forEach(x -> {
                    x.getLink().getNode().getRoots(p);            
                });
            }
        } else {
            p.add((Perlin) this);
        }
        return p;
    }

    public static boolean isBusy() {
        for (Node x : Main.getNodes()) {
            if (x instanceof View) {
                View v = (View) x;
                if (v.isLocked()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void validateNodes() {
        Main.getNodes().stream()
                .forEach(Node::setInvalid);

        Main.getNodes().stream()
                .filter(x -> x instanceof Perlin)
                .forEach(Node::validateUp);

        Main.getNodes().stream()
                .filter(x -> x instanceof Composite)
                .forEach(Node::validateDown);
    }

    public boolean fireParameterChange() {
        if (valid) {
            getTop().lock();
            Main.getTaskManager().execute(this::render);
            return true;
        }
        return false;
    }

    private void validateUp() {
        if (!(this instanceof Composite)) {
            if (hasRoots()) {
                if (out.isLinked()) {
                    In x = out.getLink();
                    x.setValid();
                    x.getNode().validateUp();
                }
            }
        }
    }

    private void validateDown() {
        if (!(this instanceof Perlin)) {
            if (hasRoots()) {
                valid = true;
                for (In x : in) {
                    if (x.isLinked()) {
                        Node n = x.getLink().getNode();
                        if (!n.valid) {
                            n.validateDown();
                        }
                    }
                }
            }
        } else {
            valid = true;
        }
    }

    protected void empty() {
        if (in != null) {
            for (In x : in) {
                x.empty();
            }
        }
    }

    public void emptyDown() {
        if (in != null) {
            for (In x : in) {
                x.empty();
                if (x.isLinked()) {
                    x.getLink().getNode().emptyDown();
                }
            }
        }
    }

    public Composite getTop() {
        if (!(this instanceof Composite)) {
            if (out.isLinked()) {
                return out.getLink().getNode().getTop();
            } else {
                return null;
            }
        }
        return (Composite) this;
    }

    public void collect(List<Node.Info> info, List<Integer> c) {
        if (in != null) {
            for (In x : in) {
                if (x.valid()) {
                    c.add(info.size());
                    Node n = x.getLink().getNode();
                    info.add(n.getInfo());
                    n.collect(info, c);
                } else {
                    c.add(null);
                }
            }
        }
    }

    public void connect(List<Node> nodes, Iterator<Integer> c) {
        if (in != null) {
            for (In x : in) {
                Integer i = c.next();
                if (i != null) {
                    Node n = nodes.get(i);
                    Out y = n.getOut();
                    y.setLink(x);
                    Socket.addConnection(y, x);
                    n.connect(nodes, c);
                }
            }
        }
    }

    private boolean hasRoots() {
        if (in != null) {
            for (In x : in) {
                if (!x.hasSource()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean valid() {
        return valid;
    }

    void setInvalid() {
        valid = false;

        if (in != null) {
            for (In x : in) {
                x.setInvalid();
            }
        }
    }

    void unlink() {
        if (in != null) {
            for (In x : in) {
                if (x.isLinked()) {
                    x.unlink();
                }
            }
        }
        if (out != null) {
            if (out.isLinked()) {
                out.unlink();
            }
        }
    }

    public static void setActiveSocket(Socket s) {
        activeSocket = s;
    }

    public static Socket getActiveSocket() {
        return activeSocket;
    }

    public abstract void render();

    public abstract void save();

    public abstract void cubic(int z, Path d) throws IOException;

    public abstract Info getInfo();    
}
