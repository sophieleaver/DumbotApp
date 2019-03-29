package com.example.sophieleaver.dumbotapp


import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.sophieleaver.dumbotapp.javafiles.NewGraph
import com.example.sophieleaver.dumbotapp.javafiles.NewNode
import com.example.sophieleaver.dumbotapp.test.PerpLinesKotlinAlgorithm
import com.github.clans.fab.FloatingActionMenu
import com.google.firebase.database.*
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

    private var nodes: MutableList<NewNode> = mutableListOf()
    private var edges: MutableList<Edge> = mutableListOf()

    private var currentNode: NewNode? = null
    private lateinit var fabMenu: FloatingActionMenu
    private lateinit var graphView: GraphView
    private lateinit var adapter: BaseGraphAdapter<ViewHolder>


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
        fabMenu = view.fabAddNode
        graphView = view.graph
        setupAdapter(createGraph())
        setupFAB()
    }

    private fun setupFAB() {

    }

    private fun setupAdapter(graph: Graph) {

        adapter = object : BaseGraphAdapter<ViewHolder>(graph) {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_map_node, parent, false)
                return SimpleViewHolder(view)
            }

            override fun onBindViewHolder(viewHolder: ViewHolder, data: Any, position: Int) {
                val nodeInfo = with(data as String) {
                    when {
                        startsWith("SA") -> Pair(
                            R.color.colorStorageNode,
                            "Storage Area ${removePrefix("SA")}"
                        )
                        startsWith("B") -> {
                            Pair(
                                R.color.colorBenchNode,
                                "Workout Station ${removePrefix("B")}"
                            )
                        }
                        else -> {
                            Pair(R.color.colorJunctionNode, "Junction $this")
                        }
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

        adapter.algorithm = PerpLinesKotlinAlgorithm()
        graphView.adapter = adapter
        graphView.setOnItemClickListener { _, _, position, _ ->
            adapter.getNode(position).let {
                currentNode = it as NewNode?
                updateFAB()
                Snackbar.make(graphView, "Current Node is $it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFAB() {
        currentNode?.run {
            fabMenu.let {
                it.fabAddNodeLeft.visibility =
                    if (leftNode == null) View.VISIBLE else View.INVISIBLE
                it.fabAddNodeRight.visibility =
                    if (rightNode == null) View.VISIBLE else View.INVISIBLE
                it.fabAddNodeTop.visibility = if (topNode == null) View.VISIBLE else View.INVISIBLE
                it.fabAddNodeBottom.visibility =
                    if (bottomNode == null) View.VISIBLE else View.INVISIBLE

//                it.fabAddNodeLeft.set
            }
        }
    }

    private fun addNode(direction: Direction) {
        val newNode = NewNode("New Node ${nodes.size + 1}")

        when (direction) {
            Direction.LEFT -> {
                currentNode!!.leftNode = newNode
                newNode.rightNode = currentNode
            }
            Direction.RIGHT -> {
                currentNode!!.rightNode = newNode
                newNode.leftNode = currentNode
            }
            Direction.TOP -> {
                currentNode!!.topNode = newNode
                newNode.bottomNode = currentNode
            }
            Direction.BOTTOM -> {
                currentNode!!.bottomNode = newNode
                newNode.topNode = currentNode
            }
        }

        nodes.add(newNode)
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
        nodes = nodeString?.removeSurrounding("[", "]")
            ?.replace("\'", "")
            ?.split(",")
            ?.map { nodeId -> NewNode(nodeId) }
            ?.toMutableList()
            ?: mutableListOf()
    }

    private fun createEdges(edgeString: String?) {
        if (edgeString != null) {
            val edgePairs =
                edgeString.removeSurrounding("[", "]")
                    .replace("\'", "")
                    .split("),")
                    .map { edge ->
                        edge.removePrefix("(").split(",").run { Pair(this[0], this[1]) }
                    }

        }
    }

    private fun setupAngles(angleString: String?) {
        if (angleString != null) {
            val angles =
                angleString.replace("\'", "").removeSurrounding("{", "}").trim().split("},")
                    .associateBy({
                        it.trim().removePrefix("(").substringBefore(')', "()").split(",")
                            .run { Pair(this[0], this[1]) }
                    }) {
                        it.trim().substringAfter(')', "()").removePrefix(":{").removeSuffix("}")
                            .removePrefix("(")
                            .split(",(").map { test ->
                                test.replace("):", ",").split(",")
                                    .run { Triple(this[0], this[1], this[2]) }
                            }
                    }
        }
    }

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

    private fun createAngleString(): String {
        val result = edges.joinToString { edge ->
            val key = "('${edge.source.data}','${edge.destination.data}'):"
            val valueDict = edge.run {
                val sourceEdges =
                    with((source as NewNode)) { connectedNodes.joinToString { "('${this.data}','${it.data}' )" } }
                val destEdges =
                    with((destination as NewNode)) { connectedNodes.joinToString { "('${this.data}','${it.data}' )" } }
                "{$sourceEdges,$destEdges}"
            }
            key + valueDict
        }
        return "{$result}"
    }

    private fun createGraph(): NewGraph {

        val node1 = NewNode("1")
        val node2 = NewNode("2")
        val node3 = NewNode("SA3")
        val node4 = NewNode("4")
        val node5 = NewNode("5")
        val node6 = NewNode("B6")
        val node7 = NewNode("SA7")
        val node8 = NewNode("B8")
        val node9 = NewNode("B9")
        val node0 = NewNode("B10")

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

        return NewGraph().apply {
            addEdge(node1, node2)
            addEdge(node1, node3)
            addEdge(node1, node4)
            addEdge(node1, node5)

            addEdge(node2, node6)

            addEdge(node3, node7)

            addEdge(node4, node9)
            addEdge(node4, node0)

            addEdge(node5, node8)
        }
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

    enum class Direction {
        LEFT, RIGHT, TOP, BOTTOM
    }
}
