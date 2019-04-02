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
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_order_weights.view.*
import kotlinx.android.synthetic.main.item_order_dumbbell.view.*
import org.jetbrains.anko.longToast
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
                        it.getValue(Dumbbell::class.java)!!.waitQueue.containsValue(currentBench)
                    }
                    .forEach {
                        val dumbbellWeightQueueListener = object : ValueEventListener {
                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.w(
                                    fragTag, "Failed to load weight data",
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

                                        val newRequestId = createRequestID()

                                        weightReference.child(
                                            weight.weightValue.toString().replace(".", "-", true)
                                        )
                                            .runTransaction(object : Transaction.Handler {
                                                override fun onComplete(
                                                    p0: DatabaseError?,
                                                    p1: Boolean,
                                                    p2: DataSnapshot?
                                                ) {
                                                    createRequest(
                                                        true,
                                                        weight.weightValue.toString(),
                                                        newRequestId
                                                    )
                                                }

                                                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                                    val p =
                                                        mutableData.getValue(Dumbbell::class.java)!!
                                                    p.waitQueue.remove(
                                                        nextInQueue.key,
                                                        nextInQueue.value
                                                    )
                                                    p.activeRequests[newRequestId] =
                                                        currentBench
                                                    mutableData.value = p
                                                    return Transaction.success(mutableData)

                                                }

                                            })

                                    }

                                }
                            }

                        }

                        if (waitQueueListeners[it.getValue(Dumbbell::class.java)!!.weightValue] == null) {
                            it.ref.addValueEventListener(dumbbellWeightQueueListener)
                            waitQueueListeners.putIfAbsent(
                                it.getValue(Dumbbell::class.java)!!.weightValue,
                                dumbbellWeightQueueListener
                            )
                        }
                    }

            }

        })


    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            view!!.findViewById<TextView>(R.id.text_workout_station).text =
                getString(R.string.workout_station, currentBench)
        }
        setupRecyclerView()
    }

    private fun getWeightData() {
        weightReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val newDumbbells: MutableList<Dumbbell> = mutableListOf()
                for (dumbbellSnapshot in dataSnapshot.children) {
                    val dumbbell = dumbbellSnapshot.getValue(Dumbbell::class.java)
                    if (dumbbell != null) newDumbbells += dumbbell
                }
                weights = newDumbbells.sortedBy { it.weightValue }.toMutableList()
                orderDumbbellRecyclerView!!.adapter!!.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(fragTag, "loadDumbbells:onCancelled", databaseError.toException())
                requireActivity().toast("Failed getting weights, please try again")
            }

        })
    }

    fun setupRecyclerView() {
        orderDumbbellRecyclerView!!.layoutManager = LinearLayoutManager(requireContext())
        orderDumbbellRecyclerView!!.adapter = DumbbellRequestAdapter()

        numberOfRequests = 0
        for (request in requests.values) {
            if (request.type == "delivering" || request.type == "current") {
                numberOfRequests++

            }
        }

        getWeightData()
    }

    private fun createRequestID(): String {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val milliseconds = now.atZone(ZoneOffset.UTC)?.toInstant()
            ?.toEpochMilli() //request ID is in milli seconds

        return milliseconds.toString()
    }

    private fun createRequest(
        dumbbellAvailable: Boolean,
        weightValue: String,
        newRequestId: String? = null
    ) {

        val now = LocalDateTime.now(ZoneOffset.UTC)
        val seconds =
            now.atZone(ZoneOffset.UTC).toEpochSecond() //request time is always in seconds

        val requestID = newRequestId ?: createRequestID()
        val status = if (dumbbellAvailable) "delivering" else "waiting"
        val path = if (dumbbellAvailable) "activeRequests" else "waitQueue"
        val bench = currentBench

        val newRequest = Request(requestID, requestID, seconds, status, weightValue, bench)
        requests[requestID] = newRequest
//        requireActivity().toast("${requests.values}")

        val formattedWeight = weightValue.replace('.', '-', true)

        if (dumbbellAvailable) requestReference.child(requestID).setValue(newRequest)

        weightReference.child("$formattedWeight/$path/$requestID").setValue(bench)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    (activity as MainActivity).onSuccessfulOrder(weightValue, status)
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
        (activity as MainActivity).updateCurrentWorkout()
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
                            requireActivity().longToast(requests.values.toString())
                            weight.waitQueue.remove(nextInQueue.key, nextInQueue.value)

                            if (!weight.waitQueue.containsValue(currentBench)) {
                                waitQueueListeners.remove(weight.weightValue)
                            }

                            val newRequestId = createRequestID()

                            weightReference.child(weightValue.toString().replace(".", "-", true))
                                .runTransaction(object : Transaction.Handler {
                                    override fun onComplete(
                                        p0: DatabaseError?,
                                        p1: Boolean,
                                        p2: DataSnapshot?
                                    ) {
                                        createRequest(
                                            true,
                                            weight.weightValue.toString(),
                                            newRequestId
                                        )
                                    }

                                    override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                        val p = mutableData.getValue(Dumbbell::class.java)!!
                                        p.waitQueue.remove(nextInQueue.key, nextInQueue.value)
                                        p.activeRequests[newRequestId] = currentBench
                                        mutableData.value = p
                                        return Transaction.success(mutableData)

                                    }

                                })
                            //todo change
                            //remove from requests and update
                            requests.remove(nextInQueue.key)
                            requireActivity().toast("removing ${nextInQueue.key}")
                            // requireActivity().longToast("Your queued ${weight.weightValue}kg dumbbells are now available and have been ordered!")
                            (activity as MainActivity).updateCurrentWorkout()
                        }
                    }

                }
                setupRecyclerView() //check if there has been a change to the current number of dumbbells
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
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorDumbbellAvailable
                    )
                )

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
                    else "The ${holder.weightValue.text} dumbbells are currently unavailable. Would you like to join the wait queue?" //todo change this
                val orderButtonColorResId =
                    if (dumbbellAvailable) R.color.colorDumbbellAvailable else R.color.colorDumbbellUnavailable
                val dumbbellDetails: Triple<Int, Int, Int> =
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

                weightValue.text =
                    getString(R.string.weight, decimalFormat.format(requestedWeight.weightValue))
                available.text = getString(availableTextResId)
                orderButton.text = getString(orderButtonTextResId)
                availabilityInfo.text =
                    getString(dumbbellDetails.first, dumbbellDetails.second, dumbbellDetails.third)
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
                    builder.setPositiveButton("CONFIRM") { dialog, _ ->
                        if (numberOfRequests >= 2) { //todo test if this works
                            holder.orderButton.isEnabled = false
                            dialog.cancel()
                            val builder2 = AlertDialog.Builder(context)
                            builder2.setMessage("Weight is now unavailable, please choose another weight")
                            builder2.setPositiveButton("OKAY") { dialog, _ -> dialog.cancel() }
                            builder.create().show()
                        } else createRequest(
                            dumbbellAvailable,
                            requestedWeight.weightValue.toString()
                        )
                        (activity as MainActivity).checkRequestLimit()
                        setupRecyclerView() //recheck to see if this changes the limit
                    }
                    builder.setNeutralButton("CANCEL") { dialog, _ -> dialog.cancel() }

                    val dialog = builder.create()
                    dialog.show()
                }
            }

            if (numberOfRequests >= 2) {
                holder.orderButton.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.material_grey_100
                        )
                    )
                    text = "2 weight limit reached"
                }
            }




            fun createRequest(dumbbellAvailable: Boolean, weightValue: String) {

                val now = LocalDateTime.now(ZoneOffset.UTC)
                val seconds =
                    now.atZone(ZoneOffset.UTC).toEpochSecond() //request time is always in seconds
                val milliseconds = now.atZone(ZoneOffset.UTC)?.toInstant()
                    ?.toEpochMilli() //request ID is in milli seconds

                val requestID = milliseconds.toString()
                val status = if (dumbbellAvailable) "delivering" else "waiting"
                val path = if (dumbbellAvailable) "activeRequests" else "waitQueue"
                val bench = currentBench

                val newRequest = Request(requestID, requestID, seconds, status, weightValue, bench)
                requests[requestID] = newRequest
                requireActivity().toast("${requests.values}")
                requestReference.child(requestID).setValue(newRequest)

                val formattedWeight = weightValue.replace(
                    '.',
                    '-',
                    true
                ) //change the weight value from 4.0 to 4-0 for firebase

                weightReference.child("$formattedWeight/$path/$requestID").setValue(bench)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            (activity as MainActivity).onSuccessfulOrder(weightValue, status)
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
        }


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val weightValue: TextView = view.text_total_stock
            val available: TextView = view.text_available
            val availabilityInfo: TextView = view.text_wait_queue
            val orderButton: Button = view.btn_order_dumbbell
        }
    }
}

