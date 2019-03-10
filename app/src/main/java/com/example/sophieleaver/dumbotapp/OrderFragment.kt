package com.example.sophieleaver.dumbotapp


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_order_weights.view.*
import kotlinx.android.synthetic.main.item_order_dumbbell.view.*
import org.jetbrains.anko.toast
import java.util.*


data class DumbbellRequest(val weight: String, val type: String, val bench: String, val time: Long)


class OrderFragment : Fragment() {

    private val ref = FirebaseDatabase.getInstance().reference

    private val requestReference = ref.child("demo2").child("requests")
    private val weightReference = ref.child("demo2").child("weights")

    private var weights: MutableList<Dumbbell> =
        mutableListOf() //= listOf("12kg", "14kg", "16kg", "18kg", "20kg", "22kg")

    private var orderDumbbellRecyclerView: RecyclerView? = null

    private val fragTag = "OrderFragment"


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_order_weights, container, false)

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(view) {
            orderDumbbellRecyclerView = order_dumbbell_list
            findViewById<TextView>(R.id.textview_current_bench).text = currentBench.toString()

            //when button pressed, alert dialog opened to change the bench
            btn_change_station.setOnClickListener {
                val builder = AlertDialog.Builder(context)
                val alertView = layoutInflater.inflate(R.layout.change_bench_layout, null)
                builder.setView(alertView)
                builder.setTitle("Change Workout Station")
                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                val dialog: AlertDialog = builder.create()
                dialog.show()

                //on click listeners for when a new bench is selected
                val b1: Button = alertView.findViewById(R.id.button_bench_1)
                b1.setOnClickListener { changeBench(1, dialog) }
                val b2: Button = alertView.findViewById(R.id.button_bench_2)
                b2.setOnClickListener { changeBench(2, dialog) }
                val b3: Button = alertView.findViewById(R.id.button_bench_3)
                b3.setOnClickListener { changeBench(3, dialog) }
                val b4: Button = alertView.findViewById(R.id.button_bench_4)
                b4.setOnClickListener { changeBench(4, dialog) }
                val b5: Button = alertView.findViewById(R.id.button_bench_5)
                b5.setOnClickListener { changeBench(5, dialog) }
                val b6: Button = alertView.findViewById(R.id.button_bench_6)
                b6.setOnClickListener { changeBench(6, dialog) }
            }
        }
        setupRecyclerView()
    }


    private fun changeBench(bench: Int, dialog: AlertDialog) {
        currentBench = bench
        val text: TextView = view!!.findViewById(R.id.textview_current_bench)
        text.text = currentBench.toString()
        dialog.cancel()
        Log.d(fragTag, "currentBench = $currentBench, set bench is $bench")
    }

    private fun getWeightData() {

        weightReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val newDumbbells: MutableList<Dumbbell> = mutableListOf()
                for (dumbbellSnapshot in dataSnapshot.children) {
//                    Log.d("WeightsFragment", "${dumbbell.key} => ${dumbbell.value.toString()}")
                    val dumbbell = dumbbellSnapshot.getValue(Dumbbell::class.java)
                    if (dumbbell != null) newDumbbells += dumbbell
                }
                weights = newDumbbells
                orderDumbbellRecyclerView!!.adapter!!.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("WeightFragment", "loadDumbbells:onCancelled", databaseError.toException())
                requireActivity().toast("Failed getting weights, please try again")
            }

        })
    }

    private fun setupRecyclerView() {
        orderDumbbellRecyclerView!!.layoutManager = LinearLayoutManager(requireContext())
        orderDumbbellRecyclerView!!.adapter = DumbbellRequestAdapter()
        getWeightData()
    }


    companion object {
        @JvmStatic
        fun newInstance() = OrderFragment()
    }

    inner class DumbbellRequestAdapter : RecyclerView.Adapter<DumbbellRequestAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(this@OrderFragment.requireContext())
                    .inflate(R.layout.item_order_dumbbell, parent, false)
            )

        override fun getItemCount(): Int = weights.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val requestedWeight = weights[position]

            holder.apply {
                val currentStock = requestedWeight.totalStock - requestedWeight.activeRequests.size
                val dumbbellAvailable = currentStock > 0

                val availableTextResId =
                    if (dumbbellAvailable) R.string.available else R.string.unavailable
                val orderButtonTextResId =
                    if (dumbbellAvailable) R.string.order else R.string.join_wait_queue
                val orderButtonColorResId =
                    if (dumbbellAvailable) R.color.colorDumbbellAvailable else R.color.colorDumbbellUnavailable
                val dumbbellDetails =
                    if (dumbbellAvailable) Triple(
                        R.string.available_dumbbells_info,
                        currentStock,
                        requestedWeight.totalStock
                    )
                    else Triple(
                        R.string.wait_queue_info,
                        requestedWeight.totalStock,
                        requestedWeight.waitQueue.size
                    )


                weightValue.text = getString(R.string.weight, requestedWeight.weightValue)
                available.text = getString(availableTextResId)
                orderButton.text = getString(orderButtonTextResId)
                availabilityInfo.text = getString(
                    dumbbellDetails.first,
                    dumbbellDetails.second,
                    dumbbellDetails.third
                )
                orderButton.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        orderButtonColorResId
                    )
                )
                orderButton.setOnClickListener {
                    createRequest(dumbbellAvailable, requestedWeight.weightValue.toString())
//                    toastz
                    if (dumbbellAvailable) {
                        (activity as MainActivity).openFragment(CurrentSessionFragment.newInstance())
                    }
                }


            }
        }

        private fun createRequest(dumbbellAvailable: Boolean, weightValue: String) {

            val requestID = requestReference.push().key
            if (requestID != null) {

                val status = if (dumbbellAvailable) "delivering" else "waiting"
                val path = if (dumbbellAvailable) "activeRequests" else "waitQueue"
                val benchID = benchNumberToFirebaseID(currentBench)
                val time = Date().time

                requestReference.child(requestID)
                    .setValue(DumbbellRequest(weightValue, status, benchID, time))
                weightReference.child("$weightValue/$path/$benchID").setValue("$status|$time")

                Log.d(
                    fragTag,
                    "Sending request $requestID to server (deliver dumbbells of $weightValue kg to bench $currentBench)"
                )

            } else {
                requireActivity().toast("Problem sending dumbbell request. Please try again")
            }

        }


        private fun benchNumberToFirebaseID(bench: Int): String =
            when (bench) {
                1 -> "B7"
                2 -> "B10"
                3 -> "B13"
                4 -> "B9"
                5 -> "B12"
                else -> "B15"
            }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val weightValue: TextView = view.text_weight_value
            val available: TextView = view.text_available
            val availabilityInfo: TextView = view.text_wait_queue
            val orderButton: Button = view.btn_order_dumbbell
        }
    }
}
