package com.example.sophieleaver.dumbotapp

import android.icu.text.DecimalFormat
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_weight_inventory.view.*
import kotlinx.android.synthetic.main.item_inventory_dumbbell.view.*
import org.jetbrains.anko.toast


data class Dumbbell(
    var weightValue: Double = 0.0,
    var totalStock: Int = 0,
    val activeRequests: MutableMap<String, String> = mutableMapOf(),
    val waitQueue: MutableMap<String, String> = mutableMapOf(),
    val storageLocation: MutableList<String> = mutableListOf()
)

class WeightsFragment : Fragment() {
    private val database = FirebaseDatabase.getInstance().reference
    private val weightReference = database.child("demo2").child("weights")
    private val decimalFormat = DecimalFormat("##.##")

    private var dumbbellList: List<Dumbbell> = emptyList()
    private lateinit var dumbbellRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_weight_inventory, container, false)

        dumbbellRecyclerView = view.dumbbell_list.apply {
            layoutManager = LinearLayoutManager(this@WeightsFragment.requireContext())
            adapter = DumbbellAdapter()
        }
        view.fab_edit_dumbbells.setOnClickListener { NewWeightDialog.display(fragmentManager!!) }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun getWeightInventory() {

        weightReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val newWeightInventory: MutableList<Dumbbell> = mutableListOf()
                for (dumbbellSnapshot in dataSnapshot.children) {
//                    Log.d("WeightsFragment", "${dumbbell.key} => ${dumbbell.value.toString()}")
                    val dumbbell = dumbbellSnapshot.getValue(Dumbbell::class.java)
                    if (dumbbell != null) newWeightInventory += dumbbell
                }
                dumbbellList = newWeightInventory.sortedBy { it.weightValue }
                dumbbellRecyclerView.adapter!!.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("WeightsFragment", "loadDumbbells:onCancelled", databaseError.toException())
                requireActivity().toast("Failed getting weights, please try again")
            }

        })

    }

    private fun setupRecyclerView() {
        dumbbellRecyclerView.layoutManager = LinearLayoutManager(this.requireContext())
        dumbbellRecyclerView.adapter = DumbbellAdapter()
        getWeightInventory()
    }


    companion object {
        @JvmStatic
        fun newInstance() = WeightsFragment()
    }

    inner class DumbbellAdapter : RecyclerView.Adapter<DumbbellAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(this@WeightsFragment.requireContext())
                    .inflate(R.layout.item_inventory_dumbbell, parent, false)
            )

        override fun getItemCount(): Int = dumbbellList.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val dumbbell: Dumbbell? = dumbbellList[position]
            dumbbell?.let {
                holder.weightValue.text =
                    getString(R.string.weight, decimalFormat.format(it.weightValue))
                holder.currentStock.text =
                    getString(R.string.current_stock, it.totalStock - it.activeRequests.size)
                holder.totalStock.text = getString(R.string.total_stock, it.totalStock)
                holder.storageLocation.text =
                    getString(R.string.storage_location, it.storageLocation.joinToString())
                holder.editDumbbellButton.setOnClickListener {
                    NewWeightDialog.display(fragmentManager!!, dumbbell)
                }
            }
        }


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val weightValue: TextView = view.text_total_stock
            val currentStock: TextView = view.text_current_stock
            val totalStock: TextView = view.text_weight_value
            val storageLocation: TextView = view.text_storage_location
            val editDumbbellButton: ImageView = view.img_remove_weight
        }
    }
}