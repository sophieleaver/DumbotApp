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
import java.time.LocalDateTime
import java.time.ZoneOffset


class OrderFragment : Fragment() {

    private val fragTag = "OrderFragment"

    private val ref = FirebaseDatabase.getInstance().reference

    private val requestReference = ref.child("demo2").child("requests")
    private val weightReference = ref.child("demo2").child("weights")

    private var weights: MutableList<Dumbbell> = mutableListOf()
    private var orderDumbbellRecyclerView: RecyclerView? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_order_weights, container, false)

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(view) {
            orderDumbbellRecyclerView = order_dumbbell_list
            findViewById<TextView>(R.id.text_workout_station).text = getString(R.string.workout_station, currentBench)

            //when button pressed, alert dialog opened to change the bench
            button_see_workout.setOnClickListener {
                (activity as MainActivity).openFragment(CurrentOrdersFragment.newInstance())
            }
        }
        setupRecyclerView()
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
                Log.w(fragTag, "loadDumbbells:onCancelled", databaseError.toException())
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
                ViewHolder(LayoutInflater.from(this@OrderFragment.requireContext()).inflate(R.layout.item_order_dumbbell,
                        parent,
                        false))

        override fun getItemCount(): Int = weights.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            //todo implement for next demo
//            if (maximumWeightLimitReached()) {
//                holder.orderButton.isEnabled = false
//                holder.orderButton.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
//            } else {
                holder.orderButton.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorDumbbellAvailable))
//            }
            val requestedWeight = weights[position]

            holder.apply {
                val currentStock = requestedWeight.totalStock - requestedWeight.activeRequests.size
                val dumbbellAvailable = currentStock > 0

                val availableTextResId = if (dumbbellAvailable) R.string.available else R.string.unavailable
                val orderButtonTextResId = if (dumbbellAvailable) R.string.order else R.string.join_wait_queue
                val orderButtonColorResId =
                    if (dumbbellAvailable) R.color.colorDumbbellAvailable else R.color.colorDumbbellUnavailable
                val dumbbellDetails = if (dumbbellAvailable) Triple(R.string.available_dumbbells_info,
                        currentStock,
                        requestedWeight.totalStock)
                else Triple(R.string.wait_queue_info, requestedWeight.totalStock, requestedWeight.waitQueue.size)

                weightValue.text = getString(R.string.weight, requestedWeight.weightValue)
                available.text = getString(availableTextResId)
                orderButton.text = getString(orderButtonTextResId)
                availabilityInfo.text = getString(dumbbellDetails.first, dumbbellDetails.second, dumbbellDetails.third)
                orderButton.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), orderButtonColorResId))
                orderButton.setOnClickListener {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("Confirm Order")
                    builder.setMessage("Are you sure you would like to order the ${holder.weightValue.text} dumbbells?")
                    builder.setPositiveButton("CONFIRM") { _, _ ->
                        createRequest(dumbbellAvailable, requestedWeight.weightValue.toString())
                    }
                    builder.setNeutralButton("CANCEL") { dialog, _ -> dialog.cancel() }

                        val dialog = builder.create()
                        dialog.show()
                    }
//                if (dumbbellAvailable) {
//                                createRequest(
//                                    dumbbellAvailable,
//                                    requestedWeight.weightValue.toString()
//                                )
//                            } else {
//                                dialog.cancel()
//                                val innerBuilder = AlertDialog.Builder(context)
//                                innerBuilder.apply {
//                                    setTitle("Weight Unavailable")
//                                    setMessage("The weight you just ordered has now become unavailable - please choose another set of dumbbells from the list.")
//                                    setPositiveButton("OKAY") { dialog, _ ->
//                                        dialog.cancel()
//                                    }
//                                }
//                            }
//                        }
                }
            }



        private fun createRequest(dumbbellAvailable: Boolean, weightValue: String) {

            val now = LocalDateTime.now(ZoneOffset.UTC)
            val seconds = now.atZone(ZoneOffset.UTC).toEpochSecond() //request time is always in seconds
            val milliseconds = now.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() //request ID is in milli seconds

            val requestID = milliseconds.toString()
            val status = if (dumbbellAvailable) "delivering" else "waiting"
            val path = if (dumbbellAvailable) "activeRequests" else "waitQueue"
            val bench = currentBench

            val newRequest = Request(requestID, seconds, status, weightValue, bench)
            requests[requestID] = newRequest
            requestReference.child(requestID).setValue(newRequest)
            weightReference.child("$weightValue/$path/$bench")
                .setValue("$status|$seconds")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        (activity as MainActivity).onSuccessfulOrder(weightValue)
                    } else {
                        Log.w(fragTag, "Failed to send request $requestID", task.exception)
                        requireActivity().toast("There was an error sending your dumbbell request. Please try again later.")
                    }
                }

            Log.d(fragTag,
                    "Sending request $requestID to server (deliver dumbbells of $weightValue kg to bench $currentBench)")
        }

        private fun maximumWeightLimitReached(): Boolean = requests.values.count {
            (it.type == "delivering") or (it.type == "current")
        } >= 2

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val weightValue: TextView = view.text_total_stock
            val available: TextView = view.text_available
            val availabilityInfo: TextView = view.text_wait_queue
            val orderButton: Button = view.btn_order_dumbbell
        }
    }
}
