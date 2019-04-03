package com.example.sophieleaver.dumbotapp

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.AlertDialog
import android.graphics.Color
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.cancel_collection_view.view.*
import kotlinx.android.synthetic.main.cancel_delivery_view.view.*
import kotlinx.android.synthetic.main.current_dumbbell_view.view.*
import kotlinx.android.synthetic.main.fragment_current_orders.view.*
import kotlinx.android.synthetic.main.item_current_dumbbell.view.*
import kotlinx.android.synthetic.main.item_queued_dumbbell.view.*
import kotlinx.android.synthetic.main.reset_warning_view.view.*
import org.jetbrains.anko.toast
import java.time.LocalDateTime
import java.time.ZoneOffset


@Suppress("DEPRECATION")
@SuppressLint("InflateParams")
class CurrentOrdersFragment : Fragment() {
    lateinit var currentDBRecyclerView: RecyclerView
    lateinit var queuedDBRecyclerView: RecyclerView

    private val ref = FirebaseDatabase.getInstance().reference
    private val decimalFormat = DecimalFormat("##.##")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_current_orders, container, false)
        currentDBRecyclerView = view.recyclerView_current_dumbbells
        currentDBRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        currentDBRecyclerView.adapter = CurrentDumbbellAdapter()

        queuedDBRecyclerView = view.recyclerView_queued_dumbbells
        queuedDBRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        queuedDBRecyclerView.adapter = QueuedDumbbellAdapter()

//        adapter.notifyDataSetChanged()

        view.fab_timer.setOnClickListener { (requireActivity() as MainActivity).showTimeFragment() }


        val resetButton: Button = view.findViewById(R.id.button_reset_workout_session)
        resetButton.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            val warningView = layoutInflater.inflate(R.layout.reset_warning_view, null)
            builder.setView(warningView)

            val dialog = builder.create()
            dialog.show()

            warningView.button_cancel_warning.setOnClickListener { dialog.cancel() }
            warningView.button_confirm_reset.setOnClickListener {

                requests.filter { it.value.type == "current" }.forEach {
                    val req = it.value

                    val now3 = LocalDateTime.now(ZoneOffset.UTC)
                    val unixSeconds = now3.atZone(ZoneOffset.UTC)?.toEpochSecond()
                    val unixMilli = now3.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli().toString()

                    //update id and type in requests to new request information
                    val currentDumbbellReq: Request = requests[req.id]!!.copy()
                    currentDumbbellReq.id = unixMilli
                    currentDumbbellReq.type = "collecting"

                    requests.remove(req.id) //remove the request with the delivering id
                    requests[currentDumbbellReq.id] = currentDumbbellReq
                    currentDBRecyclerView.adapter!!.notifyDataSetChanged()

                    //send request to firebase
                    val newRequest = ref.child("demo2").child("requests").child(currentDumbbellReq.id)
                    newRequest.child("bench").setValue(currentDumbbellReq.bench)
                    newRequest.child("time").setValue(unixSeconds)
                    newRequest.child("type").setValue("collecting")
                    newRequest.child("weight").setValue(currentDumbbellReq.weight)
                }

                requests.filter { it.value.type == "waiting" }.forEach {
                    val req = it.value

                    requests.remove(req.id) //remove the request with the delivering id
                    queuedDBRecyclerView.adapter!!.notifyDataSetChanged()

                    val formattedWeight = req.weight.replace(".", "-")
                    ref.child("demo2/weights/$formattedWeight/waitQueue/${req.id}").removeValue()
                }

                dialog.cancel()
                Toast.makeText(context, "Current workout has been rest", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerViews()
    }

    fun setUpRecyclerViews() {
        currentDBRecyclerView.layoutManager = LinearLayoutManager(this.requireContext())
        currentDBRecyclerView.adapter = CurrentDumbbellAdapter()

        queuedDBRecyclerView.layoutManager = LinearLayoutManager(this.requireContext())
        queuedDBRecyclerView.adapter = QueuedDumbbellAdapter()
    }


    companion object {
        @JvmStatic
        fun newInstance() = CurrentOrdersFragment()
    }

    inner class CurrentDumbbellAdapter : RecyclerView.Adapter<CurrentDumbbellAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(this@CurrentOrdersFragment.requireContext()).inflate(
                    R.layout.item_current_dumbbell,
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = if (requests.size == getWaitSize()) 1 else requests.size + 1

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (itemCount == 1) {
                //if there is only one item then the list is empty -> convert it into a current_description saying it is empty
                holder.weight.visibility = View.INVISIBLE
                holder.current_description.visibility = View.INVISIBLE
                holder.button.visibility = View.INVISIBLE
                holder.divider.visibility = View.INVISIBLE
                holder.emptyText.visibility = View.VISIBLE
                holder.timer.visibility = View.INVISIBLE
                holder.transported_description.visibility = View.INVISIBLE
                holder.background.setBackgroundColor(Color.rgb(242, 242, 242))
            }
            if (itemCount > 1) {
                if (position < itemCount - 1) {

                    val request: Request = requests.toList()[position].second
                    val now = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)!!.toInstant()!!.toEpochMilli()
                    val baseTime = (SystemClock.elapsedRealtime() - (now - (request.time*1000)))

                    val type = request.type
                    holder.id = request.id
                    holder.weight.text =
                        getString(R.string.weight, decimalFormat.format(request.weight.toDouble()))

                    holder.emptyText.visibility = View.INVISIBLE
                    holder.background.setBackgroundColor(Color.WHITE)

                    if (type == "delivering") {
                        //set the delivery view
                        holder.timer.visibility = View.INVISIBLE

                        holder.transported_description.visibility = View.VISIBLE
                        holder.current_description.visibility = View.INVISIBLE
                        holder.transported_description.text = getString(R.string.dumbbell_being_delivered)

                        holder.button.text = getString(R.string.cancel)
                        holder.button.visibility = View.INVISIBLE
                        //when the cancel button is clicked
                        holder.button.setOnClickListener {
                            //create an alert dialog with a custom view
                            val builder = AlertDialog.Builder(context)
                            val cancelDeliveryView = layoutInflater.inflate(R.layout.cancel_delivery_view, null)
                            builder.setView(cancelDeliveryView)

                            cancelDeliveryView.text_delivery_cancel.text =
                                getString(R.string.cancel_dumbbell, "delivery", holder.weight.text)

                            //create the dialog
                            val dialog: AlertDialog = builder.create()
                            dialog.show()

                            //button - return to current orders
                            cancelDeliveryView.button_return_del.setOnClickListener { dialog.cancel() }

                            //button - confirm cancellation of delivery
                            cancelDeliveryView.button_confirm_del.setOnClickListener {
                                val now1 = LocalDateTime.now(ZoneOffset.UTC)
                                val unixSeconds = now1.atZone(ZoneOffset.UTC)?.toEpochSecond()

                                //add cancellation to firebase
                                ref.child("demo2").child("cancelledRequests").child(request.id).setValue(unixSeconds)

                                //delete request from recyclerview

                                requests.remove(request.id)
                                currentDBRecyclerView.adapter!!.notifyDataSetChanged()
                                //                                currentDBRecyclerView.removeViewAt(position)
                                dialog.cancel()
                            }
                        }
                    }

                    if (type == "collecting") {
                        //set the collecting view
                        holder.timer.visibility = View.INVISIBLE

                        holder.transported_description.visibility = View.VISIBLE
                        holder.current_description.visibility = View.INVISIBLE

                        holder.transported_description.text = getString(R.string.dumbbell_being_collected)
                        holder.button.visibility = View.INVISIBLE

                        //when the cancel button is clicked
                        holder.button.setOnClickListener {
                            //set custom view
                            val builder = AlertDialog.Builder(context)
                            val cancelCollectionView = layoutInflater.inflate(R.layout.cancel_collection_view, null)
                            builder.setView(cancelCollectionView)
                            cancelCollectionView.text_collection_cancel.text =
                                getString(R.string.cancel_dumbbell, "collection", holder.weight.text)

                            //create dialog
                            val dialog: AlertDialog = builder.create()
                            dialog.show()

                            //button - return to current sessions
                            cancelCollectionView.button_return_col.setOnClickListener { dialog.cancel() }
                            //button - end the current workout
                            cancelCollectionView.button_confirm_col.setOnClickListener {
                                val now2 = LocalDateTime.now(ZoneOffset.UTC)
                                val unixSeconds = now2.atZone(ZoneOffset.UTC)?.toEpochSecond()

                                requests[request.id]!!.type = "current"
                                requests[request.id]!!.id =
                                    now2.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli().toString()
                                request.id = now2.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli().toString()
                                ref.child("demo2").child("cancelledRequests").child(request.id).setValue(unixSeconds)

                                currentDBRecyclerView.adapter!!.notifyDataSetChanged()
                                notifyDataSetChanged()
                                dialog.cancel()
                            }
//
                        }

                    }
                    if (type == "current") {
                        holder.timer.visibility = View.VISIBLE

                        holder.current_description.visibility = View.VISIBLE
                        holder.transported_description.visibility = View.INVISIBLE

                        holder.current_description.text = getString(R.string.current_dumbbell)
                        holder.button.text = getString(R.string.return_dumbbell)


                        holder.timer.start()
                        holder.timer.base = baseTime

                        holder.button.setOnClickListener {
                            val builder = AlertDialog.Builder(context)
                            val currentSessionView = layoutInflater.inflate(R.layout.current_dumbbell_view, null)
                            builder.setView(currentSessionView)
                            currentSessionView.text_title_currentDB.text =
                                getString(R.string.dumbbell, holder.weight.text)
                            currentSessionView.text_session_time.base = baseTime
                            currentSessionView.text_session_time.start()
                            val dialog = builder.create()
                            dialog.show()

                            currentSessionView.button_return_cur.setOnClickListener {dialog.cancel()}

                            currentSessionView.button_end_workout.setOnClickListener {
                                ref.child("demo2/weights/${request.weight.toInt()}/activeRequests/${request.id}").removeValue()
                                requireActivity().toast("demo2/weights/${request.weight.toInt()}/activeRequests/${request.id}")

                                //set entry in request hashmap to collecting
                                val now3 = LocalDateTime.now(ZoneOffset.UTC)
                                val unixSeconds = now3.atZone(ZoneOffset.UTC)?.toEpochSecond()
                                val unixMilli = now3.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli().toString()

                                //update id and type in requests to new request information
                                val currentDumbbellReq: Request = requests[request.id]!!.copy()
                                currentDumbbellReq.id = unixMilli
                                currentDumbbellReq.type = "collecting"
                                requests.remove(request.id) //remove the request with the delivering id
                                requests[currentDumbbellReq.id] = currentDumbbellReq
                                notifyDataSetChanged()

                                //send request to firebase
                                val newRequest = ref.child("demo2/requests").child(currentDumbbellReq.id)
                                newRequest.child("bench").setValue(request.bench)
                                newRequest.child("time").setValue(unixSeconds)
                                newRequest.child("type").setValue("collecting")
                                newRequest.child("weight").setValue(request.weight)

                                notifyDataSetChanged()
                                dialog.cancel()

                                //update recycler in weightorders
                                (activity as MainActivity).checkRequestLimit()
                            }
                        }
                    }
                    if (type == "waiting"){
                        //make invisible
                        holder.background.layoutParams = LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, 0)
                    }
                }

                if (position == itemCount - 1) {
                    //give final holder a height of zero to make invisible?
                    holder.background.layoutParams =
                        LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, 0)
                }
            }
        }

        fun getWaitSize() : Int {
            var size = 0
            for (request in requests.values){
                if (request.type == "waiting") size++
            }
            return size
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val weight: TextView = view.textView_current_dumbbell_weight
            var id: String = ""
            val current_description: TextView = view.textView_current_dumbbell_status
            val transported_description : TextView = view.textView_delivered_collected_dumbell
            val emptyText: TextView = view.text_no_current_dumbbells
            val button: Button = view.button_cancel_curr
            val divider: View = view.divider_current
            var timer: Chronometer = view.text_dumbbell_timer
            var background = view
        }


    }

    inner class QueuedDumbbellAdapter :
        RecyclerView.Adapter<QueuedDumbbellAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(this@CurrentOrdersFragment.requireContext())
                    .inflate(R.layout.item_queued_dumbbell, parent, false)
            )

        override fun getItemCount(): Int = if (getWaitSize() == 0) 1 else requests.size + 1 //change

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (itemCount == 1){
                holder.weight.visibility = View.INVISIBLE
                holder.current_description.visibility = View.INVISIBLE
                holder.button.visibility = View.INVISIBLE
                holder.divider.visibility = View.INVISIBLE
                holder.emptyText.visibility = View.VISIBLE
                holder.background.setBackgroundColor(Color.rgb(242,242,242))
            }
            if (itemCount > 1){
                holder.background.setBackgroundColor(Color.WHITE)
                if (position < itemCount - 1) {
                    val request: Request = requests.toList()[position].second
                    if (request.type == "waiting") {
                        holder.emptyText.visibility = View.INVISIBLE
                        holder.current_description.visibility = View.VISIBLE
                        holder.weight.text = if (request.weight.contains(".0")) {
                            "${request.weight.replace(".0", "")} KG"
                        } else {
                            "${request.weight} KG"
                        }
                        holder.current_description.text = "Currently in wait queue."

                       // val currentPosition = findPlaceInWaitQueue(request.id, request.weight)

                        holder.button.setOnClickListener {
                            findPlaceInWaitQueue(request.id, request.weight)
                        }
                    }
                    else {
                        holder.background.layoutParams = LinearLayout.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, 0)
                    }
                }
                if (position == itemCount -1){
                    holder.background.layoutParams = LinearLayout.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, 0)

                }
            }
        }

        private fun getWaitSize() : Int {
            var size = 0
            for (request in requests.values){
                if (request.type == "waiting") size++
            }
            return size
        }

        private fun findPlaceInWaitQueue(id : String, weightValue : String){ //TODO DO THIS
            var index = 0
            var place = 0
            val formattedWeightPath = weightValue.replace(".", "-")
            val listener = object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val weight = snap.getValue(Dumbbell::class.java)!!
                    for (request in weight.waitQueue.toSortedMap().asIterable()){
                        Log.d("currentorders", "$index - ${request.key}")
                        if (request.key == id){
                            place = index
                            Log.d("currentorders", "YES, $place")
                            createPersonalisedDialog(id, weightValue, place)
                        }
                        index++
                    }
                }

                override fun onCancelled(p0: DatabaseError) {}

            }

            ref.child("demo2/weights/$formattedWeightPath").addListenerForSingleValueEvent(listener)
            Log.d("currentorders", "return $place")
            //return place
        }

        fun createPersonalisedDialog(id : String, weightValue: String, position : Int){
            val builder = AlertDialog.Builder(context)
            builder.apply {
                setTitle("Are you sure you would like to leave the queue?")
                setMessage(
                    "Leaving the queue for the ${weightValue}kg dumbbells cannot be undone.\n" +
                            "You are currently ${position} line for this dumbbell."
                )
                setPositiveButton("CONFIRM") { _, _ ->
                    //remove from queue
                    val formattedWeight = weightValue.replace('.', '-', false)
                    ref.child("demo2/weights/$formattedWeight/waitQueue/${id}")
                        .removeValue()

                    requests.remove(id)
                    notifyDataSetChanged()
                    setUpRecyclerViews()
                }
                setNegativeButton("CANCEL") { _, _ -> }
            }

            val dialog = builder.create()
            dialog.show()
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val weight : TextView = view.textView_queued_dumbbell_weight
            var id : String = ""
            val current_description : TextView = view.textView_queued_dumbbell_status
            val emptyText = view.text_no_queued_dumbbells
            val button : Button = view.button_leave_que
            val divider = view.divider_queued
            val background = view
        }
    }

}
