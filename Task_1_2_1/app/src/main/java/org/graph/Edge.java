package org.graph;

public class Edge {
    private Pair<Node, Node> nodes;

    Edge (int first, int second) {
        nodes = new Pair<Node, Node>(new AdjencyNode(first), new AdjencyNode(second));
    }

    Edge (Node from, Node to) {
        nodes = new Pair<Node, Node>(from, to);
    }
}
