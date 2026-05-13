package com.atlaslibrary.domain;

import java.util.UUID;

public class AtlasNode {
    public String id = UUID.randomUUID().toString();
    public NodeType type;
    public String layerId;
    public double x;
    public double y;
    public double scale = 1.0;
    public double rotation = 0;
    public double opacity = 1.0;
    public boolean visible = true;
    public boolean locked = false;
    public String content;
}
