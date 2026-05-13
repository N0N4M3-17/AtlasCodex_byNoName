package com.atlaslibrary.domain;

import java.util.UUID;

public class Anchor {
    public String id = UUID.randomUUID().toString();
    public String sourceNodeId;
    public AnchorType type;
    public String target;
}
