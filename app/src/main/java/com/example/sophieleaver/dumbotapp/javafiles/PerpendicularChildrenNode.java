package com.example.sophieleaver.dumbotapp.javafiles;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.sophieleaver.dumbotapp.test.NodeType;

import java.util.ArrayList;
import java.util.List;

import de.blox.graphview.Node;

public class PerpendicularChildrenNode extends Node {

    private Node leftNode;
    private Node rightNode;
    private Node topNode;
    private Node bottomNode;
    private NodeType type = NodeType.JUNCTION;

    public PerpendicularChildrenNode(Object data) {
        super(data);
    }

    public PerpendicularChildrenNode(Object data, NodeType type) {
        super(data);
        this.type = type;
    }

    @Nullable
    public Node getLeftNode() {
        return leftNode;
    }

    public void setLeftNode(Node leftNode) {
        this.leftNode = leftNode;
    }

    @Nullable
    public Node getRightNode() {
        return rightNode;
    }

    public void setRightNode(Node rightNode) {
        this.rightNode = rightNode;
    }

    @Nullable
    public Node getTopNode() {
        return topNode;
    }

    public void setTopNode(Node topNode) {
        this.topNode = topNode;
    }

    @Nullable
    public Node getBottomNode() {
        return bottomNode;
    }

    public void setBottomNode(Node bottomNode) {
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

    @NonNull
    @Override
    public String toString() {
        String leftNodeDesc;
        String rightNodeDesc;
        String topNodeDesc;
        String bottomNodeDesc;

        if (leftNode != null) {
            leftNodeDesc = (String) leftNode.getData();
        } else {
            leftNodeDesc = "";
        }

        if (rightNode != null) {
            rightNodeDesc = (String) rightNode.getData();
        } else {
            rightNodeDesc = "";
        }

        if (topNode != null) {
            topNodeDesc = (String) topNode.getData();
        } else {
            topNodeDesc = "";
        }

        if (bottomNode != null) {
            bottomNodeDesc = (String) bottomNode.getData();
        } else {
            bottomNodeDesc = "";
        }


        String nodeDesc = String.format("{ leftNode = %s, rightNode = %s, topNode = %s, botttomNode = %s }",
                leftNodeDesc, rightNodeDesc, topNodeDesc, bottomNodeDesc);

        return super.toString() + nodeDesc;
    }
}



