package kresdl.whitenoise.node;

import java.awt.Component;
import kresdl.whitenoise.App;
import kresdl.whitenoise.Main;
import kresdl.whitenoise.buffer.Stream;
import kresdl.whitenoise.controls.Field;
import kresdl.whitenoise.socket.In;
import kresdl.whitenoise.socket.Out;

@SuppressWarnings("serial")
public final class Max extends Modifier {

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
                dst[k++] = Math.max(p1.get(), p2.get());
            }
        }
    }

    public static Max create(int x, int y, Main main, double min) {
        Max n = new Max(x, y, main, min);
        n.init();
        return n;
    }
    
    private Field max;

    private Max(int x, int y, Main main, double m) {
        super("Max", x, y, main);
        in = new In[] {
            new In(this),
            new In(this)
        };
        
        max = Field.crossRef(m, in[1]);
        controls = new Component[] {
            max
        };
        
        out = new Out(this);
        
        for (int i = 0; i < App.PARALLELISM; i++) {
            tasks.add(new Task(i));
        }
    }

    @Override
    public Info getInfo() {
        return new Info(x, y, max.get());
    }
}
