package com.example.sophieleaver.dumbotapp.test

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.sophieleaver.dumbotapp.javafiles.PerpendicularChildrenNode
import de.blox.graphview.*
import de.blox.graphview.Node
import de.blox.graphview.edgerenderer.EdgeRenderer
import de.blox.graphview.edgerenderer.StraightEdgeRenderer

class PerpLinesKotlinAlgorithm : Algorithm {

    private val clusterPadding = 100
    private val graphMargin = 48

    private var initEdgeLength = 20
    private var graphSize = Size(0, 0)
    private var edgeRenderer: EdgeRenderer = StraightEdgeRenderer()
    private var maxLeft: Float = 0f
    private var maxRight: Float = 0f
    private var maxTop: Float = 0f
    private var maxBottom: Float = 0f

    override fun run(graph: Graph) {

        calculateNodePositions(graph)

        positionNodes(graph)

        calculateGraphSize(graph)
    }

    private fun calculateNodePositions(graph: Graph) {

        val tempGraphSize = graph.nodeCount * initEdgeLength
        val midPoint = tempGraphSize / 2f

        graphSize = Size(tempGraphSize, tempGraphSize)

        maxLeft = midPoint
        maxRight = midPoint
        maxTop = midPoint
        maxBottom = midPoint

        initEdgeLength = graph.getNode(0).width + 20

        graph.getNode(0).setPos(Vector(midPoint, midPoint))

        val addedNodes = mutableListOf<Node>()
        val frontier = mutableListOf<Node>(graph.getNode(0))

        while (frontier.isNotEmpty()) {
            with(frontier.removeAt(0) as PerpendicularChildrenNode) {

                if (leftNode != null && !addedNodes.contains(leftNode!!)) {
                    frontier.add(leftNode!!)
                    drawLeftNode(
                        graph.getNode(this.data) as PerpendicularChildrenNode,
                        graph,
                        addedNodes,
                        frontier
                    )
                }

                if (rightNode != null && !addedNodes.contains(rightNode!!)) {
                    frontier.add(rightNode!!)
                    drawRightNode(
                        graph.getNode(this.data) as PerpendicularChildrenNode,
                        graph,
                        addedNodes,
                        frontier
                    )
                }

                if (topNode != null && !addedNodes.contains(topNode!!)) {
                    frontier.add(topNode!!)
                    drawTopNode(
                        graph.getNode(this.data) as PerpendicularChildrenNode,
                        graph,
                        addedNodes,
                        frontier
                    )
                }

                if (bottomNode != null && !addedNodes.contains(bottomNode!!)) {
                    frontier.add(bottomNode!!)
                    drawBottomNode(
                        graph.getNode(this.data) as PerpendicularChildrenNode,
                        graph,
                        addedNodes,
                        frontier
                    )
                }

                if (!addedNodes.contains(this)) addedNodes.add(this)
            }
        }

    }

    private fun drawBottomNode(
        currentNode: PerpendicularChildrenNode,
        graph: Graph,
        addedNodes: List<Node>,
        frontier: List<Node>
    ) {
        val newY = currentNode.y + initEdgeLength
        graph.getNode(currentNode.bottomNode!!.data).setPos(Vector(currentNode.x, newY))

        if (newY > maxBottom) {

            val nodesToEdit = frontier + addedNodes + currentNode
            val shiftAmount = Math.abs(maxBottom - newY) / 2f
            for (node in nodesToEdit) {
                graph.getNode(node.data).y = node.y - shiftAmount
            }
            maxBottom = graph.getNode(currentNode.bottomNode!!.data).y
        }

    }

    private fun drawTopNode(
        currentNode: PerpendicularChildrenNode,
        graph: Graph,
        addedNodes: List<Node>,
        frontier: List<Node>
    ) {
        val newY = currentNode.y - initEdgeLength
        graph.getNode(currentNode.topNode!!.data).setPos(Vector(currentNode.x, newY))

        if (newY < maxTop) {

            val nodesToEdit = frontier + addedNodes + currentNode
            val shiftAmount = Math.abs(maxTop - newY) / 2f
            for (node in nodesToEdit) {
                graph.getNode(node.data).y = node.y + shiftAmount
            }
            maxTop = graph.getNode(currentNode.topNode!!.data).y
        }

    }

    private fun drawRightNode(
        currentNode: PerpendicularChildrenNode,
        graph: Graph,
        addedNodes: List<Node>,
        frontier: List<Node>
    ) {
        val newX = currentNode.x + initEdgeLength
        graph.getNode(currentNode.rightNode!!.data).setPos(Vector(newX, currentNode.y))

        if (newX > maxRight) {

            val nodesToEdit = frontier + addedNodes + currentNode
            val shiftAmount = Math.abs(maxRight - newX) / 2f
            for (node in nodesToEdit) {
                graph.getNode(node.data).x = node.x - shiftAmount
            }
            maxRight = graph.getNode(currentNode.rightNode!!.data).x
        }

    }

    private fun drawLeftNode(
        currentNode: PerpendicularChildrenNode,
        graph: Graph,
        addedNodes: List<Node>,
        frontier: List<Node>
    ) {
        val newX = currentNode.x - initEdgeLength

        graph.getNode(currentNode.leftNode!!.data).setPos(Vector(newX, currentNode.y))

        if (newX < maxLeft) {

            val nodesToEdit = frontier + addedNodes + currentNode
            val shiftAmount = Math.abs(maxLeft - newX) / 2f
            for (node in nodesToEdit) {
                graph.getNode(node.data).x = node.x + shiftAmount
            }
            maxLeft = graph.getNode(currentNode.leftNode!!.data).x
        }

    }

    private fun positionNodes(graph: Graph) {
        val offset = getOffset(graph) //leftmost & topmost value of a node - add padding
        val nodesVisited: MutableList<Node> = mutableListOf()
        val nodeClusters: MutableList<NodeCluster> = mutableListOf()

        for (node in graph.nodes) {
            node.setPos(Vector(node.x - offset.x, node.y - offset.y))
        }

        for (node in graph.nodes) {
            if (nodesVisited.contains(node)) continue

            nodesVisited.add(node)
            var cluster = nodeClusters.find { it.contains(node) }
            if (cluster == null) {
                cluster = NodeCluster().apply { add(node) }
                nodeClusters.add(cluster)
            }

            followEdges(graph, cluster, node, nodesVisited)
        }

        positionCluster(nodeClusters)
    }

    private fun positionCluster(nodeClusters: MutableList<NodeCluster>) {
        combineSingleNodeCluster(nodeClusters)

        var cluster = nodeClusters[0]
        // move first cluster to 0,0
        cluster.offset(-cluster.rect!!.left + 24, -cluster.rect!!.top + 24)

        for (i in 1 until nodeClusters.size) {
            val nextCluster = nodeClusters[i]
            val xDiff = cluster.rect!!.right - nextCluster.rect!!.left + clusterPadding
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


    private fun getOffset(graph: Graph): Vector {
        var offsetX = Float.MAX_VALUE
        var offsetY = Float.MAX_VALUE
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

        graphSize = Size(right - left + graphMargin, bottom - top + graphMargin)
    }

    private inner class NodeCluster {
        val nodes: MutableList<Node> = mutableListOf()
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

        internal operator fun contains(node: Node): Boolean = nodes.contains(node)

        internal fun size(): Int = nodes.size

        internal fun concat(cluster: NodeCluster) {
            for (node in cluster.nodes) {
                add(node.apply { setPos(Vector(rect!!.right + clusterPadding, rect!!.top)) })
            }
        }

        internal fun offset(xDiff: Float, yDiff: Float) {
            nodes.forEach { it.setPos(it.position.add(xDiff, yDiff)) }
            rect!!.offset(xDiff, yDiff)
        }
    }
}

