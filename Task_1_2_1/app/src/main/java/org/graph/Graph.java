package org.graph;

import java.util.List;
import java.util.ArrayList;

public abstract class Graph {

    void pr () {}

    // void add_node (){}

    // void add_edge (){}
    
    
}

class IncidenceMatrix extends Graph {
    private int so = 5;

    public void pr() {
        System.out.println("JJJJJJJJ");
    }
}

class AdjencyMatrix extends Graph {
    private int so = 5;

    public void pr() {
        System.out.println("JJJJJJJJ");
    }
}


class AdjencyList extends Graph {
    private int so = 5;

    public void pr() {
        System.out.println("JJJJJJJJ");
    }
}


