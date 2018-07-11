package kresdl.whitenoise.node;

import java.awt.Component;
import kresdl.whitenoise.App;
import kresdl.whitenoise.Main;
import kresdl.whitenoise.buffer.Stream;
import kresdl.whitenoise.controls.Slider;
import kresdl.whitenoise.socket.In;
import kresdl.whitenoise.socket.Out;

@SuppressWarnings("serial")
public final class Invert extends Modifier {

    public static class Info extends Node.Info {

        public final double v;

        Info(int x, int y, double v) {
            super(x, y);
            this.v = v;
        }
    }

    class Task extends Modifier.Task {

        Task(int i) {
            super(i);
        }

        @Override
        public void run() {
            update();
            Stream p1 = in[0].getStream(i);
            double t = invert.get();
            for (int i = 0; i < n; i++) {
                dst[k++] = t - p1.get();
            }
        }
    }

    public static Invert create(int x, int y, Main main, double val) {
        Invert n = new Invert(x, y, main, val);
        n.init();
        return n;
    }
    
    private final Slider invert;
    
    private Invert(int x, int y, Main main, double val) {
        super("Invert", x, y, main);
        in = new In[] {
            new In(this)
        };
        
        invert = new Slider(val, 10, this);
        controls = new Component[] {
            invert
        };
        out = new Out(this);
        for (int i = 0; i < App.PARALLELISM; i++) {
            tasks.add(new Task(i));
        }
    }

    @Override
    public Info getInfo() {
        return new Info(x, y, invert.get());
    }
}
