package kresdl.whitenoise.node.composite;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import kresdl.geometry.Vec;
import kresdl.utilities.Misc;
import kresdl.utilities.Mouse;
import kresdl.utilities.RBGroup;
import kresdl.whitenoise.App;
import kresdl.whitenoise.Main;
import kresdl.whitenoise.buffer.Buffer;
import kresdl.whitenoise.node.Node;
import kresdl.whitenoise.node.perlin.Perlin;
import kresdl.xpanel.XPanel;

@SuppressWarnings("serial")
public final class Output extends XPanel {

    public static enum Mode {
        BW, NORMAL, COLOR, BUMP, COLBUMP;
    }
    
    class Task implements Runnable {

        private final int i;

        Task(int i) {
            this.i = i;
        }

        @Override
        public void run() {
            Controls c = node.getControls();
            byte[] array = c.getGradientEditor().getArray();
            int res = (Perlin.getRes() - 1);
            int yn = res / App.PARALLELISM;
            int ys = i * yn;
            yn += ys;

            if (buildNormals) {
                int k = res * ys;
                Buffer p = (Buffer) node.getInput().getStream(i);
                for (int y = ys; y < yn; y++) {
                    for (int x = 0; x < PRE; x++) {
                        double v = p.get(x, y + 1);
                        double u = p.get(x + 1, y + 1) - v;
                        v = p.get(x, y) - v;
                        Vec t = Vec.nrm(Vec.cross(new Vec(0, v, 1), new Vec(1, u, 0)));
                        NRM_BUFFER[k++] = t;
                    }
                }
            } else if (lightMove) {
                int nk = res * ys;
                int k = 3 * nk;
                if (mode == Mode.BUMP) {
                    for (int y = ys; y < yn; y++) {
                        for (int x = 0; x < PRE; x++) {
                            double s = -Vec.dot(NRM_BUFFER[nk++], getLgtInst(x, y));
                            byte t = (byte) (255 * Misc.sat(s));
                            dst[k++] = t;
                            dst[k++] = t;
                            dst[k++] = t;
                        }
                    }
                } else {
                    Buffer p = (Buffer) node.getInput().getStream(i);
                    for (int y = ys; y < yn; y++) {
                        for (int x = 0; x < PRE; x++) {
                            double d = p.get(x, y);
                            double m = -Vec.dot(NRM_BUFFER[nk++], getLgtInst(x, y));
                            int i = c.getGradientEditor().getArrayOffset(c.getAlpha(d));
                            dst[k++] = (byte) (m * (0xff & array[i]));
                            dst[k++] = (byte) (m * (0xff & array[i + 1]));
                            dst[k++] = (byte) (m * (0xff & array[i + 2]));
                        }
                    }
                }
            } else {
                Buffer p = (Buffer) node.getInput().getStream(i);
                int k = 3 * res * ys;

                switch (mode) {
                    case NORMAL:
                        for (int y = ys; y < yn; y++) {
                            for (int x = 0; x < res; x++) {
                                double v = p.get(x, y + 1);
                                double u = p.get(x + 1, y + 1) - v;
                                v = p.get(x, y) - v;
                                Vec t = Vec.nrm(Vec.cross(new Vec(0, v, 1), new Vec(1, u, 0)));
                                dst[k++] = (byte) (255 * Misc.sat(0.5d * (t.y + 1.0d)));
                                dst[k++] = (byte) (255 * Misc.sat(0.5d * (t.z + 1.0d)));
                                dst[k++] = (byte) (255 * Misc.sat(0.5d * (t.x + 1.0d)));
                            }
                        }
                        break;
                    case BUMP:
                        for (int y = ys; y < yn; y++) {
                            for (int x = 0; x < res; x++) {
                                double v = p.get(x, y + 1);
                                double u = p.get(x + 1, y + 1) - v;
                                v = p.get(x, y) - v;
                                Vec t = Vec.nrm(Vec.cross(new Vec(0, v, 1), new Vec(1, u, 0)));
                                byte q = (byte) (255 * Misc.sat(-Vec.dot(t, getLgtInst(x, y))));
                                dst[k++] = q;
                                dst[k++] = q;
                                dst[k++] = q;
                            }
                        }
                        break;
                    case COLBUMP:
                        for (int y = ys; y < yn; y++) {
                            for (int x = 0; x < res; x++) {
                                double v = p.get(x, y + 1);
                                double u = p.get(x + 1, y + 1) - v;
                                v = p.get(x, y) - v;
                                Vec t = Vec.nrm(Vec.cross(new Vec(0, v, 1), new Vec(1, u, 0)));
                                double d = p.get(x, y);
                                double m = Misc.sat(-Vec.dot(t, getLgtInst(x, y)));
                                int i = c.getGradientEditor().getArrayOffset(c.getAlpha(d));
                                dst[k++] = (byte) (m * (0xff & array[i]));
                                dst[k++] = (byte) (m * (0xff & array[i + 1]));
                                dst[k++] = (byte) (m * (0xff & array[i + 2]));
                            }
                        }
                        break;
                    case BW:
                        for (int y = ys; y < yn; y++) {
                            for (int x = 0; x < res; x++) {
                                byte d = (byte) (255 * Misc.sat(p.get(x, y)));
                                dst[k++] = d;
                                dst[k++] = d;
                                dst[k++] = d;
                            }
                        }
                        break;
                    default:
                        for (int y = ys; y < yn; y++) {
                            for (int x = 0; x < res; x++) {
                                int i = c.getGradientEditor().getArrayOffset(c.getAlpha(p.get(x, y)));
                                dst[k++] = array[i];
                                dst[k++] = array[i + 1];
                                dst[k++] = array[i + 2];
                            }
                        }
                }
            }
        }

        private Vec getLgtInst(int x, int y) {
            return Vec.nrm(new Vec(x - lgt.x, -128, lgt.y - y));
        }
    }

    class SaveImage extends AbstractAction {

        SaveImage() {
            super("Save Image");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (node.valid() & !Node.isBusy()) {

                FlowLayout t = new FlowLayout(FlowLayout.LEADING);
                Box b = new Box(BoxLayout.Y_AXIS);
                JPanel p1 = new JPanel(t);
                JPanel p2 = new JPanel(t);
                JPanel p3 = new JPanel(t);
                JPanel p4 = new JPanel(t);
                JRadioButton r1 = new JRadioButton("256");
                JRadioButton r2 = new JRadioButton("512");
                JRadioButton r3 = new JRadioButton("1024");
                JRadioButton r4 = new JRadioButton("PNG");
                JRadioButton r5 = new JRadioButton("JPEG");
                JRadioButton r6 = new JRadioButton("BMP");
                p1.add(new JLabel("Resolution:"));
                p2.add(r1);
                p2.add(r2);
                p2.add(r3);
                p3.add(new JLabel("Compression:"));
                p4.add(r4);
                p4.add(r5);
                p4.add(r6);
                b.add(p1);
                b.add(p2);
                b.add(Box.createRigidArea(new Dimension(0, 10)));
                b.add(new JSeparator());
                b.add(Box.createRigidArea(new Dimension(0, 10)));
                b.add(p3);
                b.add(p4);
                b.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

                RBGroup g1 = new RBGroup();
                RBGroup g2 = new RBGroup();
                g1.add(r1);
                g1.add(r2);
                g1.add(r3);
                g2.add(r4);
                g2.add(r5);
                g2.add(r6);

                r2.setSelected(true);
                r4.setSelected(true);

                if (JOptionPane.showConfirmDialog(Output.this, b,
                        "Format", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    JFileChooser f = new JFileChooser();
                    f.setDialogTitle("Save image");
                    if (f.showSaveDialog(Output.this) == JFileChooser.APPROVE_OPTION) {
                        final File file = f.getSelectedFile();
                        final String format = g2.getActionCommand().toLowerCase();
                        if (file.getName().matches("[^.]*")) {
                            file.renameTo(new File(file.getAbsolutePath() + "." + format));
                        }

                        int r = Integer.valueOf(g1.getActionCommand());
                        lock();
                        Main.getTaskManager().execute(() -> {
                            Perlin.setRes(r + 1);
                            node.emptyDown();
                            node.saveImage(file, format);
                            Perlin.setRes(PRE + 1);
                            node.renderImage();
                        });
                    }
                }
            }
        }
    }

    class SaveTree extends AbstractAction {

        SaveTree() {
            super("Save tree");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (node.valid() & !Node.isBusy()) {
                JFileChooser f = new JFileChooser();
                f.setDialogTitle("Save tree");
                f.setFileFilter(new FileNameExtensionFilter("JSON", "json"));
                if (f.showSaveDialog(Output.this) == JFileChooser.APPROVE_OPTION) {
                    File file = f.getSelectedFile();
                    lock();
                    Main.getTaskManager().execute(() -> {
                        node.saveTree(file);
                        unlock();
                    });
                }
            }
        }
    }

    class SaveCube extends AbstractAction {

        SaveCube() {
            super("Save cube");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (node.valid() & !Node.isBusy() && mode == Mode.BW || mode == Mode.COLOR) {
                FlowLayout t = new FlowLayout(FlowLayout.LEADING);
                Box b = new Box(BoxLayout.Y_AXIS);
                JPanel p1 = new JPanel(t);
                JPanel p2 = new JPanel(t);
                JPanel p3 = new JPanel(t);
                JPanel p4 = new JPanel(t);
                JPanel p5 = new JPanel(t);
                JPanel p6 = new JPanel(t);
                JRadioButton r1 = new JRadioButton("128");
                JRadioButton r2 = new JRadioButton("256");
                JRadioButton r3 = new JRadioButton("512");
                JRadioButton r4 = new JRadioButton("PNG");
                JRadioButton r5 = new JRadioButton("JPEG");
                JRadioButton r6 = new JRadioButton("BMP");
                JRadioButton r7 = new JRadioButton("JAR");
                JRadioButton r8 = new JRadioButton("ZIP");
                p1.add(new JLabel("Resolution:"));
                p2.add(r1);
                p2.add(r2);
                p2.add(r3);
                p3.add(new JLabel("Compression:"));
                p4.add(r4);
                p4.add(r5);
                p4.add(r6);
                p5.add(new JLabel("Archiver:"));
                p6.add(r7);
                p6.add(r8);
                b.add(p1);
                b.add(p2);
                b.add(Box.createRigidArea(new Dimension(0, 10)));
                b.add(new JSeparator());
                b.add(Box.createRigidArea(new Dimension(0, 10)));
                b.add(p3);
                b.add(p4);
                b.add(Box.createRigidArea(new Dimension(0, 10)));
                b.add(new JSeparator());
                b.add(Box.createRigidArea(new Dimension(0, 10)));
                b.add(p5);
                b.add(p6);
                b.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

                RBGroup g1 = new RBGroup();
                RBGroup g2 = new RBGroup();
                RBGroup g3 = new RBGroup();
                g1.add(r1);
                g1.add(r2);
                g1.add(r3);
                g2.add(r4);
                g2.add(r5);
                g2.add(r6);
                g3.add(r7);
                g3.add(r8);

                r2.setSelected(true);
                r4.setSelected(true);
                r7.setSelected(true);

                if (JOptionPane.showConfirmDialog(Output.this, b,
                        "Format", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    JFileChooser f = new JFileChooser();
                    f.setDialogTitle("Texture archive");

                    String archiver = g3.getActionCommand().toLowerCase();
                    FileFilter filter = new FileNameExtensionFilter(
                            archiver.substring(0, 1).toUpperCase() + archiver.substring(1) + "-file",
                            archiver
                    );
                    f.setFileFilter(filter);

                    if (f.showSaveDialog(Output.this) == JFileChooser.APPROVE_OPTION) {
                        File file = f.getSelectedFile();
                        String format = g2.getActionCommand().toLowerCase();
                        if (file.getName().matches("[^.]*")) {
                            file.renameTo(new File(file.getAbsolutePath() + "." + archiver));
                        }

                        int res = Integer.valueOf(g1.getActionCommand());
                        node.saveCube(res, file, format, archiver);
                    }
                }
            }
        }
    }

    public static final int PRE = App.OUT_PRE;

    private final JPopupMenu pop = new JPopupMenu();
    private final Mouse mouse = new Mouse(this);
    private final Composite node;
    private static final Vec[] NRM_BUFFER = new Vec[PRE * PRE];
    private boolean lightMove, buildNormals;
    private Mode mode = Mode.BW;
    private Point lgt = new Point();
    private byte[] dst;
    private final Set<Runnable> tasks = new HashSet<>();
    private final AbstractAction saveImage = new SaveImage(),
            saveTree = new SaveTree(),
            saveCube = new SaveCube();

    public Output(Composite node, Mode mode) {
        super(PRE, PRE, BufferedImage.TYPE_3BYTE_BGR);
        this.node = node;
        this.mode = mode;

        setPreferredSize(new Dimension(PRE, PRE));
        setBackground(Color.black);

        pop.add(new JMenuItem(new AbstractAction("B/W") {
            @Override
            public void actionPerformed(ActionEvent e) {
                mode(Mode.BW);
            }
        }));

        pop.add(new JMenuItem(new AbstractAction("Color") {
            @Override
            public void actionPerformed(ActionEvent e) {
                mode(Mode.COLOR);
            }
        }));

        pop.add(new JMenuItem(new AbstractAction("Bump") {
            @Override
            public void actionPerformed(ActionEvent e) {
                mode(Mode.BUMP);
            }
        }));

        pop.add(new JMenuItem(new AbstractAction("Bump + Color") {
            @Override
            public void actionPerformed(ActionEvent e) {
                mode(Mode.COLBUMP);
            }
        }));

        pop.add(new JMenuItem(new AbstractAction("Normals") {
            @Override
            public void actionPerformed(ActionEvent e) {
                mode(Mode.NORMAL);
            }
        }));

        pop.addSeparator();

        pop.add(new JMenuItem(saveImage));
        pop.add(new JMenuItem(saveTree));
        pop.add(new JMenuItem(saveCube));

        updateSaveOptions();

        mouse.onPress(0, this::buildNormals);
        mouse.onDrag(0, this::moveLight);

        for (int i = 0; i < App.PARALLELISM; i++) {
            tasks.add(new Task(i));
        }
    }

    private void mode(Mode m) {
        if (node.valid() & !Node.isBusy()) {
            node.updateEditorVisibility(m);
            mode = m;
            updateSaveOptions();
            lock();
            Main.getTaskManager().execute(() -> {
                redraw();
                unlock();
            });
        }
    }

    Mode getMode() {
        return mode;
    }

    private void buildNormals(MouseEvent e) {
        if ((mode == Mode.BUMP) || (mode == Mode.COLBUMP)) {
            if (node.valid() & !Node.isBusy()) {
                lock();
                Main.getTaskManager().execute(() -> {
                    buildNormals = true;
                    Main.getTaskManager().distribute(tasks);
                    buildNormals = false;
                    unlock();
                });
            }
        }
    }

    private void moveLight(MouseEvent e) {
        if ((mode == Mode.BUMP) || (mode == Mode.COLBUMP)) {
            if (node.valid() & !Node.isBusy()) {
                lock();
                lgt = Misc.pointAdd(lgt, mouse.getMovement());
                Main.getTaskManager().execute(() -> {
                    lightMove = true;
                    redraw();
                    lightMove = false;
                    unlock();
                });
            }
        }
    }

    public final void updateSaveOptions() {
        if (node.valid()) {
            saveImage.setEnabled(true);
            saveTree.setEnabled(true);
            saveCube.setEnabled(mode == Mode.COLOR);
        } else {
            saveImage.setEnabled(false);
            saveTree.setEnabled(false);
            saveCube.setEnabled(false);
        }
    }

    public void setEnablePopup(boolean enabled) {
        setComponentPopupMenu(enabled ? pop : null);
    }

    public void build(byte[] dst) {
        this.dst = dst;
        Main.getTaskManager().distribute(tasks);
        this.dst = null;
    }

    @Override
    public void drawImage(BufferedImage img, Rectangle r) {
        dst = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        Main.getTaskManager().distribute(tasks);
        dst = null;
    }

    @Override
    public void paintComponent(Graphics g) {
        if (node.valid()) {
            super.paintComponent(g);
        } else {
            Graphics2D g2 = (Graphics2D) g;
            g2.setBackground(Color.BLACK);
            g2.clearRect(0, 0, PRE, PRE);
        }
    }
}
