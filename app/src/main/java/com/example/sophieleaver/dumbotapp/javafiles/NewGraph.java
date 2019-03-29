package com.example.sophieleaver.dumbotapp.javafiles;

import com.example.sophieleaver.dumbotapp.MapFragment;

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

    public void updateNode(NewNode node, MapFragment.Direction direction) {
        switch (direction) {

//            case LEFT:
//                ((NewNode) getNode(node)).setupNodes(node.getConnectedNodes());
//            case RIGHT:
//                ((NewNode) getNode(node)).setupNodes(node.);
//            case TOP:
//                ((NewNode) getNode(node)).setupNodes(node.);
//            case BOTTOM:
//                ((NewNode) getNode(node)).setupNodes(node.);
        }
    }
}