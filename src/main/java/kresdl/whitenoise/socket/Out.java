package kresdl.whitenoise.socket;

import java.io.Serializable;
import kresdl.whitenoise.node.Node;
import kresdl.whitenoise.node.perlin.Perlin;

@SuppressWarnings("serial")
public class Out extends Socket implements Serializable {

    private In link;

    public Out(Node node) {
        super(node);
        int res = Perlin.getRes();
        data = new double[res * res];
    }

    public void setLink(In link) {
        if (link != null) {
            this.link = link;
            link.setLink(this);
        } else {
            this.link.setLink(null);
            this.link = null;
        }
    }

    @Override
    public boolean isLinked() {
        return (link != null);
    }

    public In getLink() {
        return link;
    }

    public double[] getData() {
        return data;
    }
}
