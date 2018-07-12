package com.kresdl.whitenoise.node.perlin;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;
import com.kresdl.geometry.Vec;
import com.kresdl.geometry.Vec2;
import com.kresdl.utilities.Misc;
import com.kresdl.utilities.Mouse;
import com.kresdl.whitenoise.App;
import com.kresdl.whitenoise.Main;
import com.kresdl.whitenoise.node.Node;
import com.kresdl.xpanel.XPanel;

@SuppressWarnings("serial")
public final class Output extends XPanel {
    class Task2D implements Runnable {

        private final int i;

        Task2D(int i) {
            this.i = i;
        }

        private double p(int x, int y) {
            double h2 = Perlin.getRes() / 2;

            double gx = offset.x + h + detail * (x - h2);
            double gy = offset.y + h + detail * (y - h2);

            int xf = (int) Math.floor(gx);
            int yf = (int) Math.floor(gy);

            int x1 = xf & m;
            int y1 = yf & m;
            int x2 = (x1 + 1) & m;
            int y2 = (y1 + 1) & m;

            int i1 = p[(p[x1] + y1) & m];
            int i2 = p[(p[x2] + y1) & m];
            int i3 = p[(p[x1] + y2) & m];
            int i4 = p[(p[x2] + y2) & m];

            double g1x = g[i1].x;
            double g1y = g[i1].y;

            double g2x = g[i2].x;
            double g2y = g[i2].y;

            double g3x = g[i3].x;
            double g3y = g[i3].y;

            double g4x = g[i4].x;
            double g4y = g[i4].y;

            gx -= xf;
            gy -= yf;

            double dx = gx - 1;
            double dy = gy - 1;

            double d1 = gx * g1x + gy * g1y;
            double d2 = dx * g2x + gy * g2y;
            double d3 = gx * g3x + dy * g3y;
            double d4 = dx * g4x + dy * g4y;

            gx = gx * gx * gx * (gx * (gx * 6 - 15) + 10);
            gy = gy * gy * gy * (gy * (gy * 6 - 15) + 10);

            d1 = d1 + gx * (d2 - d1);
            d2 = d3 + gx * (d4 - d3);

            double t = 0.5d * (d1 + gy * (d2 - d1) + 1);
            return Math.pow(t, power) * factor;
        }

        @Override
        public void run() {
            int rx = Perlin.getRes();
            int ry = (rx - 1) / App.PARALLELISM;
            int ys = ry * i;
            int bk = rx * ys;
            int pk = 3 * bk;
            ry = (i == (App.PARALLELISM - 1) ? ry + 1 : ry) + ys;

            if (buffer != null) {
                if (pixels != null) {
                    for (int y = ys; y < ry; y++) {
                        for (int x = 0; x < rx; x++) {
                            double q = p(x, y);
                            buffer[bk++] = q;
                            byte t = (byte) (255 * Misc.sat(q));
                            pixels[pk++] = t;
                            pixels[pk++] = t;
                            pixels[pk++] = t;
                        }
                    }
                } else {
                    for (int y = ys; y < ry; y++) {
                        for (int x = 0; x < rx; x++) {
                            buffer[bk++] = p(x, y);
                        }
                    }
                }
            } else {
                for (int y = ys; y < ry; y++) {
                    for (int x = 0; x < rx; x++) {
                        double q = p(x, y);
                        byte t = (byte) (255 * Misc.sat(q));
                        pixels[pk++] = t;
                        pixels[pk++] = t;
                        pixels[pk++] = t;
                    }
                }
            }
        }
    }

    class Task3D implements Runnable {

        private final int i, z;

        Task3D(int i, int z) {
            this.i = i;
            this.z = z;
        }

        double p(int x, int y, int z) {
            double h2 = Perlin.getRes() / 2;

            double gx = offset.x + h + detail * (x - h2);
            double gy = offset.y + h + detail * (y - h2);
            double gz = h + detail * (z - h2);

            int xf = (int) Math.floor(gx);
            int yf = (int) Math.floor(gy);
            int zf = (int) Math.floor(gz);

            int x1 = xf & m;
            int y1 = yf & m;
            int z1 = zf & m;
            int x2 = (x1 + 1) & m;
            int y2 = (y1 + 1) & m;
            int z2 = (z1 + 1) & m;

            int i1 = p[(p[(p[x1] + y1) & m] + z1) & m];
            int i2 = p[(p[(p[x2] + y1) & m] + z1) & m];
            int i3 = p[(p[(p[x1] + y2) & m] + z1) & m];
            int i4 = p[(p[(p[x2] + y2) & m] + z1) & m];
            int i5 = p[(p[(p[x1] + y1) & m] + z2) & m];
            int i6 = p[(p[(p[x2] + y1) & m] + z2) & m];
            int i7 = p[(p[(p[x1] + y2) & m] + z2) & m];
            int i8 = p[(p[(p[x2] + y2) & m] + z2) & m];

            double g1x = g[i1].x;
            double g1y = g[i1].y;
            double g1z = g[i1].z;

            double g2x = g[i2].x;
            double g2y = g[i2].y;
            double g2z = g[i2].z;

            double g3x = g[i3].x;
            double g3y = g[i3].y;
            double g3z = g[i3].z;

            double g4x = g[i4].x;
            double g4y = g[i4].y;
            double g4z = g[i4].z;

            double g5x = g[i5].x;
            double g5y = g[i5].y;
            double g5z = g[i5].z;

            double g6x = g[i6].x;
            double g6y = g[i6].y;
            double g6z = g[i6].z;

            double g7x = g[i7].x;
            double g7y = g[i7].y;
            double g7z = g[i7].z;

            double g8x = g[i8].x;
            double g8y = g[i8].y;
            double g8z = g[i8].z;

            gx -= xf;
            gy -= yf;
            gz -= zf;

            double dx = gx - 1;
            double dy = gy - 1;
            double dz = gz - 1;

            double d1 = gx * g1x + gy * g1y + gz * g1z;
            double d2 = dx * g2x + gy * g2y + gz * g2z;
            double d3 = gx * g3x + dy * g3y + gz * g3z;
            double d4 = dx * g4x + dy * g4y + gz * g4z;

            double d5 = gx * g5x + gy * g5y + dz * g5z;
            double d6 = dx * g6x + gy * g6y + dz * g6z;
            double d7 = gx * g7x + dy * g7y + dz * g7z;
            double d8 = dx * g8x + dy * g8y + dz * g8z;

            gx = gx * gx * gx * (gx * (gx * 6 - 15) + 10);
            gy = gy * gy * gy * (gy * (gy * 6 - 15) + 10);
            gz = gz * gz * gz * (gz * (gz * 6 - 15) + 10);

            d1 = d1 + gx * (d2 - d1);
            d2 = d3 + gx * (d4 - d3);
            d3 = d5 + gx * (d6 - d5);
            d4 = d7 + gx * (d8 - d7);

            d1 = d1 + gy * (d2 - d1);
            d2 = d3 + gy * (d4 - d3);

            double t = 0.5d * (d1 + gz * (d2 - d1) + 1);
            return Math.pow(t, power) * factor;
        }

        @Override
        public void run() {
            int rx = Perlin.getRes();
            int ry = rx / App.PARALLELISM;
            int ys = ry * i;
            int f = rx * ys;
            ry += ys;

            for (int y = ys; y < ry; y++) {
                for (int x = 0; x < rx; x++) {
                    buffer[f++] = p(x, y, z);
                }
            }
        }
    }

    public static final int PRE = App.NOISE_PRE;

    private static int pSize, m, h;
    private final Perlin node;
    private int[] p;
    private static Vec[] g;
    private double detail, factor, power;
    private Vec2 offset;
    private final Mouse mouse = new Mouse(this);
    private final Set<Runnable> tasks = new HashSet<>();
    private byte[] pixels;
    private double[] buffer;

    static {
        loadGradients();
    }

    static void loadGradients() {
        try (ObjectInputStream in = new ObjectInputStream(
                Output.class.getClassLoader().getResourceAsStream("grad.ser"))) {
            pSize = (Integer) in.readObject();
            m = pSize - 1;
            h = pSize / 2;
            g = new Vec[pSize];
            for (int i = 0; i < pSize; i++) {
                g[i] = (Vec) in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    static void generateGradients(int size, String file) {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(file))) {
            pSize = size;
            m = size - 1;
            h = size / 2;
            g = new Vec[size];
            out.writeObject(size);
            for (int i = 0; i < size; i++) {
                Vec gr = new Vec(2 * Math.random() - 1, 2 * Math.random() - 1, 2 * Math.random() - 1);
                g[i] = gr;
                out.writeObject(gr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Output(Perlin node, double detail, Vec2 offset, double power, double factor, int[] perm) {
        super(Perlin.getRes(), Perlin.getRes(), BufferedImage.TYPE_3BYTE_BGR);
        this.node = node;
        this.detail = detail;
        this.offset = offset;
        this.power = power;
        this.factor = factor;
        setPreferredSize(new Dimension(PRE, PRE));
        mouse.onDrag(0, this::move);
        createTasks();

        if (perm != null) {
            this.p = perm;
        } else {
            lock();
            Main.getTaskManager().execute(() -> {
                perm();
                redraw();
                unlock();
            });
        }
    }

    private void createTasks() {
        for (int i = 0; i < App.PARALLELISM; i++) {
            tasks.add(new Task2D(i));
        }
    }

    public void init() {
        drawImage();
        swapBuffers();
    }

    public void fill(double[] buffer) {
        pixels = null;
        this.buffer = buffer;
        Main.getTaskManager().distribute(tasks);
    }

    public void fill(double[] buffer, int z) {
        pixels = null;
        this.buffer = buffer;

        Set<Runnable> t = new HashSet<>();
        for (int i = 0; i < App.PARALLELISM; i++) {
            t.add(new Task3D(i, z));
        }
        Main.getTaskManager().distribute(t);
    }

    private void perm() {
        int[] t = new int[pSize];
        p = new int[pSize];

        for (int i = 0; i < pSize; i++) {
            t[i] = i;
        }
        for (int i = 0; i < pSize; i++) {
            int q = (int) (Math.random() * (pSize - i));
            p[i] = t[q];
            t[q] = t[t.length - i - 1];
        }
    }

    void setBuffer(double[] buffer) {
        this.buffer = buffer;
    }

    @Override
    public void drawImage(BufferedImage img, Rectangle r) {
        pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        Main.getTaskManager().distribute(tasks);
    }

    private void move(MouseEvent e) {
        if (!Node.isBusy()) {
            Point d = mouse.getMovement();
            offset = Vec2.sub(offset, new Vec2(d.x * detail, d.y * detail));
            node.fireChange();
        }
    }

    double getDetail() {
        return detail;
    }

    void setDetail(double d) {
        detail = d;
    }

    Perlin getNode() {
        return node;
    }

    double getFactor() {
        return factor;
    }

    double getPower() {
        return power;
    }

    void setFactor(double f) {
        factor = f;
    }

    void setPower(double p) {
        power = p;
    }

    Vec2 getOffset() {
        return offset;
    }

    void setOffset(Vec2 offs) {
        offset = offs;
    }
    
    int[] getPerm() {
        return p;
    }
    
    void setPerm(int[] perm) {
        p = perm;
    }    
}
