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
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.otaliastudios.zoom.Alignment
import de.blox.graphview.*
import kotlinx.android.synthetic.main.dialog_delete_graph.*
import kotlinx.android.synthetic.main.fragment_map.view.*
import org.jetbrains.anko.toast
import kotlin.random.Random

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

    private lateinit var fabMenuAddNode: FloatingActionMenu
    private lateinit var fabMenuDeleteNode: FloatingActionMenu
    private lateinit var graphView: GraphView
    private lateinit var mapFrame: View
    private lateinit var adapter: BaseGraphAdapter<ViewHolder>

    private var nodes: List<PerpendicularChildrenNode> = mutableListOf()
    private var edges: List<Edge> = mutableListOf()

    private var currentNode: PerpendicularChildrenNode? = null

//    todo - fix menu open/close

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
        mapFrame = view.map_frame
        graphView = view.graph
        fabMenuAddNode = view.fabAddNode
        fabMenuDeleteNode = view.fabDeleteMenu
        loadGraph()
    }

    private fun setupFAB() {
        with(fabMenuAddNode) {
            fabAddNodeLeft.setOnClickListener { showNodeTypeSelectionDialog(Direction.LEFT) }
            fabAddNodeRight.setOnClickListener { showNodeTypeSelectionDialog(Direction.RIGHT) }
            fabAddNodeTop.setOnClickListener { showNodeTypeSelectionDialog(Direction.TOP) }
            fabAddNodeBottom.setOnClickListener { showNodeTypeSelectionDialog(Direction.BOTTOM) }
        }

        with(fabMenuDeleteNode) {
            fabDeleteNode.setOnClickListener {
                when {
                    currentNode == null -> Snackbar.make(
                        mapFrame,
                        "No node selected",
                        Snackbar.LENGTH_SHORT
                    )
                    currentNode!!.connectedNodes.size > 1 -> Snackbar.make(
                        mapFrame,
                        "Can only delete leaf nodes",
                        Snackbar.LENGTH_SHORT
                    )
                    else -> showRemoveNodeDialog()
                }
            }
            fabDeleteGraph.setOnClickListener { showGraphDeletionDialog() }
        }
    }

    private fun showGraphDeletionDialog() {
        val dialog = AlertDialog.Builder(requireContext()).run {
            setTitle("Delete Entire Graph")
            setMessage(getText(R.string.message_delete_graph))
            setView(R.layout.dialog_delete_graph)
            setPositiveButton("Delete Node") { _, _ -> deleteNode() }
            setNeutralButton("Cancel") { dialog, _ -> dialog.cancel() }
            create()
        }

        with(dialog) {
            setOnShowListener {
                (it as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                it.btn_reauthenticate.setOnClickListener { reauthenticate(this@with) }
            }

            show()
        }

    }

    private fun reauthenticate(dialog: AlertDialog) {
        with(dialog) {
            val email = layout_email_reauth.editText!!.text.toString()
            val password = layout_email_reauth.editText!!.text.toString()
            if (!email.isBlank() && !password.isBlank()) {
                val credential = EmailAuthProvider.getCredential(email, password)
                FirebaseAuth.getInstance().currentUser!!.reauthenticate(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            this@MapFragment.requireActivity().toast("Successful Authentication")
                        } else {
                            Log.w(fragTag, "Reauthentication Failed", task.exception)
                            dismiss()
                            this@MapFragment.requireActivity().toast("Reauthentication Failed")
                        }
                    }
            }
        }
    }

    private fun showRemoveNodeDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Remove Selected Node")
            setMessage("Are you sure you want to delete ${currentNode!!.data}?")
            setPositiveButton("Delete Node") { _, _ -> deleteNode() }
            setNeutralButton("Cancel") { dialog, _ -> dialog.cancel() }
            create()
            show()
        }
    }

    private fun deleteNode() {
        with(graphView.adapter.graph) {
            removeNode(nodes.find { it == currentNode }!!)

            when (currentNode!!.connectedNodes[0]) {
                currentNode!!.leftNode -> (nodes.find { currentNode!!.leftNode == it }!! as PerpendicularChildrenNode).rightNode =
                    null
                currentNode!!.rightNode -> (nodes.find { currentNode!!.rightNode == it }!! as PerpendicularChildrenNode).leftNode =
                    null
                currentNode!!.topNode -> (nodes.find { currentNode!!.topNode == it }!! as PerpendicularChildrenNode).bottomNode =
                    null
                currentNode!!.bottomNode -> (nodes.find { currentNode!!.bottomNode == it }!! as PerpendicularChildrenNode).topNode =
                    null
            }

            this@MapFragment.nodes = nodes as List<PerpendicularChildrenNode>
            this@MapFragment.currentNode = null
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
                fabMenuAddNode.run { if (isOpened) close(true) }
                updateFAB()
                Toast.makeText(requireContext(), "Selected ${it.data}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFAB() {
        currentNode?.run {
            fabMenuAddNode.let {
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

                dataSnapshot.children.find { it.key == "nodes" }?.let {
                    createNodes(it.getValue(String::class.java))
                }
                dataSnapshot.children.find { it.key == "angles" }?.let {
                    setupAngles(it.getValue(String::class.java))
                }
                dataSnapshot.children.find { it.key == "edges" }?.let {
                    createEdges(it.getValue(String::class.java))
                }

                val graph = Graph()
                edges.forEach { graph.addEdge(it.source, it.destination) }

                setupAdapter(graph)
                setupFAB()

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
                    edge.removePrefix("(").removeSuffix(")").split(",")
                        .run { buildEdge(this[0], this[1]) }
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
                            .split(",(")
                            .map { test ->
                                test.replace("):", ",").split(",")
                                    .run { Pair(Pair(this[0], this[1]), this[2]) }
                            }
                            .run { mapOf(*this.toTypedArray()) }
                    }

            val edgeCount: MutableMap<PerpendicularChildrenNode, Int> =
                nodes.associateWith {
                    angles.keys.count { edge ->
                        edge.toList().contains(it.data)
                    }
                }.toMutableMap()

            val frontier: MutableList<PerpendicularChildrenNode> = mutableListOf(nodes.first())
            var plane1: MutableList<String>
            var plane2: MutableList<String>


            while (frontier.isNotEmpty()) {
                val currentNode = frontier.removeAt(0).run { nodes.find { it == this } }!!
                val nodeData = currentNode.data

                val edgeDesciptions = angles.asIterable()
                    .first { entry -> (entry.key.first == nodeData) or (entry.key.second == nodeData) }
                    .value
                    .filterKeys { edgePair -> (edgePair.first == nodeData) or (edgePair.second == nodeData) }

                plane1 =
                    edgeDesciptions.filterValues { direction -> "BbFf".contains(direction, true) }
                        .keys
                        .map { edgePair -> edgePair.toList().find { it != nodeData }!! }
                        .toMutableList()

                plane2 =
                    edgeDesciptions.filterValues { direction -> "AaCc".contains(direction, true) }
                        .keys
                        .map { edgePair -> edgePair.toList().find { it != nodeData }!! }
                        .toMutableList()

                if (currentNode.connectedNodes.isEmpty()) {
                    val firstPlaneToSet = choosePlane(plane1, plane2)

                    currentNode.leftNode = nodes.find { it.data == firstPlaneToSet.first[0] }
                    nodes.find { currentNode.leftNode == it }!!.rightNode = currentNode
                    edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                    edgeCount[nodes.find { currentNode.leftNode == it }!!] =
                        edgeCount.getValue(nodes.find { currentNode.leftNode == it }!!) - 1
                    if (edgeCount[currentNode.leftNode]!! > 0) frontier.add(currentNode.leftNode as PerpendicularChildrenNode)

                    if (firstPlaneToSet.first.size == 2) {
                        currentNode.rightNode =
                            nodes.find { it.data == firstPlaneToSet.first[1] }
                        nodes.find { currentNode.rightNode == it }!!.leftNode = currentNode
                        edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                        edgeCount[nodes.find { currentNode.rightNode == it }!!] =
                            edgeCount.getValue(nodes.find { currentNode.rightNode == it }!!) - 1
                        if (edgeCount[currentNode.rightNode]!! > 0) frontier.add(currentNode.rightNode as PerpendicularChildrenNode)
                    }

                    if (firstPlaneToSet.second.isNotEmpty()) {
                        val edgeDict =
                            (angles[Pair(nodeData, currentNode.leftNode!!.data)] ?: angles[Pair(
                                currentNode.leftNode!!.data,
                                nodeData
                            )])!!
                                .filterValues { direction -> "AaCc".contains(direction, true) }
                                .asIterable()

                        edgeDict.find { it.value == "A" }?.also { entry ->
                            currentNode.topNode =
                                nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                            nodes.find { it == currentNode.topNode }!!.bottomNode = currentNode
                            edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                            edgeCount[nodes.find { currentNode.topNode == it }!!] =
                                edgeCount.getValue(nodes.find { currentNode.topNode == it }!!) - 1
                            if (edgeCount[currentNode.topNode!!]!! > 0) frontier.add(currentNode.topNode as PerpendicularChildrenNode)
                        }

                        edgeDict.find { it.value == "C" }?.also { entry ->
                            currentNode.bottomNode =
                                nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                            nodes.find { it == currentNode.bottomNode }!!.topNode = currentNode
                            edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                            edgeCount[nodes.find { currentNode.bottomNode == it }!!] =
                                edgeCount.getValue(nodes.find { currentNode.bottomNode == it }!!) - 1
                            if (edgeCount[currentNode.bottomNode!!]!! > 0) frontier.add(currentNode.bottomNode as PerpendicularChildrenNode)
                        }
                    }

                } else {
                    when {
                        currentNode.leftNode != null -> {
                            val edgeDict =
                                (angles[Pair(nodeData, currentNode.leftNode!!.data)] ?: angles[Pair(
                                    currentNode.leftNode!!.data,
                                    nodeData
                                )])!!
                                    .filter { it.key.toList().contains(nodeData) }
                                    .asIterable()

                            edgeDict.find { it.value == "A" }?.also { entry ->
                                currentNode.topNode =
                                    nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                                nodes.find { it == currentNode.topNode }!!.bottomNode = currentNode
                                edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                                edgeCount[nodes.find { currentNode.topNode == it }!!] =
                                    edgeCount.getValue(nodes.find { currentNode.topNode == it }!!) - 1
                                if (edgeCount[currentNode.topNode!!]!! > 0) frontier.add(currentNode.topNode as PerpendicularChildrenNode)
                            }

                            edgeDict.find { it.value == "C" }?.also { entry ->
                                currentNode.bottomNode =
                                    nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                                nodes.find { it == currentNode.bottomNode }!!.topNode = currentNode
                                edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                                edgeCount[nodes.find { currentNode.bottomNode == it }!!] =
                                    edgeCount.getValue(nodes.find { currentNode.bottomNode == it }!!) - 1
                                if (edgeCount[currentNode.bottomNode!!]!! > 0) frontier.add(
                                    currentNode.bottomNode as PerpendicularChildrenNode
                                )
                            }

                            edgeDict.find { it.value == "F" }?.also { entry ->
                                currentNode.rightNode =
                                    nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                                nodes.find { it == currentNode.rightNode }!!.leftNode = currentNode
                                edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                                edgeCount[nodes.find { currentNode.rightNode == it }!!] =
                                    edgeCount.getValue(nodes.find { currentNode.rightNode == it }!!) - 1
                                if (edgeCount[currentNode.rightNode!!]!! > 0) frontier.add(
                                    currentNode.rightNode as PerpendicularChildrenNode
                                )
                            }

                        }
                        currentNode.rightNode != null -> {
                            val edgeDict =
                                (angles[Pair(nodeData, currentNode.rightNode!!.data)]
                                    ?: angles[Pair(currentNode.rightNode!!.data, nodeData)])!!
                                    .filter { it.key.toList().contains(nodeData) }
                                    .asIterable()

                            edgeDict.find { it.value == "C" }?.also { entry ->
                                currentNode.topNode =
                                    nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                                nodes.find { it == currentNode.topNode }!!.bottomNode = currentNode
                                edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                                edgeCount[nodes.find { currentNode.topNode == it }!!] =
                                    edgeCount.getValue(nodes.find { currentNode.topNode == it }!!) - 1
                                if (edgeCount[currentNode.topNode!!]!! > 0) frontier.add(currentNode.topNode as PerpendicularChildrenNode)
                            }

                            edgeDict.find { it.value == "A" }?.also { entry ->
                                currentNode.bottomNode =
                                    nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                                nodes.find { it == currentNode.bottomNode }!!.topNode = currentNode
                                edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                                edgeCount[nodes.find { currentNode.bottomNode == it }!!] =
                                    edgeCount.getValue(nodes.find { currentNode.bottomNode == it }!!) - 1
                                if (edgeCount[currentNode.bottomNode!!]!! > 0) frontier.add(
                                    currentNode.bottomNode as PerpendicularChildrenNode
                                )
                            }

                            edgeDict.find { it.value == "F" }?.also { entry ->
                                currentNode.leftNode =
                                    nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                                nodes.find { it == currentNode.leftNode }!!.rightNode = currentNode
                                edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                                edgeCount[nodes.find { currentNode.leftNode == it }!!] =
                                    edgeCount.getValue(nodes.find { currentNode.leftNode == it }!!) - 1
                                if (edgeCount[currentNode.leftNode!!]!! > 0) frontier.add(
                                    currentNode.leftNode as PerpendicularChildrenNode
                                )
                            }
                        }
                        currentNode.topNode != null -> {
                            val edgeDict =
                                (angles[Pair(nodeData, currentNode.topNode!!.data)] ?: angles[Pair(
                                    currentNode.topNode!!.data,
                                    nodeData
                                )])!!
                                    .filter { it.key.toList().contains(nodeData) }
                                    .asIterable()

                            edgeDict.find { it.value == "C" }?.also { entry ->
                                currentNode.leftNode =
                                    nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                                nodes.find { it == currentNode.leftNode }!!.rightNode = currentNode
                                edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                                edgeCount[nodes.find { currentNode.leftNode == it }!!] =
                                    edgeCount.getValue(nodes.find { currentNode.leftNode == it }!!) - 1
                                if (edgeCount[currentNode.leftNode!!]!! > 0) frontier.add(
                                    currentNode.leftNode as PerpendicularChildrenNode
                                )
                            }

                            edgeDict.find { it.value == "A" }?.also { entry ->
                                currentNode.rightNode =
                                    nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                                nodes.find { it == currentNode.rightNode }!!.leftNode = currentNode
                                edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                                edgeCount[nodes.find { currentNode.rightNode == it }!!] =
                                    edgeCount.getValue(nodes.find { currentNode.rightNode == it }!!) - 1
                                if (edgeCount[currentNode.rightNode!!]!! > 0) frontier.add(
                                    currentNode.rightNode as PerpendicularChildrenNode
                                )
                            }

                            edgeDict.find { it.value == "F" }?.also { entry ->
                                currentNode.bottomNode =
                                    nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                                nodes.find { it == currentNode.bottomNode }!!.topNode = currentNode
                                edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                                edgeCount[nodes.find { currentNode.bottomNode == it }!!] =
                                    edgeCount.getValue(nodes.find { currentNode.bottomNode == it }!!) - 1
                                if (edgeCount[currentNode.bottomNode!!]!! > 0) frontier.add(
                                    currentNode.bottomNode as PerpendicularChildrenNode
                                )
                            }
                        }
                        currentNode.bottomNode != null -> {
                            val edgeDict =
                                (angles[Pair(nodeData, currentNode.bottomNode!!.data)]
                                    ?: angles[Pair(currentNode.bottomNode!!.data, nodeData)])!!
                                    .filter { it.key.toList().contains(nodeData) }
                                    .asIterable()

                            edgeDict.find { it.value == "A" }?.also { entry ->
                                currentNode.leftNode =
                                    nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                                nodes.find { it == currentNode.leftNode }!!.rightNode = currentNode
                                edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                                edgeCount[nodes.find { currentNode.leftNode == it }!!] =
                                    edgeCount.getValue(nodes.find { currentNode.leftNode == it }!!) - 1
                                if (edgeCount[currentNode.leftNode!!]!! > 0) frontier.add(
                                    currentNode.leftNode as PerpendicularChildrenNode
                                )
                            }

                            edgeDict.find { it.value == "C" }?.also { entry ->
                                currentNode.rightNode =
                                    nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                                nodes.find { it == currentNode.rightNode }!!.leftNode = currentNode
                                edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                                edgeCount[nodes.find { currentNode.rightNode == it }!!] =
                                    edgeCount.getValue(nodes.find { currentNode.rightNode == it }!!) - 1
                                if (edgeCount[currentNode.rightNode!!]!! > 0) frontier.add(
                                    currentNode.rightNode as PerpendicularChildrenNode
                                )
                            }

                            edgeDict.find { it.value == "F" }?.also { entry ->
                                currentNode.topNode =
                                    nodes.find { node -> node.data == entry.key.toList().find { edgeId -> edgeId != nodeData } }!!
                                nodes.find { it == currentNode.topNode }!!.bottomNode = currentNode
                                edgeCount[currentNode] = edgeCount.getValue(currentNode) - 1
                                edgeCount[nodes.find { currentNode.topNode == it }!!] =
                                    edgeCount.getValue(nodes.find { currentNode.topNode == it }!!) - 1
                                if (edgeCount[currentNode.topNode!!]!! > 0) frontier.add(
                                    currentNode.topNode as PerpendicularChildrenNode
                                )
                            }
                        }
                    }
                }


            }


        }


    }

    private fun choosePlane(plane1: MutableList<String>, plane2: MutableList<String>):
            Pair<MutableList<String>, MutableList<String>> = when {
        plane1.size > plane2.size -> Pair(plane1, plane2)
        plane2.size > plane1.size -> Pair(plane2, plane1)
        else -> if (Random.nextBoolean()) Pair(plane1, plane2) else Pair(plane2, plane1)
    }

    private fun buildEdge(sourceId: String, destId: String): Edge =
        with(nodes) { Edge(find { it.data == sourceId }, find { it.data == destId }) }

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
        val angleString = edges.joinToString { edge ->
            val key = "('${edge.source.data}','${edge.destination.data}'):"
            val valueDict = edge.run {
                val sourceEdges =
                    with((source as PerpendicularChildrenNode)) {
                        val backwardsEdge = "('$data','${edge.destination.data}') : 'B'"
                        val otherEdges: Triple<String?, String?, String?> =
                            when (edge.destination) {
                                leftNode -> {
                                    val node1 =
                                        rightNode?.let { "('$data','${rightNode!!.data}'): 'F'" }
                                    val node2 =
                                        topNode?.let { "('$data','${topNode!!.data}'): 'A'" }
                                    val node3 =
                                        bottomNode?.let { "('$data','${bottomNode!!.data}'): 'C'" }
                                    Triple(node1, node2, node3)
                                }

                                rightNode -> {
                                    val node1 =
                                        leftNode?.let { "('$data','${leftNode!!.data}'): 'F'" }
                                    val node2 =
                                        topNode?.let { "('$data','${topNode!!.data}'): 'C'" }
                                    val node3 =
                                        bottomNode?.let { "('$data','${bottomNode!!.data}'): 'A'" }
                                    Triple(node1, node2, node3)
                                }

                                topNode -> {
                                    val node1 =
                                        rightNode?.let { "('$data','${rightNode!!.data}'): 'A'" }
                                    val node2 =
                                        leftNode?.let { "('$data','${leftNode!!.data}'): 'C'" }
                                    val node3 =
                                        bottomNode?.let { "('$data','${bottomNode!!.data}'): 'F'" }
                                    Triple(node1, node2, node3)
                                }
                                else -> {
                                    val node1 =
                                        rightNode?.let { "('$data','${rightNode!!.data}'): 'C'" }
                                    val node2 =
                                        topNode?.let { "('$data','${topNode!!.data}'): 'F'" }
                                    val node3 =
                                        leftNode?.let { "('$data','${leftNode!!.data}'): 'A'" }
                                    Triple(node1, node2, node3)
                                }

                            }
                        (listOf(backwardsEdge) + otherEdges.toList().toMutableList().filterNotNull()).joinToString()
                    }
                val destEdges =
                    with((destination as PerpendicularChildrenNode)) {
                        val otherEdges: Triple<String?, String?, String?> =
                            when (edge.source) {
                                leftNode -> {
                                    val node1 =
                                        rightNode?.let { "('$data','${rightNode!!.data}'): 'F'" }
                                    val node2 =
                                        topNode?.let { "('$data','${topNode!!.data}'): 'A'" }
                                    val node3 =
                                        bottomNode?.let { "('$data','${bottomNode!!.data}'): 'C'" }
                                    Triple(node1, node2, node3)
                                }

                                rightNode -> {
                                    val node1 =
                                        leftNode?.let { "('$data','${leftNode!!.data}'): 'F'" }
                                    val node2 =
                                        topNode?.let { "('$data','${topNode!!.data}'): 'C'" }
                                    val node3 =
                                        bottomNode?.let { "('$data','${bottomNode!!.data}'): 'A'" }
                                    Triple(node1, node2, node3)
                                }

                                topNode -> {
                                    val node1 =
                                        rightNode?.let { "('$data','${rightNode!!.data}'): 'A'" }
                                    val node2 =
                                        leftNode?.let { "('$data','${leftNode!!.data}'): 'C'" }
                                    val node3 =
                                        bottomNode?.let { "('$data','${bottomNode!!.data}'): 'F'" }
                                    Triple(node1, node2, node3)
                                }
                                else -> {
                                    val node1 =
                                        rightNode?.let { "('$data','${rightNode!!.data}'): 'C'" }
                                    val node2 =
                                        topNode?.let { "('$data','${topNode!!.data}'): 'F'" }
                                    val node3 =
                                        leftNode?.let { "('$data','${leftNode!!.data}'): 'A'" }
                                    Triple(node1, node2, node3)
                                }

                            }
                        otherEdges.toList().toMutableList().filterNotNull().joinToString()
                    }
                "{ ${sourceEdges.removePrefix(",")} ${if (destEdges.isNotBlank()) "," else ""} ${destEdges.removePrefix(
                    ","
                )} }"
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

    enum class Direction {
        LEFT, RIGHT, TOP, BOTTOM
    }
}
