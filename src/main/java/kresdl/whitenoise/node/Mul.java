package kresdl.whitenoise.node;

import java.awt.Component;
import java.io.Serializable;
import kresdl.whitenoise.App;
import kresdl.whitenoise.Main;
import kresdl.whitenoise.buffer.Stream;
import kresdl.whitenoise.controls.Field;
import kresdl.whitenoise.socket.In;
import kresdl.whitenoise.socket.Out;

@SuppressWarnings("serial")
public final class Mul extends Modifier {

    public static class Info extends Node.Info implements Serializable {

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
                dst[k++] = p1.get() * p2.get();
            }
        }
    }
    
    private final Field factor;
    
    public static Mul create(int x, int y, Main main, double val) {
        Mul n = new Mul(x, y, main, val);
        n.init();
        return n;
    }

    private Mul(int x, int y, Main main, double val) {
        super("Mul", x, y, main);
        in = new In[] { 
            new In(this), 
            new In(this) 
        };
        
        factor = Field.crossRef(val, in[1]);        
        controls = new Component[] { 
            factor 
        };
        
        out = new Out(this);
        
        for (int i = 0; i < App.PARALLELISM; i++) {
            tasks.add(new Task(i));
        }
    }

    @Override
    public Info getInfo() {
        return new Info(x, y, factor.get());
    }
}
