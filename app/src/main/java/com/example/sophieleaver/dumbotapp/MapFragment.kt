package com.example.sophieleaver.dumbotapp


import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.sophieleaver.dumbotapp.javafiles.PerpendicularChildrenNode
import com.example.sophieleaver.dumbotapp.javafiles.PerpendicularLinesAlgorithm
import com.github.clans.fab.FloatingActionMenu
import com.google.firebase.database.*
import com.otaliastudios.zoom.Alignment
import de.blox.graphview.*
import kotlinx.android.synthetic.main.fragment_map.view.*

//todo - add notice that you have to click node before adding works

/**
 * A simple [Fragment] subclass.
 * Use the [MapFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MapFragment : Fragment() {
    private val fragTag: String = "MapFragment"
    private val reference: DatabaseReference =
        FirebaseDatabase.getInstance().reference.child("demo2")

    private lateinit var fabMenu: FloatingActionMenu
    private lateinit var graphView: GraphView
    private lateinit var adapter: BaseGraphAdapter<ViewHolder>

    private var nodes: List<PerpendicularChildrenNode> = mutableListOf()
    private var edges: List<Edge> = mutableListOf()

    private var currentNode: PerpendicularChildrenNode? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        graphView = view.graph
        fabMenu = view.fabAddNode
        setupAdapter(createGraph())
        setupFAB()
    }

    private fun setupFAB() {
        with(fabMenu) {
            fabAddNodeLeft.setOnClickListener { showNodeTypeSelectionDialog(Direction.LEFT) }
            fabAddNodeRight.setOnClickListener { showNodeTypeSelectionDialog(Direction.RIGHT) }
            fabAddNodeTop.setOnClickListener { showNodeTypeSelectionDialog(Direction.TOP) }
            fabAddNodeBottom.setOnClickListener { showNodeTypeSelectionDialog(Direction.BOTTOM) }
        }
    }

    private fun setupAdapter(graph: Graph) {

        adapter = object : BaseGraphAdapter<ViewHolder>(graph) {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_map_node, parent, false)
                return SimpleViewHolder(view)
            }

            override fun onBindViewHolder(viewHolder: ViewHolder, data: Any, position: Int) {
                val nodeInfo = (data as String).run {
                    when {
                        startsWith("SA") -> Pair(
                            R.color.colorStorageNode,
                            "Storage Area ${removePrefix("SA")}"
                        )
                        startsWith("B") -> Pair(
                            R.color.colorBenchNode,
                            "Workout Station ${removePrefix("B")}"
                        )

                        else -> Pair(R.color.colorJunctionNode, "Junction $this")
                    }
                }

                (viewHolder as SimpleViewHolder).let {
                    it.itemView.background =
                        ContextCompat.getDrawable(
                            this@MapFragment.requireContext(),
                            nodeInfo.first
                        )
                    it.textView.text = nodeInfo.second
                }
            }

            inner class SimpleViewHolder(itemView: View) : ViewHolder(itemView) {
                var textView: TextView = itemView.findViewById(R.id.textView)
            }
        }

        adapter.algorithm = PerpendicularLinesAlgorithm()
        graphView.adapter = adapter
        graphView.setOnItemClickListener { _, _, position, _ ->
            adapter.getNode(position).let {
                currentNode = it as PerpendicularChildrenNode
                fabMenu.run { if (isOpened) close(true) }
                updateFAB()
                Snackbar.make(graphView, "Current Node is ${it.data}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFAB() {
        currentNode?.run {
            fabMenu.let {
                it.fabAddNodeLeft.visibility = if (leftNode == null) View.VISIBLE else View.GONE
                it.fabAddNodeRight.visibility = if (rightNode == null) View.VISIBLE else View.GONE
                it.fabAddNodeTop.visibility = if (topNode == null) View.VISIBLE else View.GONE
                it.fabAddNodeBottom.visibility = if (bottomNode == null) View.VISIBLE else View.GONE
            }
        }
    }

    private fun addNode(direction: Direction, nodePrefix: String) {
        with(adapter) {
            val newNode = PerpendicularChildrenNode("$nodePrefix ${nodes.size + 1}")
            graph.run {

                when (direction) {
                    Direction.LEFT -> {
                        (getNode(currentNode!!.data) as PerpendicularChildrenNode).leftNode =
                            newNode
                        newNode.rightNode =
                            (getNode(currentNode!!.data) as PerpendicularChildrenNode)
                    }
                    Direction.RIGHT -> {
                        (getNode(currentNode!!.data) as PerpendicularChildrenNode).rightNode =
                            newNode
                        newNode.leftNode =
                            (getNode(currentNode!!.data) as PerpendicularChildrenNode)
                    }
                    Direction.TOP -> {
                        (getNode(currentNode!!.data) as PerpendicularChildrenNode).topNode = newNode
                        newNode.bottomNode =
                            (getNode(currentNode!!.data) as PerpendicularChildrenNode)
                    }
                    Direction.BOTTOM -> {
                        (getNode(currentNode!!.data) as PerpendicularChildrenNode).bottomNode =
                            newNode
                        newNode.topNode = (getNode(currentNode!!.data) as PerpendicularChildrenNode)
                    }
                }

                addEdge(getNode(currentNode!!.data), newNode)
                this@MapFragment.nodes = nodes as List<PerpendicularChildrenNode>
            }

            notifyNodeAdded(newNode)
            graphView.zoomTo(1f, true)
            graphView.setAlignment(Alignment.CENTER)
        }
    }

    private fun showNodeTypeSelectionDialog(direction: Direction) {
        val nodeOptions = mapOf("Storage Area" to "SA", "Workout Station" to "B", "Other" to "")

        AlertDialog.Builder(requireContext()).apply {
            setTitle("Select Node Type")
            setItems(nodeOptions.keys.toTypedArray()) { _, i ->
                addNode(direction, nodeOptions.values.toList()[i])
            }
            create()
            show()
        }
    }

    private fun loadGraph() {
        reference.child("layout").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                with(dataSnapshot.children) {
                    //createNodes(find { it.key == "nodes" }?.getValue(String::class.java))
//                    setupAngles(find { it.key == "angles" }?.getValue(String::class.java))
//                    createEdges(find { it.key == "edges" }?.getValue(String::class.java))
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(fragTag, "Failed to load layout data", databaseError.toException())
            }

        })
    }

    private fun createNodes(nodeString: String?) {
        nodeString?.let {
            nodes = it.removeSurrounding("[", "]")
                .replace("\'", "")
                .split(",")
                .map { nodeId -> PerpendicularChildrenNode(nodeId) }
                .toList()
        }
    }

    private fun createEdges(edgeString: String?) {
        edgeString?.let {
            edges = it.removeSurrounding("[", "]")
                .replace("\'", "")
                .split("),")
                .map { edge ->
                    edge.removePrefix("(").split(",").run { buildEdge(this[0], this[1]) }
                }

        }
    }

    private fun setupAngles(angleString: String?) {
        if (angleString != null) {
            val angles =
                angleString.replace("\'", "").removeSurrounding("{", "}").trim().split("},")
                    .associateBy({
                        it.trim().removePrefix("(").substringBefore(')', "()").split(",")
                            .run { findEdge(this[0], this[1]) }
                    }) {
                        it.trim().substringAfter(')', "()").removePrefix(":{").removeSuffix("}")
                            .removePrefix("(")
                            .split(",(").map { test ->
                                test.replace("):", ",").split(",")
                                    .run {
                                        Pair(
                                            findEdge(this[0], this[1]),
                                            Direction.valueOf(this[2])
                                        )
                                    }
                            }
                            .run { mapOf(*this.toTypedArray()) }
                    }

            angles.values.forEach {

            }
            mapOf(*listOf(Pair(1, 2)).toTypedArray())
        }


    }

    private fun buildEdge(sourceId: String, destId: String): Edge =
        with(nodes) { Edge(find { it.data == sourceId }, find { it.data == destId }) }

    private fun findEdge(sourceId: String, destId: String): Edge =
        edges.find { (it.source.data == sourceId) and (it.destination.data == destId) }!!


    private fun writeToFirebase() {
        val nodeString = "[${nodes.joinToString { "'${it.data}'" }}]"
        val edgeString =
            "[${edges.joinToString { "('${it.source.data}','${it.destination.data}')" }}]"
        val anglesString: String = createAngleString()

        with(reference.child("layout")) {
            child("nodes").setValue(nodeString)
            child("edges").setValue(edgeString)
            child("angles").setValue(anglesString)
        }
    }

    //todo - add directions
    private fun createAngleString(): String {
        val angleString = edges.joinToString { edge ->
            val key = "('${edge.source.data}','${edge.destination.data}'):"
            val valueDict = edge.run {
                val sourceEdges =
                    with((source as PerpendicularChildrenNode)) { connectedNodes.joinToString { "('${this.data}','${it.data}' )" } }
                val destEdges =
                    with((destination as PerpendicularChildrenNode)) { connectedNodes.joinToString { "('${this.data}','${it.data}' )" } }
                "{$sourceEdges,$destEdges}"
            }
            key + valueDict
        }
        return "{$angleString}"
    }

    private fun createGraph(): Graph {

        val node1 = PerpendicularChildrenNode("1")
        val node2 = PerpendicularChildrenNode("2")
        val node3 = PerpendicularChildrenNode("SA3")
        val node4 = PerpendicularChildrenNode("4")
        val node5 = PerpendicularChildrenNode("5")
        val node6 = PerpendicularChildrenNode("B6")
        val node7 = PerpendicularChildrenNode("SA7")
        val node8 = PerpendicularChildrenNode("B8")
        val node9 = PerpendicularChildrenNode("B9")
        val node0 = PerpendicularChildrenNode("B10")

        node1.setupNodes(node5, node3, node2, node4)
        node2.setupNodes(null, null, node6, node1)
        node3.setupNodes(node1, null, node7, null)
        node4.setupNodes(null, node9, node1, node0)
        node5.setupNodes(node8, node1, null, null)
        node6.setupNodes(null, null, null, node2)
        node7.setupNodes(null, null, null, node3)
        node8.setupNodes(null, node5, null, null)
        node9.setupNodes(node4, null, null, null)
        node0.setupNodes(null, null, node4, null)

        return Graph().apply {
            addEdge(node1, node2)
            addEdge(node1, node3)
            addEdge(node1, node4)
            addEdge(node1, node5)

            addEdge(node2, node6)

            addEdge(node3, node7)

            addEdge(node4, node9)
            addEdge(node4, node0)

            addEdge(node5, node8)
        }.also { nodes = it.nodes as List<PerpendicularChildrenNode> }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MapFragment.
         */
        @JvmStatic
        fun newInstance() = MapFragment()
    }

    internal enum class Direction(code: Char) {
        LEFT('A'), RIGHT('C'), TOP('F'), BOTTOM('B')
    }
}
