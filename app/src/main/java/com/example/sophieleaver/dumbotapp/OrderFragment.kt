package com.example.sophieleaver.dumbotapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.icu.text.DecimalFormat
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
    private val decimalFormat = DecimalFormat("##.##")

    private val requestReference = ref.child("demo2").child("requests")
    private val weightReference = ref.child("demo2").child("weights")

    private val waitQueueListeners: MutableMap<Double, ValueEventListener> = mutableMapOf()

    private var weights: MutableList<Dumbbell> = mutableListOf()
    private var orderDumbbellRecyclerView: RecyclerView? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_order_weights, container, false)

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(view) {
            orderDumbbellRecyclerView = order_dumbbell_list

            //when button pressed, alert dialog opened to change the bench
            button_see_workout.setOnClickListener {
                (activity as MainActivity).showCurrentOrdersFragment()
            }
        }

        setupRecyclerView()

        weightReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(fragTag, "Failed to get list of weights", databaseError.toException())
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children
                    .filter {
                        it.getValue(Dumbbell::class.java)!!.waitQueue.containsValue(
                            currentBench
                        )
                    }
                    .forEach {
                        val dumbbellWeightQueueListener = object : ValueEventListener {
                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.w(
                                    fragTag,
                                    "Failed to load weight data",
                                    databaseError.toException()
                                )
                            }

                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val weight = dataSnapshot.getValue(Dumbbell::class.java)!!
                                if ((weight.totalStock - weight.activeRequests.size) > 0) {
                                    val nextInQueue =
                                        weight.waitQueue.toSortedMap().asIterable().first()
                                    if (nextInQueue.value == currentBench) {
                                        weight.waitQueue.remove(nextInQueue.key, nextInQueue.value)

                                        if (!weight.waitQueue.containsValue(currentBench)) {
                                            waitQueueListeners.remove(weight.weightValue)
                                        }

                                        dataSnapshot.ref.updateChildren(mapOf("waitQueue" to weight.waitQueue)) { _, _ ->
                                            requireActivity().toast("One of your queued dumbbells is now being delivered")
                                            createRequest(true, weight.weightValue.toString())
                                        }
                                    }

                                }
                            }

                        }

                        it.ref.addValueEventListener(dumbbellWeightQueueListener)
                        waitQueueListeners[it.getValue(Dumbbell::class.java)!!.weightValue] =
                            dumbbellWeightQueueListener
                    }

            }

        })


    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            view!!.findViewById<TextView>(R.id.text_workout_station).text = getString(R.string.workout_station, currentBench)
        }
    }

    private fun getWeightData() {
        weightReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val newDumbbells: MutableList<Dumbbell> = mutableListOf()
                for (dumbbellSnapshot in dataSnapshot.children) {
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

    private fun createRequest(dumbbellAvailable: Boolean, weightValue: String) {

        val now = LocalDateTime.now(ZoneOffset.UTC)
        val seconds =
            now.atZone(ZoneOffset.UTC).toEpochSecond() //request time is always in seconds
        val milliseconds = now.atZone(ZoneOffset.UTC)?.toInstant()
            ?.toEpochMilli() //request ID is in milli seconds

        val requestID = milliseconds.toString()
        val status = if (dumbbellAvailable) "delivering" else "waiting"
        val path = if (dumbbellAvailable) "activeRequests" else "waitQueue"
        val bench = currentBench

        val newRequest = Request(requestID, seconds, status, weightValue, bench)
        requests[requestID] = newRequest
        requireActivity().toast("${requests.values}")


        val formattedWeight = weightValue.replace(
            '.',
            '-',
            true
        ) //change the weight value from 4.0 to 4-0 for firebase

        if (dumbbellAvailable) requestReference.child(requestID).setValue(newRequest)
//        else ref.child("demo2/waitQueue/$requestID").setValue("$bench, $formattedWeight")

        weightReference.child("$formattedWeight/$path/$requestID").setValue(bench)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
//                    todo - change if wait queue
//                    todo - start listening if wait queue
                    (activity as MainActivity).onSuccessfulOrder(weightValue)
                    if (!dumbbellAvailable && !waitQueueListeners.containsKey(weightValue.toDouble())) {
                        listenToDumbbellWaitQueue(weightValue.toDouble(), formattedWeight)
                    }
                } else {
                    Log.w(fragTag, "Failed to send request $requestID", task.exception)
                    requireActivity().toast("There was an error sending your dumbbell request. Please try again later.")
                }
            }

        Log.d(
            fragTag,
            "Sending request $requestID to server (deliver dumbbells of $weightValue kg to bench $currentBench)"
        )
    }

    private fun listenToDumbbellWaitQueue(weightValue: Double, formattedWeight: String) {
        val dumbbellWaitQueueListener = object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(fragTag, "Something happened :)", databaseError.toException())
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val weight = dataSnapshot.getValue(Dumbbell::class.java)!!
                if ((weight.totalStock - weight.activeRequests.size) > 0) {
                    if (weight.waitQueue.isNotEmpty()) {
                        val nextInQueue = weight.waitQueue.toSortedMap().asIterable().first()
                        if (nextInQueue.value == currentBench) {
                            weight.waitQueue.remove(nextInQueue.key, nextInQueue.value)

                            if (!weight.waitQueue.containsValue(currentBench)) {
                                waitQueueListeners.remove(weight.weightValue)
                            }

                            dataSnapshot.ref.updateChildren(mapOf("waitQueue" to weight.waitQueue)) { _, _ ->
                                requireActivity().toast("Your queued ${weight.weightValue} kg weight is being delivered")
                                createRequest(true, weight.weightValue.toString())
                            }
                        }
                    }

                }
            }
        }

        weightReference.child(formattedWeight).addValueEventListener(dumbbellWaitQueueListener)
        waitQueueListeners.putIfAbsent(weightValue, dumbbellWaitQueueListener)
    }


    companion object {
        @JvmStatic
        fun newInstance() = OrderFragment()
    }

    inner class DumbbellRequestAdapter : RecyclerView.Adapter<DumbbellRequestAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(this@OrderFragment.requireContext()).inflate(
                    R.layout.item_order_dumbbell,
                    parent, false
                )
            )

        override fun getItemCount(): Int = weights.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.orderButton.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorDumbbellAvailable))

            val requestedWeight = weights[position]

            holder.apply {
                val currentStock = requestedWeight.totalStock - requestedWeight.activeRequests.size
                val dumbbellAvailable = currentStock > 0

                val availableTextResId =
                    if (dumbbellAvailable) R.string.available else R.string.unavailable
                val orderButtonTextResId =
                    if (dumbbellAvailable) R.string.order else R.string.join_wait_queue
                val dialogTitle = if (dumbbellAvailable) "Confirm Order" else "Join Wait Queue"
                val dialogMessage =
                    if (dumbbellAvailable) "Are you sure you would like to order the ${holder.weightValue.text} dumbbells?"
                    else "The ${holder.weightValue.text} dumbbells are currently unavailable. Would you like to join the wait queue?"
                val orderButtonColorResId =
                    if (dumbbellAvailable) R.color.colorDumbbellAvailable else R.color.colorDumbbellUnavailable
                val dumbbellDetails: Triple<Int, Int, Int> =
                    if (dumbbellAvailable) Triple(
                        R.string.available_dumbbells_info,
                        currentStock,
                        requestedWeight.totalStock
                    )
                    else Triple(R.string.wait_queue_info, requestedWeight.totalStock, requestedWeight.waitQueue.size)

                weightValue.text =
                    getString(R.string.weight, decimalFormat.format(requestedWeight.weightValue))
                available.text = getString(availableTextResId)
                orderButton.text = getString(orderButtonTextResId)
                availabilityInfo.text = getString(dumbbellDetails.first, dumbbellDetails.second, dumbbellDetails.third)
                orderButton.backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            orderButtonColorResId
                        )
                    )
                orderButton.setOnClickListener {
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle(dialogTitle)
                    builder.setMessage(dialogMessage)
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




        /*private fun createRequest(dumbbellAvailable: Boolean, weightValue: String) {

            val now = LocalDateTime.now(ZoneOffset.UTC)
            val seconds = now.atZone(ZoneOffset.UTC).toEpochSecond() //request time is always in seconds
            val milliseconds = now.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() //request ID is in milli seconds

            val requestID = milliseconds.toString()
            val status = if (dumbbellAvailable) "delivering" else "waiting"
            val path = if (dumbbellAvailable) "activeRequests" else "waitQueue"
            val bench = currentBench

            val newRequest = Request(requestID, requestID, seconds, status, weightValue, bench)
            requests[requestID] = newRequest
            //requireActivity().toast("${requests.values}")
            requestReference.child(requestID).setValue(newRequest)

            val formattedWeight = weightValue.replace('.', '-', true) //change the weight value from 4.0 to 4-0 for firebase

            weightReference.child("$formattedWeight/$path/$requestID").setValue(bench)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        (activity as MainActivity).onSuccessfulOrder(weightValue)
                    } else {
                        Log.w(fragTag, "Failed to send request $requestID", task.exception)
                        requireActivity().toast("There was an error sending your dumbbell request. Please try again later.")
                    }
                }

            Log.d(fragTag,
                "Sending request $requestID to server (deliver dumbbells of $weightValue kg to bench $currentBench)"
            )
        }*/


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val weightValue: TextView = view.text_total_stock
            val available: TextView = view.text_available
            val availabilityInfo: TextView = view.text_wait_queue
            val orderButton: Button = view.btn_order_dumbbell
        }
    }
}
