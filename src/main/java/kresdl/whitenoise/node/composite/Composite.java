package kresdl.whitenoise.node.composite;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
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
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
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
import kresdl.whitenoise.node.perlin.Perlin;
import kresdl.whitenoise.socket.In;

@SuppressWarnings("serial")
public final class Composite extends Node implements View {

    public class Progress extends JDialog implements PropertyChangeListener {

        private final JProgressBar bar = new JProgressBar();

        private Progress() {
            super();
            JPanel b = new JPanel(new BorderLayout());
            b.setPreferredSize(new Dimension(200, 200));
            JLabel lb = new JLabel("Saving...");
            lb.setPreferredSize(new Dimension(200, 32));
            lb.setHorizontalAlignment(JLabel.CENTER);
            lb.setVerticalAlignment(JLabel.BOTTOM);
            b.add(lb, BorderLayout.PAGE_START);     
            JPanel p = new JPanel(new GridBagLayout());         
            bar.setPreferredSize(new Dimension(150, 16));
            bar.setMinimum(0);
            bar.setMaximum(100);
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.NONE;
            c.anchor= GridBagConstraints.CENTER;
            p.add(bar, c);
            b.add(p, BorderLayout.CENTER);
            setContentPane(b);
            setModal(true);
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            setUndecorated(true);
            setOpacity(0.75f);
            pack();
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equals("progress")) {
                int progress = (int) e.getNewValue();
                bar.setValue(progress);
            } else {
                SwingWorker.StateValue state = (SwingWorker.StateValue) e.getNewValue();
                switch (state) {
                    case STARTED:
                        setLocationRelativeTo(output);
                        setVisible(true);
                        break;
                    case DONE:
                        setVisible(false);                                  
                }
            }
        }
    }
    
    public static class Info extends Node.Info {

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
    private static Output.Save.Task work;

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
    
    public void run(Output.Save.Task task) {
        work = task;
        work.addPropertyChangeListener(new Progress());
        lock();
        Main.getTaskManager().execute(work);        
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
    public void save(File file, String format) {
        int res = Perlin.getRes() - 1;
        BufferedImage img = new BufferedImage(res, res, BufferedImage.TYPE_3BYTE_BGR);
        byte[] dst = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        output.build(dst);
        empty();
        try {
            ImageIO.write(img, format, file);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            img.flush();
        }
    }

    @Override
    public void cubic(int z, Path d, String format) throws IOException {
        int r = Perlin.getRes();
        BufferedImage img = new BufferedImage(r, r, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        Set<Runnable> tasks = new HashSet<>();

        for (int i = 0; i < App.PARALLELISM; i++) {
            tasks.add(getTask(data, i));
        }

        Main.getTaskManager().distribute(tasks);
        try (OutputStream s = Files.newOutputStream(d)) {
            ImageIO.write(img, format, s);
            work.updateProgress((double) z / Perlin.getRes());
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

    void saveImage(File file, String format) {
        getRoots(new HashSet<>()).stream().forEach(p -> {
            p.save(file, format);
        });
    }

    void saveCube(File file, String format, String archiver) {
        try {
            Files.deleteIfExists(file.toPath());
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            URI uri = URI.create("jar:" + file.toURI().toString());
            try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
                Set<Perlin> s = getRoots(new HashSet<>());
                for (int z = 0; z < Perlin.getRes(); z++) {
                    Path d = fs.getPath(String.format("/z%d.%s", z + 1, format));
                    for (Perlin p : s) {
                        p.cubic(z, d, format);
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
