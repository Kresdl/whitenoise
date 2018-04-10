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
public final class Transform extends Modifier {

    public static class Info extends Node.Info implements Serializable {

        public final double q, p, t;

        Info(int x, int y, double q, double p, double t) {
            super(x, y);
            this.q = q;
            this.p = p;
            this.t = t;
        }
    }

    class Task extends Modifier.Task {

        Task(int i) {
            super(i);
        }

        @Override
        public void run() {
            update();
            double qv = q.get();
            Stream p1 = in[0].getStream(i), 
                    p2 = in[1].getStream(i), 
                    p3 = in[2].getStream(i);
            for (int i = 0; i < n; i++) {
                dst[k++] = Math.pow(p1.get(), qv) * p2.get() + p3.get();
            }
        }
    }

    public static Transform create(int x, int y, Main main, double qval, double pval, double tval) {
        Transform n = new Transform(x, y, main, qval, pval, tval);
        n.init();
        return n;
    }
    
    private final Field q, p, t;

    private Transform(int x, int y, Main main, double qval, double pval, double tval) {
        super("<html>px<sup>q</sup>+t</html>", x, y, main);
        in = new In[] {
            new In(this),
            new In(this),
            new In(this)
        };
        
        q = new Field(qval, this);
        p = Field.crossRef(pval, in[0]);
        t = Field.crossRef(tval, in[1]);
        controls = new Component[] {
            q, p, t
        };
        
        out = new Out(this);
        for (int i = 0; i < App.PARALLELISM; i++) {
            tasks.add(new Task(i));
        }
    }

    @Override
    public Info getInfo() {
        return new Info(x, y, q.get(), p.get(), t.get());
    }
}
