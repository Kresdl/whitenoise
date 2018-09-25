package com.kresdl.whitenoise;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import com.kresdl.geometry.Vec2;
import com.kresdl.utilities.Gradient;
import com.kresdl.utilities.Misc;
import com.kresdl.utilities.Mouse;
import com.kresdl.utilities.TaskManager;
import com.kresdl.whitenoise.node.Add;
import com.kresdl.whitenoise.node.Interpolate;
import com.kresdl.whitenoise.node.Invert;
import com.kresdl.whitenoise.node.Mad;
import com.kresdl.whitenoise.node.Mask;
import com.kresdl.whitenoise.node.Max;
import com.kresdl.whitenoise.node.Min;
import com.kresdl.whitenoise.node.Mul;
import com.kresdl.whitenoise.node.Node;
import com.kresdl.whitenoise.node.Power;
import com.kresdl.whitenoise.node.Sub;
import com.kresdl.whitenoise.node.Transform;
import com.kresdl.whitenoise.node.composite.Composite;
import com.kresdl.whitenoise.node.composite.Output;
import com.kresdl.whitenoise.node.composite.Output.Mode;
import com.kresdl.whitenoise.node.perlin.Perlin;
import com.kresdl.whitenoise.socket.In;
import com.kresdl.whitenoise.socket.Out;
import com.kresdl.whitenoise.socket.Socket;

@SuppressWarnings("serial")
public final class Main extends JPanel {

    class About extends AbstractAction {

        About() {
            super("About");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                JTextPane p = new JTextPane();
                p.setEditable(false);
                p.setOpaque(false);
                p.setPreferredSize(new Dimension(300, 120));
                p.setPage(getClass().getClassLoader().getResource("about.html"));
                JOptionPane.showMessageDialog(getTopLevelAncestor(), p, "About", JOptionPane.PLAIN_MESSAGE);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    class LoadTree extends AbstractAction {

        LoadTree() {
            super("Load tree");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!Node.isBusy()) {
                JFileChooser f = new JFileChooser();
                f.setDialogTitle("Load tree");
                f.setFileFilter(new FileNameExtensionFilter("JSON", "json"));
                if (f.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File file = f.getSelectedFile();
                    try {
                        loadTree(new FileInputStream(file));
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    class Clear extends AbstractAction {

        Clear() {
            super("Clear");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            clear();
        }
    }

    private abstract static class AddNode extends AbstractAction implements Runnable {

        AddNode(String s) {
            super(s);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!Node.isBusy()) {
                run();
            }
        }
    }

    private class AddSPerlin extends AddNode {

        AddSPerlin() {
            super("Simple Perlin noise");
        }

        @Override
        public void run() {
            addNode(Perlin.create(pl.x, pl.y, Main.this, 0.1d, new Vec2(), 1, 1, null));
        }
    }

    private class AddInterpolate extends AddNode {

        AddInterpolate() {
            super("Interpolate");
        }

        @Override
        public void run() {
            addNode(Interpolate.create(pl.x, pl.y, Main.this, 0.5d));
        }
    }

    private class AddMask extends AddNode {

        AddMask() {
            super("Mask");
        }

        @Override
        public void run() {
            addNode(Mask.create(pl.x, pl.y, Main.this));
        }
    }

    private class AddInvert extends AddNode {

        AddInvert() {
            super("Invert");
        }

        @Override
        public void run() {
            addNode(Invert.create(pl.x, pl.y, Main.this, 1));
        }
    }

    private class AddMax extends AddNode {

        AddMax() {
            super("Max");
        }

        @Override
        public void run() {
            addNode(Max.create(pl.x, pl.y, Main.this, 0.5d));
        }
    }

    private class AddMin extends AddNode {

        AddMin() {
            super("Min");
        }

        @Override
        public void run() {
            addNode(Min.create(pl.x, pl.y, Main.this, 0.5d));
        }
    }

    private class AddMul extends AddNode {

        AddMul() {
            super("Mul");
        }

        @Override
        public void run() {
            addNode(Mul.create(pl.x, pl.y, Main.this, 1));
        }
    }

    private class AddPower extends AddNode {

        AddPower() {
            super("Power");
        }

        @Override
        public void run() {
            addNode(Power.create(pl.x, pl.y, Main.this, 1));
        }
    }

    private class AddSub extends AddNode {

        AddSub() {
            super("Sub");
        }

        @Override
        public void run() {
            addNode(Sub.create(pl.x, pl.y, Main.this, 0));
        }
    }

    private class AddAdd extends AddNode {

        AddAdd() {
            super("Add");
        }

        @Override
        public void run() {
            addNode(Add.create(pl.x, pl.y, Main.this, 0));
        }
    }

    private class AddMad extends AddNode {

        AddMad() {
            super("Mul add");
        }

        @Override
        public void run() {
            addNode(Mad.create(pl.x, pl.y, Main.this));
        }
    }

    private class AddTransform extends AddNode {

        AddTransform() {
            super("Transform");
        }

        @Override
        public void run() {
            addNode(Transform.create(pl.x, pl.y, Main.this, 1.0d, 1.0d, 0.0d));
        }
    }

    private class AddOutput extends AddNode {

        AddOutput() {
            super("Output");
        }

        @Override
        public void run() {
            addNode(Composite.create(pl.x, pl.y, Main.this, null, 0, Mode.BW));
        }
    }

    private final Mouse mouse = new Mouse(this);
    private Point pl;
    private static final Set<Node> NODES = new HashSet<>();
    private static final TaskManager TASKMANAGER = new TaskManager(Executors.newCachedThreadPool(r -> {
       Thread t = new Thread(r);
       t.setDaemon(true);
       return t;
    }));


    Main() {
        super();
        JPopupMenu p = new JPopupMenu("Add node");
        p.add(new JMenuItem(new AddSPerlin()));
        p.add(new JMenuItem(new AddOutput()));
        p.add(new JMenuItem(new AddMask()));
        p.add(new JMenuItem(new AddInterpolate()));
        p.add(new JMenuItem(new AddTransform()));
        p.add(new JMenuItem(new AddMax()));
        p.add(new JMenuItem(new AddMin()));
        p.add(new JMenuItem(new AddAdd()));
        p.add(new JMenuItem(new AddSub()));
        p.add(new JMenuItem(new AddMul()));
        p.add(new JMenuItem(new AddPower()));
        p.add(new JMenuItem(new AddMad()));
        p.add(new JMenuItem(new AddInvert()));

        mouse.onPress(2, e -> {
            pl = e.getPoint();
            p.show(this, e.getX(), e.getY());
        });

        setLayout(null);
        setPreferredSize(new Dimension(App.SCROLLWIDTH, App.SCROLLHEIGHT));
        setBackground(App.BGCOLOR);
        
        loadTree(getClass().getClassLoader().getResourceAsStream("tree.json"));
    }

    Mouse getMouse() {
        return mouse;
    }

    public static Set<Node> getNodes() {
        return NODES;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.black);

        Point p = getLocationOnScreen();
        Set<Out> out = Socket.getConnections().keySet();
        int np = App.BEZIERCURVEPOINTS;
        Point[] c = new Point[4], q = new Point[np + 2];

        for (Out x : out) {
            In y = x.getLink();
            Point n1 = x.getNode().getLocation();
            Point n2 = y.getNode().getLocation();
            Point a = new Point(x.getX() + n1.x, x.getY() + n1.y);
            Point b = new Point(y.getX() + n2.x, y.getY() + n2.y);
            c[0] = a;
            int t = a.x + (b.x - a.x) / 2;
            c[1] = new Point(t, a.y);
            c[2] = new Point(t, b.y);
            c[3] = b;

            Misc.bezier(c, np, q);

            for (int i = 0; i < (np + 1); i++) {
                a = q[i];
                b = q[i + 1];
                g2.drawLine(a.x + 8, a.y + 8, b.x + 8, b.y + 8);
            }
        }
    }

    public void updateSize() {
        JViewport vp = (JViewport) getParent();
        JScrollPane sp = (JScrollPane) vp.getParent();

        int xmax = sp.getHorizontalScrollBar().getValue() + vp.getWidth();
        int ymax = sp.getVerticalScrollBar().getValue() + vp.getHeight();

        Iterator<Node> t = NODES.iterator();
        while (t.hasNext()) {
            Node c = t.next();
            xmax = Math.max(c.getX() + c.getWidth(), xmax);
            ymax = Math.max(c.getY() + c.getWidth(), ymax);
        }
        xmax = App.SCROLLWIDTH * (int) Math.ceil((double) xmax / App.SCROLLWIDTH);
        ymax = App.SCROLLHEIGHT * (int) Math.ceil((double) ymax / App.SCROLLHEIGHT);

        Dimension d = getSize();
        if ((xmax != d.width) || (ymax != d.height)) {
            setPreferredSize(new Dimension(xmax, ymax));
        }
    }

    public void addNode(Node node) {
        add(node);
        NODES.add(node);
        updateSize();
        node.revalidate();
        repaint();
    }

    public void removeNode(Node node) {
        remove(node);
        NODES.remove(node);
        updateSize();
        revalidate();
        repaint();
    }

    public void clear() {
        if (!Node.isBusy()) {
            int opt = JOptionPane.showConfirmDialog(null, "Clear panel?", "Clear", JOptionPane.YES_NO_OPTION);
            if (opt == 0) {
                NODES.stream().forEach(this::remove);
                NODES.clear();
                Socket.getConnections().clear();
                Node.setActiveSocket(null);
                updateSize();
                revalidate();
                repaint();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void loadTree(InputStream json) {
        List<Node> nodes = parseTree(json);

        TASKMANAGER.execute(() -> {
            nodes.stream().forEach(x -> {
                if (x instanceof Perlin) {
                    ((Perlin) x).getNoise().init();
                }
            });
            NODES.addAll(nodes);
            Node.validateNodes();

            SwingUtilities.invokeLater(() -> {
                nodes.stream().forEach(this::add);
                updateSize();
                revalidate();
                repaint();
                Composite c = (Composite) nodes.get(0);
                c.fireStructureChange();
            });
        });
    }

    private List<Node> parseTree(InputStream json) {
        Scanner all = new Scanner(json);
        List<Node> nodes = new ArrayList<>();
        String nl;
        for (;;) {
            nl = all.nextLine();
            if (nl.matches("\"connections\".+")) {
                break;
            }
            Scanner line = new Scanner(nl);
            String t = type(line);

            int x = integer(line);
            int y = integer(line);
            double f;
            Matcher m;

            switch (t) {
                case "Composite":
                    f = dbl(line);
                    Output.Mode mode = mode(line);
                    List<Gradient.Entry> e = new ArrayList<>();
                    m = Pattern.compile("\"(-?\\d\\.\\d+)\":\\[\"(\\d+)\",\"(\\d+)\",\"(\\d+)\"\\]").matcher(grad(line));
                    while (m.find()) {
                        double key = Double.parseDouble(m.group(1));
                        int r = Integer.parseInt(m.group(2));
                        int g = Integer.parseInt(m.group(3));
                        int b = Integer.parseInt(m.group(4));
                        Gradient.Entry q = new Gradient.Entry(key, new Color(r, g, b));
                        e.add(q);
                    }
                    Gradient g = new Gradient();
                    g.setStart(e.get(0).color);
                    int n = e.size();
                    for (int i = 1; i < n - 1; i++) {
                        Gradient.Entry q = e.get(i);
                        g.set(q.key, q.color);
                    }
                    g.setEnd(e.get(n - 1).color);
                    nodes.add(Composite.create(x, y, this, g, f, mode));
                    break;
                case "Perlin":
                    double d = dbl(line);
                    Vec2 offs = new Vec2(dbl(line), dbl(line));
                    double pow = dbl(line);
                    f = dbl(line);
                    List<Integer> perm = new ArrayList<>();
                    m = Pattern.compile("\"(\\d+)\"").matcher(perm(line));
                    while (m.find()) {
                        int q = Integer.parseInt(m.group(1));
                        perm.add(q);
                    }
                    int[] pm = new int[perm.size()];
                    for (int i = 0; i < pm.length; i++) {
                        pm[i] = perm.get(i);
                    }
                    nodes.add(Perlin.create(x, y, this, d, offs, pow, f, pm));
                    break;
                case "Transform":
                    nodes.add(Transform.create(x, y, this, dbl(line), dbl(line), dbl(line)));
                    break;
                case "Mad":
                    nodes.add(Mad.create(x, y, this));
                    break;
                case "Mask":
                    nodes.add(Mask.create(x, y, this));
                    break;
                case "Add":
                    nodes.add(Add.create(x, y, this, dbl(line)));
                    break;
                case "Sub":
                    nodes.add(Sub.create(x, y, this, dbl(line)));
                    break;
                case "Mul":
                    nodes.add(Mul.create(x, y, this, dbl(line)));
                    break;
                case "Power":
                    nodes.add(Power.create(x, y, this, dbl(line)));
                    break;
                case "Min":
                    nodes.add(Min.create(x, y, this, dbl(line)));
                    break;
                case "Max":
                    nodes.add(Max.create(x, y, this, dbl(line)));
                    break;
                case "Interpolate":
                    nodes.add(Interpolate.create(x, y, this, dbl(line)));
            }
        }

        List<Integer> c = new ArrayList<>();
        Matcher m = Pattern.compile("\"(\\w+)\"").matcher(nl.replace("\"connections\":", ""));
        while (m.find()) {
            String x = m.group(1);
            c.add(x.equals("null") ? null : Integer.valueOf(x));
        }
        Composite comp = (Composite) nodes.get(0);
        comp.connect(nodes.subList(1, nodes.size()), c.iterator());
        return nodes;
    }

    private String type(Scanner s) {
        if (s.findInLine(Pattern.compile(":\"(\\w+)\"")) == null) {
            return null;
        }
        String x = s.match().group(1);
        return x;
    }

    private Integer integer(Scanner s) {
        if (s.findInLine(Pattern.compile("\"(\\d+)\"")) == null) {
            return null;
        }
        String x = s.match().group(1);
        return Integer.valueOf(x);
    }

    private double dbl(Scanner s) {
        s.findInLine(Pattern.compile("\"(-?\\d+\\.\\d+)\""));
        String x = s.match().group(1);
        return Double.parseDouble(x);
    }

    private Output.Mode mode(Scanner s) {
        s.findInLine(Pattern.compile(":\"(\\w+)\""));
        String x = s.match().group(1);
        return Output.Mode.valueOf(x);
    }

    private String grad(Scanner s) {
        s.findInLine(Pattern.compile("\\{([^}]*)"));
        String x = s.match().group(1);
        return x;
    }

    private String perm(Scanner s) {
        s.findInLine(Pattern.compile("\\[([^\\]]*)"));
        String x = s.match().group(1);
        return x;
    }

    public static TaskManager getTaskManager() {
        return TASKMANAGER;
    }
}
