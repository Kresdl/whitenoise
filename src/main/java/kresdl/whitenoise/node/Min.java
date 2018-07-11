package kresdl.whitenoise.node;

import java.awt.Component;
import kresdl.whitenoise.App;
import kresdl.whitenoise.Main;
import kresdl.whitenoise.buffer.Stream;
import kresdl.whitenoise.controls.Field;
import kresdl.whitenoise.socket.In;
import kresdl.whitenoise.socket.Out;

@SuppressWarnings("serial")
public final class Min extends Modifier {

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
                    p2 = in[1].getStream(i);
            for (int i = 0; i < n; i++) {
                dst[k++] = Math.min(p1.get(), p2.get());
            }
        }
    }
    
    public static Min create(int x, int y, Main main, double min) {
        Min n = new Min(x, y, main, min);
        n.init();
        return n;
    }
    
    private final Field min;

    private Min(int x, int y, Main main, double m) {
        super("Min", x, y, main);
        in = new In[] {
            new In(this),
            new In(this)
        };
        
        min = Field.crossRef(m, in[1]);
        controls = new Component[] { 
            min
        };
        
        out = new Out(this);
        for (int i = 0; i < App.PARALLELISM; i++) {
            tasks.add(new Task(i));
        }
    }

    @Override
    public Info getInfo() {
        return new Info(x, y, min.get());
    }
}
