package com.example.sophieleaver.dumbotapp


import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.sophieleaver.dumbotapp.javafiles.NewGraph
import com.example.sophieleaver.dumbotapp.javafiles.NewNode
import com.example.sophieleaver.dumbotapp.javafiles.PerpendicularLinesAlgorithm
import de.blox.graphview.*
import kotlinx.android.synthetic.main.fragment_map.view.*


/**
 * A simple [Fragment] subclass.
 * Use the [MapFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MapFragment : Fragment() {
    private var currentNode: Node? = null
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
        graphView = view.graph
        setupAdapter(createGraph())
    }

    private fun setupAdapter(graph: Graph) {

        adapter = object : BaseGraphAdapter<ViewHolder>(graph) {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_map_node, parent, false)
                return SimpleViewHolder(view)
            }

            override fun onBindViewHolder(viewHolder: ViewHolder, data: Any, position: Int) {
                (viewHolder as SimpleViewHolder).textView.text = data.toString()
            }

            inner class SimpleViewHolder(itemView: View) : ViewHolder(itemView) {
                var textView: TextView = itemView.findViewById(R.id.textView)
            }
        }

        adapter.algorithm =
            PerpendicularLinesAlgorithm()
        graphView.adapter = adapter
        graphView.setOnItemClickListener { _, _, position, _ ->
            with(adapter.getNode(position)) {
                currentNode = this
                Snackbar.make(graphView, "Current Node is $data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createGraph(): NewGraph {

        val node1 = NewNode("Node 1")
        val node2 = NewNode("Node 2")
        val node3 = NewNode("Node 3")
        val node4 = NewNode("Node 4")
        val node5 = NewNode("Node 5")
        val node6 = NewNode("Node 6")
        val node7 = NewNode("Node 7")
        val node8 = NewNode("Node 8")
        val node9 = NewNode("Node 9")
        val node0 = NewNode("Node 0")

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
}
