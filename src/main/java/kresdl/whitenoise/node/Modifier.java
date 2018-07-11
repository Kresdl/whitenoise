package kresdl.whitenoise.node;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import kresdl.whitenoise.App;
import kresdl.whitenoise.Main;
import kresdl.whitenoise.node.perlin.Perlin;
import kresdl.whitenoise.socket.In;

@SuppressWarnings("serial")
abstract class Modifier extends Node {

    abstract class Task implements Runnable {

        final int i;
        int k, n;

        Task(int i) {
            this.i = i;
        }

        void update() {
            int r = Perlin.getRes();
            if (((int) Math.sqrt(dst.length) & 1) == 1) {
                n = r * (r - 1) / App.PARALLELISM;
                k = i * n;
                n += (i == (App.PARALLELISM - 1) ? r : 0);
            } else {
                n = r * r / App.PARALLELISM;
                k = i * n;
            }
        }
    }

    Set<Runnable> tasks = new HashSet<>();
    double[] dst;

    Modifier(String name, int x, int y, Main main) {
        super(name, x, y, main);
    }

    @Override
    public void render() {
        if (hasData()) {
            dst = getOut().getData();
            Main.getTaskManager().distribute(tasks);
            In link = getOut().getLink();
            link.send(dst);
            link.getNode().render();
        }
    }

    @Override
    public void save(File file, String format) {
        if (hasData()) {
            int r = Perlin.getRes();
            dst = new double[r * r];
            Main.getTaskManager().distribute(tasks);
            empty();
            In link = getOut().getLink();
            link.send(dst);
            link.getNode().save(file, format);
        }
    }

    @Override
    public void cubic(int z, Path d, String format) throws IOException {
        if (hasData()) {
            int r = Perlin.getRes();
            dst = new double[r * r];
            Main.getTaskManager().distribute(tasks);
            empty();
            In link = getOut().getLink();
            link.send(dst);
            link.getNode().cubic(z, d, format);
        }
    }

}
