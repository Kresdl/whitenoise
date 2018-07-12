package com.kresdl.whitenoise.buffer;

public class Single implements Stream {

    private final double d;

    public Single(double data) {
        d = data;
    }

    @Override
    public double get() {
        return d;
    }
}
