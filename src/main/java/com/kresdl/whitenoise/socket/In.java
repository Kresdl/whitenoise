package com.kresdl.whitenoise.socket;

import java.io.Serializable;
import java.util.function.Supplier;
import com.kresdl.whitenoise.App;
import com.kresdl.whitenoise.buffer.Buffer;
import com.kresdl.whitenoise.buffer.Single;
import com.kresdl.whitenoise.buffer.Stream;
import com.kresdl.whitenoise.node.Node;
import com.kresdl.whitenoise.node.perlin.Perlin;

@SuppressWarnings("serial")
public class In extends Socket implements Serializable {

    private Out link;
    private Supplier<Double> def;
    private boolean valid;

    public In(Node node) {
        super(node);
    }

    public Stream getStream(int fraction) {
        if (data != null) {
            int r = Perlin.getRes();
            if (((int) Math.sqrt(data.length) & 1) == 1) {
                return new Buffer(data, fraction * r * (r - 1) / App.PARALLELISM);
            } else {
                return new Buffer(data, fraction * r * r / App.PARALLELISM);
            }
        } else {
            return new Single(def.get());
        }
    }

    public void send(double[] data) {
        this.data = data;
    }

    public boolean hasData() {
        return (data != null) || (hasDefault() & !valid);
    }

    @Override
    public boolean isLinked() {
        return (link != null);
    }

    void setLink(Out link) {
        this.link = link;
    }

    public Out getLink() {
        return link;
    }

    public boolean hasDefault() {
        return (def != null);
    }

    public Supplier<Double> getDefault() {
        return def;
    }

    public void setDefault(Supplier<Double> def) {
        this.def = def;
    }

    public void empty() {
        data = null;
    }

    public boolean hasSource() {
        return (valid || hasDefault());
    }

    public void setInvalid() {
        valid = false;
    }

    public void setValid() {
        valid = true;
    }

    public boolean valid() {
        return valid;
    }
}
