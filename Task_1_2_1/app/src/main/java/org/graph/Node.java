package org.graph;

import java.util.List;
import java.util.ArrayList;

public abstract class Node {
    private int identifier;

    public int getId() {
        return identifier;
    }
    
}

class AdjencyNode extends Node {
    private int identifier;
    private List<Edge> outNodes;

    AdjencyNode(int id) {
        identifier = id;
        outNodes = new ArrayList<>();
    }
}

class IncidenceNode extends Node {

}
