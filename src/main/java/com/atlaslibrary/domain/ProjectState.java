package com.atlaslibrary.domain;

import java.util.ArrayList;
import java.util.List;

public class ProjectState {
    public String projectName = "Untitled Project";
    public final List<Layer> layers = new ArrayList<>();
    public final List<AtlasNode> nodes = new ArrayList<>();
    public final List<Article> articles = new ArrayList<>();
    public final List<Anchor> anchors = new ArrayList<>();
}
