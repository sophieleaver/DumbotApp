package com.example.sophieleaver.dumbotapp.javafiles;

import de.blox.graphview.Graph;
import de.blox.graphview.Node;

public class NewGraph extends Graph {
    @Override
    public Node getNode(Object data) {
        if (data instanceof Node) {
            data = ((Node) data).getData();
        }
        return super.getNode(data);
    }
}