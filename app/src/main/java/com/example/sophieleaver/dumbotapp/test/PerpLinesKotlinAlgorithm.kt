package com.example.sophieleaver.dumbotapp.test

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.example.sophieleaver.dumbotapp.javafiles.NewGraph
import com.example.sophieleaver.dumbotapp.javafiles.NewNode
import de.blox.graphview.*
import de.blox.graphview.Node
import de.blox.graphview.Vector
import de.blox.graphview.edgerenderer.EdgeRenderer
import de.blox.graphview.edgerenderer.StraightEdgeRenderer
import java.util.*

class PerpLinesKotlinAlgorithm : Algorithm {

    private var initEdgeLength = 20
    private var graphSize = Size(0, 0)
    private var edgeRenderer: EdgeRenderer =
        StraightEdgeRenderer() // or ArrowEdgeRender() or custom implementation

    private var maxLeft: Float = 0.toFloat()
    private var maxRight: Float = 0.toFloat()
    private var maxTop: Float = 0.toFloat()
    private var maxBottom: Float = 0.toFloat()

    override fun run(graph: Graph) {

        initPositionNodes(graph as NewGraph)

        positionNodes(graph)

        calculateGraphSize(graph)
    }

    private fun initPositionNodes(graph: NewGraph) {

        val frontier = ArrayList<Node>()
        val addedNodes = ArrayList<Node>()

        val firstNode = graph.nodes[0]
        initEdgeLength += firstNode.width
        val tempGraphSize = graph.nodeCount * initEdgeLength
        graphSize = Size(tempGraphSize, tempGraphSize)

        val midPoint = tempGraphSize / 2f
        maxLeft = midPoint
        maxRight = midPoint
        maxTop = midPoint
        maxBottom = midPoint

        graph.getNode(0).setPos(Vector(midPoint, midPoint))

        frontier.add(firstNode)

        var currentNode: NewNode

        while (!frontier.isEmpty()) {
            currentNode = frontier.removeAt(0) as NewNode

            if (currentNode.leftNode != null && !addedNodes.contains(currentNode.leftNode!!)) {
                frontier.add(currentNode.leftNode!!)
                drawLeftNode(graph.getNode(currentNode) as NewNode, graph, addedNodes, frontier)
            }

            if (currentNode.rightNode != null && !addedNodes.contains(currentNode.rightNode!!)) {
                frontier.add(currentNode.rightNode!!)
                drawRightNode(graph.getNode(currentNode) as NewNode, graph, addedNodes, frontier)
            }

            if (currentNode.topNode != null && !addedNodes.contains(currentNode.topNode!!)) {
                frontier.add(currentNode.topNode!!)
                drawTopNode(graph.getNode(currentNode) as NewNode, graph, addedNodes, frontier)
            }

            if (currentNode.bottomNode != null && !addedNodes.contains(currentNode.bottomNode!!)) {
                frontier.add(currentNode.bottomNode!!)
                drawBottomNode(graph.getNode(currentNode) as NewNode, graph, addedNodes, frontier)
            }

            if (!addedNodes.contains(currentNode)) addedNodes.add(currentNode)
        }

    }

    private fun drawBottomNode(
        currentNode: NewNode,
        graph: Graph,
        addedNodes: List<Node>,
        frontier: List<Node>
    ) {
        val newY = currentNode.y + initEdgeLength
        graph.getNode(currentNode.bottomNode).setPos(Vector(currentNode.x, newY))

        if (newY > maxBottom) {

            val nodesToEdit = ArrayList<Node>()
            nodesToEdit.addAll(frontier)
            nodesToEdit.addAll(addedNodes)
            nodesToEdit.add(currentNode)

            val difference = Math.abs(maxBottom - newY)
            val shiftAmount = difference / 2f
            for (node in nodesToEdit) {
                graph.getNode(node).y = node.y - shiftAmount
            }
            maxBottom = graph.getNode(currentNode.bottomNode).y
        }

    }

    private fun drawTopNode(
        currentNode: NewNode,
        graph: Graph,
        addedNodes: List<Node>,
        frontier: List<Node>
    ) {
        val newY = currentNode.y - initEdgeLength
        graph.getNode(currentNode.topNode).setPos(Vector(currentNode.x, newY))

        if (newY < maxTop) {

            val nodesToEdit = ArrayList<Node>()
            nodesToEdit.addAll(frontier)
            nodesToEdit.addAll(addedNodes)
            nodesToEdit.add(currentNode)

            val difference = Math.abs(maxTop - newY)
            val shiftAmount = difference / 2f
            for (node in nodesToEdit) {
                graph.getNode(node).y = node.y + shiftAmount
            }
            maxTop = graph.getNode(currentNode.topNode).y
        }

    }

    private fun drawRightNode(
        currentNode: NewNode,
        graph: Graph,
        addedNodes: List<Node>,
        frontier: List<Node>
    ) {
        val newX = currentNode.x + initEdgeLength
        graph.getNode(currentNode.rightNode).setPos(Vector(newX, currentNode.y))

        if (newX > maxRight) {

            val nodesToEdit = ArrayList<Node>()
            nodesToEdit.addAll(frontier)
            nodesToEdit.addAll(addedNodes)
            nodesToEdit.add(currentNode)

            val difference = Math.abs(maxRight - newX)
            val shiftAmount = difference / 2f
            for (node in nodesToEdit) {
                graph.getNode(node).x = node.x - shiftAmount
            }
            maxRight = graph.getNode(currentNode.rightNode).x
        }

    }

    private fun drawLeftNode(
        currentNode: NewNode,
        graph: Graph,
        addedNodes: List<Node>,
        frontier: List<Node>
    ) {
        //currentNode = graph node
        val newX = currentNode.x - initEdgeLength

        //edit left graph node
        graph.getNode(currentNode.leftNode).setPos(Vector(newX, currentNode.y))

        if (newX < maxLeft) {

            val nodesToEdit = ArrayList<Node>()
            nodesToEdit.addAll(frontier)
            nodesToEdit.addAll(addedNodes)
            nodesToEdit.add(currentNode)

            val difference = Math.abs(maxLeft - newX)
            val shiftAmount = difference / 2f
            for (node in nodesToEdit) {
                graph.getNode(node).x = node.x + shiftAmount
            }
            maxLeft = graph.getNode(currentNode.leftNode).x
        }

    }

    private fun positionNodes(graph: Graph) {
        val offset = getOffset(graph) //leftmost & topmost value of a node - add padding
        val nodesVisited = ArrayList<Node>()
        val nodeClusters = ArrayList<NodeCluster>()
        for (node in graph.nodes) {
            node.setPos(Vector(node.x - offset.x, node.y - offset.y))
        }

        for (node in graph.nodes) {
            if (nodesVisited.contains(node)) {
                continue
            }

            nodesVisited.add(node)
            var cluster = findClusterOf(nodeClusters, node)
            if (cluster == null) {
                cluster = NodeCluster()
                cluster.add(node)
                nodeClusters.add(cluster)
            }

            followEdges(graph, cluster, node, nodesVisited)
        }

        positionCluster(nodeClusters)
    }

    private fun positionCluster(nodeClusters: MutableList<NodeCluster>) {
        Log.d("TestAlgo", "positionCluster()")
        combineSingleNodeCluster(nodeClusters)

        var cluster = nodeClusters[0]
        // move first cluster to 0,0
        cluster.offset(-cluster.rect!!.left + 24, -cluster.rect!!.top + 24)

        Log.d("TestAlgo", "Starting loop through " + nodeClusters.size + " clusters")
        for (i in 1 until nodeClusters.size) {
            val nextCluster = nodeClusters[i]
            val xDiff = cluster.rect!!.right - nextCluster.rect!!.left + CLUSTER_PADDING
            Log.d("TestAlgo", "cluster top = " + cluster.rect!!.top)
            val yDiff = cluster.rect!!.top - nextCluster.rect!!.top
            nextCluster.offset(xDiff, yDiff)
            cluster = nextCluster
        }
    }


    private fun combineSingleNodeCluster(nodeClusters: MutableList<NodeCluster>) {
        var firstSingleNodeCluster: NodeCluster? = null
        val iterator = nodeClusters.iterator()
        while (iterator.hasNext()) {
            val cluster = iterator.next()
            if (cluster.size() == 1) {
                if (firstSingleNodeCluster == null) {
                    firstSingleNodeCluster = cluster
                    continue
                }

                firstSingleNodeCluster.concat(cluster)
                iterator.remove()
            }
        }
    }

    private fun followEdges(
        graph: Graph,
        cluster: NodeCluster,
        node: Node,
        nodesVisited: MutableList<Node>
    ) {
        for (successor in graph.successorsOf(node)) {
            if (nodesVisited.contains(successor)) {
                continue
            }

            nodesVisited.add(successor)
            cluster.add(successor)

            followEdges(graph, cluster, successor, nodesVisited)
        }

        for (predecessor in graph.predecessorsOf(node)) {
            if (nodesVisited.contains(predecessor)) {
                continue
            }

            nodesVisited.add(predecessor)
            cluster.add(predecessor)

            followEdges(graph, cluster, predecessor, nodesVisited)
        }
    }

    private fun findClusterOf(clusters: List<NodeCluster>, node: Node): NodeCluster? {
        for (cluster in clusters) {
            if (cluster.contains(node)) {
                return cluster
            }
        }

        return null
    }


    private fun getOffset(graph: Graph): Vector {
        var offsetX = java.lang.Float.MAX_VALUE
        var offsetY = java.lang.Float.MAX_VALUE
        for (node in graph.nodes) {
            offsetX = Math.min(offsetX, node.x)
            offsetY = Math.min(offsetY, node.y)
        }
        return Vector(offsetX, offsetY)
    }

    override fun getGraphSize(): Size {
        return graphSize
    }

    override fun drawEdges(canvas: Canvas, graph: Graph, linePaint: Paint) {
        edgeRenderer.render(canvas, graph, linePaint)
    }

    override fun setEdgeRenderer(renderer: EdgeRenderer) {
        edgeRenderer = renderer
    }

    private fun calculateGraphSize(graph: Graph) {

        var left = Integer.MAX_VALUE
        var top = Integer.MAX_VALUE
        var right = Integer.MIN_VALUE
        var bottom = Integer.MIN_VALUE
        for (node in graph.nodes) {
            left = Math.min(left.toFloat(), node.x).toInt()
            top = Math.min(top.toFloat(), node.y).toInt()
            right = Math.max(right.toFloat(), node.x + node.width).toInt()
            bottom = Math.max(bottom.toFloat(), node.y + node.height).toInt()
        }

        graphSize = Size(right - left + 48, bottom - top + 48)
    }

    private class NodeCluster {
        val nodes = ArrayList<Node>()
        var rect: RectF? = null

        internal fun add(node: Node) {
            nodes.add(node)

            if (rect == null) {
                rect = RectF(node.x, node.y, node.x + node.width, node.y + node.height)
            } else {
                rect!!.left = Math.min(rect!!.left, node.x)
                rect!!.top = Math.min(rect!!.top, node.y)
                rect!!.right = Math.max(rect!!.right, node.x + node.width)
                rect!!.bottom = Math.max(rect!!.bottom, node.y + node.height)
            }
        }

        internal operator fun contains(node: Node): Boolean {
            return nodes.contains(node)
        }

        internal fun size(): Int {
            return nodes.size
        }

        internal fun concat(cluster: NodeCluster) {
            for (node in cluster.nodes) {
                node.setPos(Vector(rect!!.right + CLUSTER_PADDING, rect!!.top))
                add(node)
            }
        }

        internal fun offset(xDiff: Float, yDiff: Float) {
            for (node in nodes) {
                node.setPos(node.position.add(xDiff, yDiff))
            }

            rect!!.offset(xDiff, yDiff)
        }
    }

    companion object {

        private val CLUSTER_PADDING = 100
    }
}

