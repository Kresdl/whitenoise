package kresdl.whitenoise.node.composite;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import kresdl.gradienteditor.GradientEditor;
import kresdl.utilities.Gradient;
import kresdl.whitenoise.App;
import kresdl.whitenoise.Main;
import kresdl.whitenoise.buffer.Stream;
import kresdl.whitenoise.node.Mad;
import kresdl.whitenoise.node.Mask;
import kresdl.whitenoise.node.Node;
import kresdl.whitenoise.node.Transform;
import kresdl.whitenoise.node.View;
import kresdl.whitenoise.node.composite.Output.Mode;
import static kresdl.whitenoise.node.composite.Output.PRE;
import kresdl.whitenoise.node.perlin.Perlin;
import kresdl.whitenoise.socket.In;

@SuppressWarnings("serial")
public final class Composite extends Node implements View {

    private Work work;
    
    public static class Progress extends JDialog implements PropertyChangeListener {

        private static final JLabel LABEL = new JLabel("Saving...", SwingConstants.CENTER);

        static Progress create() {
            Progress p = new Progress();
            JPanel panel = new JPanel(new BorderLayout());
            panel.setPreferredSize(new Dimension(200, 200));
            panel.add(LABEL, BorderLayout.CENTER);
            p.setContentPane(panel);
            p.setModal(true);
            p.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            p.setUndecorated(true);
            p.setOpacity(0.75f);
            p.pack();
            return p;
        }
        
        private Progress() {
            super();
        }

        void showit(Output owner) {
            LABEL.setText("Saving...");
            setLocationRelativeTo(owner);
            setVisible(true);
        }

        void hideit() {
            setVisible(false);
        }
        
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            int progress = (int) e.getNewValue();
            LABEL.setText("Saving... " + (int) progress + "%");                
        }                    
    }

    private static final Progress PROGRESS = Progress.create();

    private class Work extends SwingWorker<Void, Void> {
        private int res;
        
        private Work(int res) {
            this.res = res;
        }
        
        @Override
        public Void doInBackground() {
            Perlin.setRes(res);
            emptyDown();
            saveCube();
            Perlin.setRes(PRE + 1);
            renderImage();
            return null;
        }
        
        public void updateProgress(double progress) {
            this.setProgress((int) (100 * progress));
        }
    }
    
    public void doWork(int res) {
        work = new Work(res);
        work.addPropertyChangeListener(PROGRESS);
        Main.getTaskManager().execute(work);
    }
    
    public static class Info extends Node.Info implements Serializable {

        public Gradient g;
        public double f;
        public Mode mode;

        Info(int x, int y, Gradient g, double f, Mode mode) {
            super(x, y);
            this.g = g;
            this.f = f;
            this.mode = mode;
        }
    }

    private final Output output;
    private final Controls ctrl;
    
    public static Composite create(int x, int y, Main main, Gradient g, double distribution, Mode mode) {
        Composite n = new Composite(x, y, main, g, distribution, mode);
        n.init();
        return n;
    }

    private Composite(int x, int y, Main main, Gradient g, double distribution, Mode mode) {
        super("Output", x, y, main);
        in = new In[]{
            new In(this)
        };

        output = new Output(this, mode);
        ctrl = new Controls(this, g, distribution);

        controls = new Component[]{
            output,
            Box.createRigidArea(new Dimension(0, 5)),
            ctrl
        };
        updateEditorVisibility(mode);
    }

    final void updateEditorVisibility(Mode mode) {
        if ((mode == Mode.NORMAL) || (mode == Mode.BUMP) || (mode == Mode.BW)) {
            ctrl.setVisible(false);
        } else {
            ctrl.setVisible(true);
        }
        updateBounds();
    }

    @Override
    public void render() {
        output.redraw();
        output.unlock();
    }

    @Override
    public void save() {
        int res = Perlin.getRes() - 1;
        BufferedImage img = new BufferedImage(res, res, BufferedImage.TYPE_3BYTE_BGR);
        byte[] dst = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        output.build(dst);
        empty();
        try {
            ImageIO.write(img, Output.getFormat(), Output.getFile());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            img.flush();
        }
    }

    @Override
    public void cubic(int z, Path d) throws IOException {
        int r = Perlin.getRes();
        BufferedImage img = new BufferedImage(r, r, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        Set<Runnable> tasks = new HashSet<>();

        for (int i = 0; i < App.PARALLELISM; i++) {
            tasks.add(getTask(data, i));
        }

        Main.getTaskManager().distribute(tasks);
        try (OutputStream s = Files.newOutputStream(d)) {
            ImageIO.write(img, Output.getFormat(), s);
            work.updateProgress((double) z / Perlin.getRes());//   output.getProgress().advance(100.0d / Perlin.getRes());
        } finally {
            img.flush();
        }
    }

    Runnable getTask(byte[] data, int i) {
        return () -> {
            Stream x = in[0].getStream(i);
            int r = Perlin.getRes();
            int n = r / App.PARALLELISM;

            int a = 4 * r * i * n;
            int b = a + 4 * r * n;

            GradientEditor ge = ctrl.getGradientEditor();
            byte[] array = ge.getArray();
            while (a < b) {
                double alpha = ctrl.getAlpha(x.get());
                int offset = ge.getArrayOffset(alpha);
                data[a++] = (byte) (255 * alpha);
                data[a++] = array[offset];
                data[a++] = array[offset + 1];
                data[a++] = array[offset + 2];
            }
        };
    }

    Controls getControls() {
        return ctrl;
    }

    In getInput() {
        return in[0];
    }

    public void fireStructureChange() {
        output.setEnablePopup(valid());
        output.updateSaveOptions();
        if (valid()) {
            output.lock();
            Main.getTaskManager().execute(() -> {
                emptyDown();
                renderImage();
            });
        }
    }

    void renderImage() {
        getRoots(new HashSet<>()).stream().forEach(Perlin::render);
    }

    void saveTree(File f) {
        String json = serializeTree();
        try (FileWriter out = new FileWriter(f)) {
            out.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void saveImage() {
        getRoots(new HashSet<>()).stream().forEach(Perlin::save);
    }

    void saveCube() {
        File f = Output.getFile();
        try {
            Files.deleteIfExists(f.toPath());
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            URI uri = URI.create("jar:" + f.toURI().toString());
            try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
                Set<Perlin> s = getRoots(new HashSet<>());
                for (int z = 0; z < Perlin.getRes(); z++) {
                    Path d = fs.getPath(String.format("/z%d.%s", z + 1, Output.getFormat()));
                    for (Perlin p : s) {
                        p.cubic(z, d);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Info getInfo() {
        return new Info(x, y, ctrl.getGradientEditor().cloneGradient(), ctrl.getFar(), output.getMode());
    }

    @Override
    public boolean isLocked() {
        return output.isLocked();
    }

    public void lock() {
        output.lock();
    }

    private String serializeTree() {
        StringBuilder s = new StringBuilder();
        s.append("{\"nodes\":[");

        Composite.Info ci = getInfo();
        s.append(String.format("{\"type\":\"Composite\",\"x\":\"%d\",\"y\":\"%d\"", ci.x, ci.y));
        s.append(String.format(Locale.US, ",\"f\":\"%.4f\",\"mode\":\"%s\",\"g\":{", ci.f, ci.mode));
        Iterator<Map.Entry<Double, Color>> it2 = ci.g.entrySet().iterator();
        for (;;) {
            Map.Entry<Double, Color> e = it2.next();
            Color c = e.getValue();
            s.append(String.format(Locale.US, "\"%.4f\":[\"%d\",\"%d\",\"%d\"]", e.getKey(), c.getRed(), c.getGreen(), c.getBlue()));
            if (!it2.hasNext()) {
                break;
            }
            s.append(",");
        }
        s.append("}},\n");

        List<Node.Info> info = new ArrayList<>();
        List<Integer> conn = new ArrayList<>();
        collect(info, conn);
        Iterator<Node.Info> it = info.iterator();

        for (;;) {
            Node.Info n = it.next();
            Matcher m = Pattern.compile(".+\\.(\\w+)\\$Info").matcher(n.getClass().getName());
            m.find();
            s.append(String.format("{\"type\":\"%s\",\"x\":\"%d\",\"y\":\"%d\"", m.group(1), n.x, n.y));
            if (n instanceof Perlin.Info) {
                Perlin.Info i = (Perlin.Info) n;
                s.append(String.format(Locale.US, ",\"d\":\"%.4f\",\"offsx\":\"%.4f\",\"offsy\":\"%.4f\",\"pow\":\"%.4f\",\"f\":\"%.4f\",\"pm\":[", i.d, i.offs.x, i.offs.y, i.pow, i.f));
                for (int j = 0; j < i.pm.length; j++) {
                    s.append(String.format("\"%d\"", i.pm[j]));
                    if (j < i.pm.length - 1) {
                        s.append(",");
                    }
                }
                s.append("]}");
            } else if (n instanceof Transform.Info) {
                Transform.Info i = (Transform.Info) n;
                s.append(String.format(Locale.US, ",\"q\":\"%.4f\",\"p\":\"%.4f\",\"t\":\"%.4f\"}", i.q, i.p, i.t));
            } else if (n instanceof Mad.Info || n instanceof Mask.Info) {
                s.append("}");
            } else {
                Field f = null;
                try {
                    f = n.getClass().getDeclaredField("v");
                    f.setAccessible(true);
                    s.append(String.format(Locale.US, ",\"v\":\"%.4f\"}", f.getDouble(n)));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException();
                }
            }

            if (!it.hasNext()) {
                break;
            }
            s.append(",\n");
        }
        s.append("],\n\"connections\":[");
        int n = conn.size();
        for (int i = 0; i < n - 1; i++) {
            s.append(String.format("\"%d\",", conn.get(i)));
        }
        s.append(String.format("\"%d\"]}", conn.get(n - 1)));
        return s.toString();
    }
}
