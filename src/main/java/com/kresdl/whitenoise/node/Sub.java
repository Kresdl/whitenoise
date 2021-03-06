package com.kresdl.whitenoise.node;

import java.awt.Component;
import com.kresdl.whitenoise.App;
import com.kresdl.whitenoise.Main;
import com.kresdl.whitenoise.buffer.Stream;
import com.kresdl.whitenoise.controls.Field;
import com.kresdl.whitenoise.socket.In;
import com.kresdl.whitenoise.socket.Out;

@SuppressWarnings("serial")
public final class Sub extends Modifier {

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
                dst[k++] = p1.get() - p2.get();
            }
        }
    }
    
    public static Sub create(int x, int y, Main main, double val) {
        Sub n = new Sub(x, y, main, val);
        n.init();
        return n;
    }
    
    private final Field sub;

    private Sub(int x, int y, Main main, double val) {
        super("Sub", x, y, main);
        in = new In[] {
            new In(this),
            new In(this)
        };
        
        sub = Field.crossRef(val, in[0]);
        controls = new Component[] {
            sub
        };
        
        out = new Out(this);
        for (int i = 0; i < App.PARALLELISM; i++) {
            tasks.add(new Task(i));
        }
    }

    @Override
    public Info getInfo() {
        return new Info(x, y, sub.get());
    }
}
