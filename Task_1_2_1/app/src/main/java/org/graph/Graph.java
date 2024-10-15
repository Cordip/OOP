package org.graph;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.*;
import com.google.common.collect.Iterables;

public abstract class Graph {

    void print () {

    }

    Node add_node (int id) {
        Node newNode = new AdjencyNode(id);
        return newNode;
    }

    Edge add_edge (int fst, int snd) {
        Edge newNode = new Edge(fst, snd);
        return newNode;
    }
    
    
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
    private int len;
    private List<Node> nodes;

    AdjencyList () {
        len = 0;
        nodes = new ArrayList<>();
    }

    public Node add_node () {
        Node newNode = new AdjencyNode(len);
        if (nodes.stream().noneMatch(x -> x.getId() == len)) {
            nodes.add(new AdjencyNode(len));
        }
        len += 1;
        return newNode;
    }

    public Node add_node (int id) {
        Node newNode = new AdjencyNode(id);
        if (nodes.stream().noneMatch(x -> x.getId() == id)) {
            nodes.add(newNode);
        }
        len += 1;
        return newNode;
        
    }

    public Edge add_edge (int first, int second) {
        Stream <Node> stream = nodes.stream();
       
        Edge newEdge = null;

        Node fromNode = null; 
        Node toNode = null;

        for (Node tmpNode : (Iterable <Node> ) stream :: iterator) {
            if (first == tmpNode.getId()) {
                fromNode = tmpNode;
            } else if (second == tmpNode.getId()) {
                toNode = tmpNode;
            }
        }
        if (fromNode != null) {
            fromNode = add_node(first);
        } if (toNode != null) {
            toNode = add_node(second);
        }
        newEdge = new Edge(fromNode, toNode);
        return newEdge;
    }

    public Edge add_edge (Node fromNode, Node toNode) {
        Edge newEdge = new Edge(fromNode, toNode);
        return newEdge;
    } 
}


