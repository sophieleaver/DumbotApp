package com.example.sophieleaver.dumbotapp.javafiles;

import com.example.sophieleaver.dumbotapp.test.NodeType;

import java.util.ArrayList;
import java.util.List;

import de.blox.graphview.Node;

public class NewNode extends Node {

    private Node leftNode;
    private Node rightNode;
    private Node topNode;
    private Node bottomNode;
    private NodeType type = NodeType.JUNCTION;

    public NewNode(Object data) {
        super(data);
    }

    public NewNode(Object data, NodeType type) {
        super(data);
        this.type = type;
    }

    Node getLeftNode() {
        return leftNode;
    }

    void setLeftNode(Node leftNode) {
        this.leftNode = leftNode;
    }

    Node getRightNode() {
        return rightNode;
    }

    void setRightNode(Node rightNode) {
        this.rightNode = rightNode;
    }

    Node getTopNode() {
        return topNode;
    }

    void setTopNode(Node topNode) {
        this.topNode = topNode;
    }

    Node getBottomNode() {
        return bottomNode;
    }

    void setBottomNode(Node bottomNode) {
        this.bottomNode = bottomNode;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public List<Node> getConnectedNodes() {
        List<Node> result = new ArrayList<>();
        if (leftNode != null) {
            result.add(leftNode);
        }

        if (rightNode != null) {
            result.add(rightNode);
        }

        if (topNode != null) {
            result.add(topNode);
        }

        if (bottomNode != null) {
            result.add(bottomNode);
        }

        return result;
    }

    public void setupNodes(Node leftNode, Node rightNode, Node topNode, Node bottomNode) {
        this.leftNode = leftNode;
        this.rightNode = rightNode;
        this.topNode = topNode;
        this.bottomNode = bottomNode;
    }

}

