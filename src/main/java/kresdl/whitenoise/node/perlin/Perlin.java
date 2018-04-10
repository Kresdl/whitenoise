package kresdl.whitenoise.node.perlin;

import java.awt.Component;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import kresdl.geometry.Vec2;
import kresdl.whitenoise.App;
import kresdl.whitenoise.Main;
import kresdl.whitenoise.node.Node;
import kresdl.whitenoise.node.View;
import kresdl.whitenoise.socket.In;
import kresdl.whitenoise.socket.Out;

@SuppressWarnings("serial")
public final class Perlin extends Node implements View {

    private static int res = App.OUT_PRE + 1;
    private final Output noise;

    public static class Info extends Node.Info implements Serializable {

        public final double d, pow, f;
        public final Vec2 offs;
        public final int[] pm;

        Info(int x, int y, double d, Vec2 offs, double pow, double f, int[] pm) {
            super(x, y);
            this.d = d;
            this.offs = offs;
            this.pow = pow;
            this.f = f;
            this.pm = pm;
        }
    }

    public void fireChange() {
        noise.lock();
        if (!fireParameterChange()) {
            Main.getTaskManager().execute(() -> {
                noise.setBuffer(null);
                noise.redraw();
                noise.unlock();
            });
        }
    }

    @Override
    public void render() {
        double data[] = out.getData();
        noise.setBuffer(data);
        noise.redraw();
        noise.unlock();
        In link = out.getLink();
        link.send(data);
        link.getNode().render();
    }

    @Override
    public void save() {
        int r = getRes();
        double[] data = new double[r * r];
        noise.fill(data);
        In link = out.getLink();
        link.send(data);
        link.getNode().save();
    }

    @Override
    public void cubic(int z, Path d) throws IOException {
        int r = getRes();
        double[] data = new double[r * r];
        noise.fill(data, z);
        In link = out.getLink();
        link.send(data);
        link.getNode().cubic(z, d);
    }

    public static void setRes(int res) {
        Perlin.res = res;
    }

    public static int getRes() {
        return res;
    }
    
    public Output getNoise() {
        return noise;
    }
        
    public static Perlin create(int x, int y, Main main, double detail, Vec2 offset, double power, double factor, int[] perm) {
        Perlin n = new Perlin(x, y, main, detail, offset, power, factor, perm);
        n.init();
        return n;
    }
 
    private Perlin(int x, int y, Main main, double detail, Vec2 offset, double power, double factor, int[] perm) {
        super("Perlin noise", x, y, main);

        noise = new Output(this, detail, offset, power, factor, perm);
        controls = new Component[] {
            new Controls(noise)
        };
        out = new Out(this);
    }

    @Override
    public Info getInfo() {
        return new Info(x, y, noise.getDetail(), noise.getOffset(), noise.getPower(), noise.getFactor(), noise.getPerm());
    }
    
    @Override
    public boolean isLocked() {
        return noise.isLocked();
    }
}
