package com.example.sophieleaver.dumbotapp

import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
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
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_weight_inventory.view.*
import kotlinx.android.synthetic.main.item_inventory_dumbbell.view.*
import org.jetbrains.anko.toast


@Parcelize
data class Dumbbell(
    val weightValue: Int? = 0,
    val totalStock: Int? = 0,
    val currentStock: Int? = 0,
    val queueLength: Int? = 0,
    val storageLocation: List<String>? = emptyList()
) : Parcelable


class WeightsFragment : Fragment() {
    private val database = FirebaseDatabase.getInstance().reference
    private val weightReference = database.child("demo2").child("weights")

    private var dumbbellList: MutableList<Dumbbell> = mutableListOf()
    private lateinit var dumbbellRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_weight_inventory, container, false).apply {
            dumbbellRecyclerView = dumbbell_list.apply {
                layoutManager = LinearLayoutManager(this@WeightsFragment.requireContext())
                adapter = DumbbellAdapter()
            }
            fab_edit_dumbbells.setOnClickListener { requireActivity().toast("add new dumbbell") }
        }
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
                dumbbellList = newWeightInventory
                dumbbellRecyclerView.adapter!!.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("WeightFragment", "loadDumbbells:onCancelled", databaseError.toException())
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
        fun newInstance() =
            WeightsFragment().apply {
                arguments = Bundle().apply { }
            }
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
                holder.weightValue.text = getString(R.string.weight, it.weightValue)
                holder.currentStock.text = getString(R.string.current_stock, it.currentStock)
                holder.totalStock.text = getString(R.string.total_stock, it.totalStock)
                holder.storageLocation.text =
                    getString(R.string.storage_location, it.storageLocation?.joinToString())
                holder.editDumbbellButton.setOnClickListener {
                    this@WeightsFragment.requireActivity().toast("edit dumbbell")
                }
            }
        }


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val weightValue: TextView = view.text_weight_value
            val currentStock: TextView = view.text_dumbot
            val totalStock: TextView = view.text_dumbot_information
            val storageLocation: TextView = view.text_storage_location
            val editDumbbellButton: ImageView = view.img_edit_dumbbell
        }
    }
}
