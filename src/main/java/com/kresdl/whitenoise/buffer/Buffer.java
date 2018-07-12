package com.kresdl.whitenoise.buffer;

import com.kresdl.whitenoise.node.perlin.Perlin;

public class Buffer implements Stream {

    private final double[] data;
    private int q;

    public Buffer(double[] data, int q) {
        this.data = data;
        this.q = q;
    }

    public boolean hasNext() {
        return (q < data.length);
    }

    public double get(int x, int y) {
        return data[x + y * Perlin.getRes()];
    }

    @Override
    public double get() {
        return data[q++];
    }
}
