package com.example.sophieleaver.dumbotapp.javafiles;

import de.blox.graphview.Node;

public class NewNode extends Node {

    private Node leftNode;
    private Node rightNode;
    private Node topNode;
    private Node bottomNode;

    public NewNode(Object data) {
        super(data);
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

    public void setupNodes(Node leftNode, Node rightNode, Node topNode, Node bottomNode) {
        this.leftNode = leftNode;
        this.rightNode = rightNode;
        this.topNode = topNode;
        this.bottomNode = bottomNode;
    }

}

