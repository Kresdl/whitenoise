package kresdl.whitenoise.node;

import java.awt.Component;
import kresdl.utilities.Misc;
import kresdl.whitenoise.App;
import kresdl.whitenoise.Main;
import kresdl.whitenoise.buffer.Stream;
import kresdl.whitenoise.controls.Slider;
import kresdl.whitenoise.socket.In;
import kresdl.whitenoise.socket.Out;

@SuppressWarnings("serial")
public final class Interpolate extends Modifier {

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
            Stream p1 = in[0].getStream(i), 
                    p2 = in[1].getStream(i), 
                    p3 = in[2].getStream(i);
            for (int i = 0; i < n; i++) {
                double p = Misc.sat(p2.get());
                dst[k++] = get(p1, p3, p);
            }
        }

        private double get(Stream a, Stream b, double p) {
            return b.get() * p + a.get() * (1.0d - p);
        }
    }

    public static Interpolate create(int x, int y, Main main, double val) {
        Interpolate n = new Interpolate(x, y, main, val);
        n.init();
        return n;
    }
    
    private final Slider lerp;
    
    private Interpolate(int x, int y, Main main, double pval) {
        super("Interpolate", x, y, main);
        in = new In[] {
            new In(this),
            new In(this),
            new In(this)
        };
        
        lerp = Slider.crossRef(pval, 1, in[1]);
        controls = new Component[] {
            lerp
        };
        
        out = new Out(this);
        for (int i = 0; i < App.PARALLELISM; i++) {
            tasks.add(new Task(i));
        }
    }

    @Override
    public Info getInfo() {
        return new Info(x, y, lerp.get());
    }
}
