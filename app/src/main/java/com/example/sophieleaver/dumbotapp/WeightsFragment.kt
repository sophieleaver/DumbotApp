package com.example.sophieleaver.dumbotapp

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_weights.view.*
import kotlinx.android.synthetic.main.item_dumbbell.view.*
import org.jetbrains.anko.toast


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

data class Dumbbell(val weightValue: Int?, val totalStock: Int?, val currentStock: Int?, val storageLocation: List<String>?)

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [WeightsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [WeightsFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class WeightsFragment : Fragment() {

    private val firebaseFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val dumbbellReference: CollectionReference = firebaseFirestore.collection("dumbbells")
    private val dumbbellList: MutableList<Dumbbell> = mutableListOf()
    private lateinit var dumbbellRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_weights, container, false).apply {
            dumbbell_list.let {
                it.layoutManager = LinearLayoutManager(this@WeightsFragment.requireContext())
                it.adapter = DumbbellAdapter()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(view) {
            fab_edit_dumbbells.setOnClickListener { this@WeightsFragment.requireActivity().toast("add new dumbbell") }
            dumbbellRecyclerView = dumbbell_list
        }

        getDumbbellData()

    }

    private fun getDumbbellData() {
        dumbbellReference.get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        Log.d("WeightsFragment", document.id + " => " + document.data)
                        dumbbellList.add(document.toObject(Dumbbell::class.java))
                    }
                    dumbbellList.sortBy { it.weightValue }
                    setupRecyclerView()
                }
    }

    private fun setupRecyclerView() {
        dumbbellRecyclerView.layoutManager = LinearLayoutManager(this.requireContext())
        dumbbellRecyclerView.adapter = DumbbellAdapter()
    }


    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                WeightsFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }

    inner class DumbbellAdapter : RecyclerView.Adapter<DumbbellAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(LayoutInflater.from(this@WeightsFragment.requireContext())
                    .inflate(R.layout.item_dumbbell, parent, false))

        override fun getItemCount(): Int = dumbbellList.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val dumbbell: Dumbbell = dumbbellList[position]
            holder.apply {
                weightValue.text = getString(R.string.weight, dumbbell.weightValue)
                currentStock.text = getString(R.string.current_stock, dumbbell.currentStock)
                totalStock.text = getString(R.string.total_stock, dumbbell.totalStock)
                storageLocation.text =
                    getString(R.string.storage_location, dumbbell.storageLocation?.joinToString())
                editDumbbellButton.setOnClickListener { this@WeightsFragment.requireActivity().toast("edit dumbbell") }
            }
        }


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val weightValue: TextView = view.text_weight_value
            val currentStock: TextView = view.text_current_stock
            val totalStock: TextView = view.text_total_stock
            val storageLocation: TextView = view.text_storage_location
            val editDumbbellButton: ImageView = view.img_edit_dumbbell
        }
    }
}
