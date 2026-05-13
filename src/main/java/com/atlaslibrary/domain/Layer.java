package com.atlaslibrary.domain;

import java.util.UUID;

public class Layer {
    public String id = UUID.randomUUID().toString();
    public String name;
    public int zOrder;
    public boolean visible = true;
    public boolean locked = false;
    public double opacity = 1.0;

    public Layer(String name, int zOrder) { this.name = name; this.zOrder = zOrder; }
}
