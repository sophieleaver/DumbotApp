package com.example.sophieleaver.dumbotapp.javafiles;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.blox.graphview.Algorithm;
import de.blox.graphview.Graph;
import de.blox.graphview.Node;
import de.blox.graphview.Size;
import de.blox.graphview.Vector;
import de.blox.graphview.edgerenderer.EdgeRenderer;
import de.blox.graphview.edgerenderer.StraightEdgeRenderer;

public class PerpendicularLinesAlgorithm implements Algorithm {

    private static final int CLUSTER_PADDING = 0;
    private int initEdgeLength = 0;
    private Size graphSize = new Size(0, 0);
    private EdgeRenderer edgeRenderer = new StraightEdgeRenderer(); // or ArrowEdgeRender() or custom implementation

    private float maxLeft;
    private float maxRight;
    private float maxTop;
    private float maxBottom;

    @Override
    public void run(@NotNull Graph graph) {

        initPositionNodes(graph);

        positionNodes(graph);

        calculateGraphSize(graph);
    }

    private void initPositionNodes(Graph graph) {

        List<Node> frontier = new ArrayList<>();
        List<Node> addedNodes = new ArrayList<>();

        Node firstNode = graph.getNode(0);
        initEdgeLength = firstNode.getWidth() + 150;
        int tempGraphSize = graph.getNodeCount() * initEdgeLength;
        graphSize = new Size(tempGraphSize, tempGraphSize);

        float midPoint = tempGraphSize / 2f;
        maxLeft = midPoint;
        maxRight = midPoint;
        maxTop = midPoint;
        maxBottom = midPoint;

        graph.getNode(0).setPos(new Vector(midPoint, midPoint));

        frontier.add(firstNode);

        PerpendicularChildrenNode currentNode;

        while (!frontier.isEmpty()) {
            currentNode = (PerpendicularChildrenNode) frontier.remove(0);

            if (currentNode.getLeftNode() != null && !addedNodes.contains(currentNode.getLeftNode())) {
                frontier.add(currentNode.getLeftNode());
                drawLeftNode((PerpendicularChildrenNode) getNode(graph, currentNode), graph, addedNodes, frontier);
            }

            if (currentNode.getRightNode() != null && !addedNodes.contains(currentNode.getRightNode())) {
                frontier.add(currentNode.getRightNode());
                drawRightNode((PerpendicularChildrenNode) getNode(graph, currentNode), graph, addedNodes, frontier);
            }

            if (currentNode.getTopNode() != null && !addedNodes.contains(currentNode.getTopNode())) {
                frontier.add(currentNode.getTopNode());
                drawTopNode((PerpendicularChildrenNode) getNode(graph, currentNode), graph, addedNodes, frontier);
            }

            if (currentNode.getBottomNode() != null && !addedNodes.contains(currentNode.getBottomNode())) {
                frontier.add(currentNode.getBottomNode());
                drawBottomNode((PerpendicularChildrenNode) getNode(graph, currentNode), graph, addedNodes, frontier);
            }

            if (!addedNodes.contains(currentNode)) addedNodes.add(currentNode);
        }

    }

    private void drawBottomNode(PerpendicularChildrenNode currentNode, Graph graph, List<Node> addedNodes, List<Node> frontier) {
        float newY = currentNode.getY() + initEdgeLength;
        getNode(graph, currentNode.getBottomNode()).setPos(new Vector(currentNode.getX(), newY));

        if (newY > maxBottom) {

            ArrayList<Node> nodesToEdit = new ArrayList<>();
            nodesToEdit.addAll(frontier);
            nodesToEdit.addAll(addedNodes);
            nodesToEdit.add(currentNode);

            float difference = Math.abs(maxBottom - newY);
            float shiftAmount = difference / 2f;
            for (Node node : nodesToEdit) {
                getNode(graph, node).setY(node.getY() - shiftAmount);
            }
            maxBottom = getNode(graph, currentNode.getBottomNode()).getY();
        }

    }

    private void drawTopNode(PerpendicularChildrenNode currentNode, Graph graph, List<Node> addedNodes, List<Node> frontier) {
        float newY = currentNode.getY() - initEdgeLength;
        getNode(graph, currentNode.getTopNode()).setPos(new Vector(currentNode.getX(), newY));

        if (newY < maxTop) {

            ArrayList<Node> nodesToEdit = new ArrayList<>();
            nodesToEdit.addAll(frontier);
            nodesToEdit.addAll(addedNodes);
            nodesToEdit.add(currentNode);

            float difference = Math.abs(maxTop - newY);
            float shiftAmount = difference / 2f;
            for (Node node : nodesToEdit) {
                getNode(graph, node).setY(node.getY() + shiftAmount);
            }
            maxTop = getNode(graph, currentNode.getTopNode()).getY();
        }

    }

    private void drawRightNode(PerpendicularChildrenNode currentNode, Graph graph, List<Node> addedNodes, List<Node> frontier) {
        float newX = currentNode.getX() + initEdgeLength;
        getNode(graph, currentNode.getRightNode()).setPos(new Vector(newX, currentNode.getY()));

        if (newX > maxRight) {

            ArrayList<Node> nodesToEdit = new ArrayList<>();
            nodesToEdit.addAll(frontier);
            nodesToEdit.addAll(addedNodes);
            nodesToEdit.add(currentNode);

            float difference = Math.abs(maxRight - newX);
            float shiftAmount = difference / 2f;
            for (Node node : nodesToEdit) {
                getNode(graph, node).setX(node.getX() - shiftAmount);
            }
            maxRight = getNode(graph, currentNode.getRightNode()).getX();
        }

    }

    private void drawLeftNode(PerpendicularChildrenNode currentNode, Graph graph, List<Node> addedNodes, List<Node> frontier) {
        //currentNode = graph node
        float newX = currentNode.getX() - initEdgeLength;

        //edit left graph node
        getNode(graph, currentNode.getLeftNode()).setPos(new Vector(newX, currentNode.getY()));

        if (newX < maxLeft) {

            ArrayList<Node> nodesToEdit = new ArrayList<>();
            nodesToEdit.addAll(frontier);
            nodesToEdit.addAll(addedNodes);
            nodesToEdit.add(currentNode);

            float difference = Math.abs(maxLeft - newX);
            float shiftAmount = difference / 2f;
            for (Node node : nodesToEdit) {
                getNode(graph, node).setX(node.getX() + shiftAmount);
            }
            maxLeft = getNode(graph, currentNode.getLeftNode()).getX();
        }

    }

    private Node getNode(Graph graph, Object data) {
        if (data instanceof Node) {
            data = ((Node) data).getData();
        }
        return graph.getNode(data);
    }

    private void positionNodes(Graph graph) {
        Vector offset = getOffset(graph); //leftmost & topmost value of a node - add padding
        List<Node> nodesVisited = new ArrayList<>();
        List<NodeCluster> nodeClusters = new ArrayList<>();
        for (Node node : graph.getNodes()) {
            node.setPos(new Vector(node.getX() - offset.getX(), node.getY() - offset.getY()));
        }

        for (Node node : graph.getNodes()) {
            if (nodesVisited.contains(node)) {
                continue;
            }

            nodesVisited.add(node);
            NodeCluster cluster = findClusterOf(nodeClusters, node);
            if (cluster == null) {
                cluster = new NodeCluster();
                cluster.add(node);
                nodeClusters.add(cluster);
            }

            followEdges(graph, cluster, node, nodesVisited);
        }

        positionCluster(nodeClusters);
    }

    private void positionCluster(List<NodeCluster> nodeClusters) {
        combineSingleNodeCluster(nodeClusters);

        NodeCluster cluster = nodeClusters.get(0);
        // move first cluster to 0,0
        cluster.offset(-cluster.rect.left + 24, -cluster.rect.top + 24);

        for (int i = 1; i < nodeClusters.size(); i++) {
            final NodeCluster nextCluster = nodeClusters.get(i);
            final float xDiff = cluster.rect.right - nextCluster.rect.left + CLUSTER_PADDING;
            final float yDiff = cluster.rect.top - nextCluster.rect.top;
            nextCluster.offset(xDiff, yDiff);
            cluster = nextCluster;
        }
    }


    private void combineSingleNodeCluster(List<NodeCluster> nodeClusters) {
        NodeCluster firstSingleNodeCluster = null;
        final Iterator<NodeCluster> iterator = nodeClusters.iterator();
        while (iterator.hasNext()) {
            NodeCluster cluster = iterator.next();
            if (cluster.size() == 1) {
                if (firstSingleNodeCluster == null) {
                    firstSingleNodeCluster = cluster;
                    continue;
                }

                firstSingleNodeCluster.concat(cluster);
                iterator.remove();
            }
        }
    }

    private void followEdges(Graph graph, NodeCluster cluster, Node node, List<Node> nodesVisited) {
        for (Node successor : graph.successorsOf(node)) {
            if (nodesVisited.contains(successor)) {
                continue;
            }

            nodesVisited.add(successor);
            cluster.add(successor);

            followEdges(graph, cluster, successor, nodesVisited);
        }

        for (Node predecessor : graph.predecessorsOf(node)) {
            if (nodesVisited.contains(predecessor)) {
                continue;
            }

            nodesVisited.add(predecessor);
            cluster.add(predecessor);

            followEdges(graph, cluster, predecessor, nodesVisited);
        }
    }

    private NodeCluster findClusterOf(List<NodeCluster> clusters, Node node) {
        for (NodeCluster cluster : clusters) {
            if (cluster.contains(node)) {
                return cluster;
            }
        }

        return null;
    }


    private Vector getOffset(Graph graph) {
        float offsetX = Float.MAX_VALUE;
        float offsetY = Float.MAX_VALUE;
        for (Node node : graph.getNodes()) {
            offsetX = Math.min(offsetX, node.getX());
            offsetY = Math.min(offsetY, node.getY());
        }
        return new Vector(offsetX, offsetY);
    }

    @NotNull
    @Override
    public Size getGraphSize() {
        return graphSize;
    }

    @Override
    public void drawEdges(@NotNull Canvas canvas, @NotNull Graph graph, @NotNull Paint linePaint) {
        edgeRenderer.render(canvas, graph, linePaint);
    }

    @Override
    public void setEdgeRenderer(@NotNull EdgeRenderer renderer) {
        edgeRenderer = renderer;
    }

    private void calculateGraphSize(Graph graph) {

        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (Node node : graph.getNodes()) {
            left = (int) Math.min(left, node.getX());
            top = (int) Math.min(top, node.getY());
            right = (int) Math.max(right, node.getX() + node.getWidth());
            bottom = (int) Math.max(bottom, node.getY() + node.getHeight());
        }

        graphSize = new Size(right - left + 48, bottom - top + 48);
    }

    private static class NodeCluster {
        private List<Node> nodes = new ArrayList<>();
        private RectF rect;

        void add(Node node) {
            nodes.add(node);

            if (rect == null) {
                rect = new RectF(node.getX(), node.getY(), node.getX() + node.getWidth(), node.getY() + node.getHeight());
            } else {
                rect.left = Math.min(rect.left, node.getX());
                rect.top = Math.min(rect.top, node.getY());
                rect.right = Math.max(rect.right, node.getX() + node.getWidth());
                rect.bottom = Math.max(rect.bottom, node.getY() + node.getHeight());
            }
        }

        boolean contains(Node node) {
            return nodes.contains(node);
        }

        int size() {
            return nodes.size();
        }

        void concat(NodeCluster cluster) {
            for (Node node : cluster.nodes) {
                node.setPos(new Vector(rect.right + CLUSTER_PADDING, rect.top));
                add(node);
            }
        }

        void offset(float xDiff, float yDiff) {
            for (Node node : nodes) {
                node.setPos(node.getPosition().add(xDiff, yDiff));
            }

            rect.offset(xDiff, yDiff);
        }
    }
}

