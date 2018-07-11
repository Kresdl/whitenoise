package kresdl.whitenoise.node;

import java.io.Serializable;
import kresdl.whitenoise.App;
import kresdl.whitenoise.Main;
import kresdl.whitenoise.buffer.Stream;
import kresdl.whitenoise.socket.In;
import kresdl.whitenoise.socket.Out;

@SuppressWarnings("serial")
public final class Mad extends Modifier {

    public static class Info extends Node.Info {

        Info(int x, int y) {
            super(x, y);
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
                double m = p2.get();
                dst[k++] = p1.get() * m + m;
            }
        }
    }
    
    public static Mad create(int x, int y, Main main) {
        Mad n = new Mad(x, y, main);
        n.init();
        return n;
    }

    private Mad(int x, int y, Main main) {
        super("Mul Add", x, y, main);
        in = new In[] {
            new In(this),
            new In(this)
        };
        
        out = new Out(this);
        
        for (int i = 0; i < App.PARALLELISM; i++) {
            tasks.add(new Task(i));
        }
    }

    @Override
    public Info getInfo() {
        return new Info(x, y);
    }
}
