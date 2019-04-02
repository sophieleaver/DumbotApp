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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.cancel_collection_view.view.*
import kotlinx.android.synthetic.main.cancel_delivery_view.view.*
import kotlinx.android.synthetic.main.current_dumbbell_view.view.*
import kotlinx.android.synthetic.main.fragment_current_orders.view.*
import kotlinx.android.synthetic.main.item_current_dumbbell.view.*
import kotlinx.android.synthetic.main.reset_warning_view.view.*
import org.jetbrains.anko.toast
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * TODO loophole needs to be fixed - has a weight, returns it, orders two more, cancels return
 *
 */

@SuppressLint("InflateParams")
class CurrentOrdersFragment : Fragment() {
    lateinit var currentDBRecyclerView: RecyclerView
    private lateinit var queuedDBRecyclerView: RecyclerView

    private val ref = FirebaseDatabase.getInstance().reference
    private val decimalFormat = DecimalFormat("##.##")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_current_orders, container, false)
        //1554127890698
//        requests.put("1554127891220" ,Request("1554127891220","1554127891220", 1554127891, "current", "1.0", "B7"))
        currentDBRecyclerView = view.recyclerView_current_dumbbells
        currentDBRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        currentDBRecyclerView.adapter = CurrentDumbbellAdapter()

//        queuedDBRecyclerView = view.recyclerView_queued_dumbbells
//        queuedDBRecyclerView.layoutManager = LinearLayoutManager(requireContext())
//        queuedDBRecyclerView.adapter = QueuedDumbbellAdapter()

//        adapter.notifyDataSetChanged()

        view.fab_timer.setOnClickListener { (requireActivity() as MainActivity).showTimeFragment() }


        val cancelButton: Button = view.findViewById(R.id.button_reset_workout_session)
        cancelButton.setOnClickListener {
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

        //queuedDBRecyclerView.layoutManager = LinearLayoutManager(this.requireContext())
        //queuedDBRecyclerView.adapter = QueuedDumbbellAdapater2()
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

        override fun getItemCount(): Int = requests.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (itemCount == 1) {
                //if there is only one item then the list is empty -> convert it into a current_description saying it is empty
                holder.weight.visibility = View.INVISIBLE
                holder.current_description.visibility = View.INVISIBLE
                holder.button.visibility = View.INVISIBLE
                holder.divider.visibility = View.INVISIBLE
                holder.emptyText.visibility = View.VISIBLE
//                holder.transported_description.visibility = View.INVISIBLE
                holder.background.setBackgroundColor(Color.rgb(242, 242, 242))
            }
            if (itemCount > 1) {
                if (position < itemCount - 1) {

                    val request: Request = requests.toList()[position].second
                    val now = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)!!.toInstant()!!.toEpochMilli()
//                    val baseTime = (SystemClock.elapsedRealtime() - (request.time * 1000 -  now))//1554127891
                    val baseTime = SystemClock.elapsedRealtime() - (now - request.id.toLong())


                    val type = request.type
                    holder.id = request.id
                    holder.weight.text =
                        getString(R.string.weight, decimalFormat.format(request.weight.toDouble()))

                    holder.emptyText.visibility = View.INVISIBLE
                    holder.background.setBackgroundColor(Color.WHITE)

                    if (type == "delivering") {
                        //set the delivery view
                        holder.timer.visibility = View.INVISIBLE

//                        holder.transported_description.visibility = View.VISIBLE
                        holder.current_description.visibility = View.INVISIBLE
//                        holder.transported_description.text = getString(R.string.dumbbell_being_delivered)

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

                                requireActivity().toast("${request.id} and key ${requests[request.id]}")
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

//                        holder.transported_description.visibility = View.VISIBLE
                        holder.current_description.visibility = View.INVISIBLE

//                        holder.transported_description.text = getString(R.string.dumbbell_being_collected)
                        holder.button.visibility = View.INVISIBLE

                        //holder.button.text = getString(R.string.cancel)

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
//                        holder.transported_description.visibility = View.INVISIBLE

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

                            currentSessionView.button_return_cur.setOnClickListener { dialog.cancel() }

                            currentSessionView.button_end_workout.setOnClickListener {
                                //requireActivity().toast("demo2/weights/${request.weight.toInt()}/activeRequests/${request.id}")

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
                                //requireActivity().toast(currentDumbbellReq.toString())
                                notifyDataSetChanged()

                                //send request to firebase
                                val newRequest = ref.child("demo2/requests").child(currentDumbbellReq.id)
                                newRequest.child("bench").setValue(request.bench)
                                newRequest.child("time").setValue(unixSeconds)
                                newRequest.child("type").setValue("collecting")
                                newRequest.child("weight").setValue(request.weight)

                                notifyDataSetChanged()
                                dialog.cancel()
                            }
                        }
                    }
                }

                if (position == itemCount - 1) {
                    //give final holder a height of zero to make invisible?
                    holder.background.layoutParams = LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, 0)
                }
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val weight: TextView = view.textView_current_dumbbell_weight
            var id: String = ""
            val current_description: TextView = view.textView_current_dumbbell_status
            //            val transported_description : TextView = view.textView_delivered_collected_dumbell
            val emptyText: TextView = view.text_no_current_dumbbells
            val button: Button = view.button_cancel_curr
            val divider: View = view.divider_current
            var timer: Chronometer = view.text_dumbbell_timer
            var background = view
        }


    }

    inner class QueuedDumbbellAdapater2 :
        RecyclerView.Adapter<QueuedDumbbellAdapater2.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(this@CurrentOrdersFragment.requireContext()).inflate(
                    R.layout.item_current_dumbbell,
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = requests.size + 1 //change

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (itemCount == 1) {
                //if there is only one item then the list is empty -> convert it into a description saying it is empty
                holder.weight.visibility = View.INVISIBLE
                holder.description.visibility = View.INVISIBLE
                holder.button.visibility = View.INVISIBLE
                holder.divider.visibility = View.INVISIBLE
                holder.emptyText.visibility = View.VISIBLE
                holder.background.setBackgroundColor(Color.rgb(242, 242, 242))
            }
            if (itemCount > 1) {
                if (position < itemCount - 1) {

                    val request: Request = requests.toList()[position].second
                    val now =
                        LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)!!.toInstant()!!.toEpochMilli()
                    val baseTime = SystemClock.elapsedRealtime() - (now - (request.time * 1000))

                    val type = request.type
                    holder.id = request.id
                    holder.weight.text =
                        getString(R.string.weight, decimalFormat.format(request.weight.toDouble()))

                    holder.emptyText.visibility = View.INVISIBLE
                    holder.background.setBackgroundColor(Color.WHITE)

                    if (type == "delivering") {
                        //set the delivery view
                        holder.timer.visibility = View.INVISIBLE
                        holder.description.text = getString(R.string.dumbbell_being_delivered)
                        holder.button.text = getString(R.string.cancel)

                        //when the cancel button is clicked
                        holder.button.setOnClickListener {
                            //create an alert dialog with a custom view
                            val builder = AlertDialog.Builder(context)
                            val cancelDeliveryView =
                                layoutInflater.inflate(R.layout.cancel_delivery_view, null)
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
                                ref.child("demo2").child("cancelledRequests").child(request.id)
                                    .setValue(unixSeconds)

                                //delete request from recyclerview

                                requireActivity().toast("${request.id} and key ${requests[request.id]}")
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
                        holder.description.text = getString(R.string.dumbbell_being_collected)
                        holder.button.text = getString(R.string.cancel)

                        //when the cancel button is clicked
                        holder.button.setOnClickListener {
                            //set custom view
                            val builder = AlertDialog.Builder(context)
                            val cancelCollectionView =
                                layoutInflater.inflate(R.layout.cancel_collection_view, null)
                            builder.setView(cancelCollectionView)
                            cancelCollectionView.text_collection_cancel.text =
                                getString(
                                    R.string.cancel_dumbbell,
                                    "collection",
                                    holder.weight.text
                                )

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
                                    now2.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
                                        .toString()
                                request.id =
                                    now2.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
                                        .toString()
                                ref.child("demo2").child("cancelledRequests").child(request.id)
                                    .setValue(unixSeconds)

                                currentDBRecyclerView.adapter!!.notifyDataSetChanged()
                                notifyDataSetChanged()
                                dialog.cancel()
                            }
//
                        }

                    }
                    if (type == "current") {

                        holder.timer.visibility = View.VISIBLE
                        holder.description.text = getString(R.string.current_dumbbell)
                        holder.button.text = getString(R.string.return_dumbbell)
                        holder.timer.base = baseTime
                        holder.timer.start()

                        holder.button.setOnClickListener {
                            val builder = AlertDialog.Builder(context)
                            val currentSessionView =
                                layoutInflater.inflate(R.layout.current_dumbbell_view, null)
                            builder.setView(currentSessionView)
                            currentSessionView.text_title_currentDB.text =
                                getString(R.string.dumbbell, holder.weight.text)
                            currentSessionView.text_session_time.base = baseTime
                            currentSessionView.text_session_time.start()
                            val dialog = builder.create()
                            dialog.show()

                            currentSessionView.button_return_cur.setOnClickListener { dialog.cancel() }

                            currentSessionView.button_end_workout.setOnClickListener {
                                ref.child("demo2/weights/${request.weight.toInt()}/activeRequests/${request.id}")
                                    .removeValue()
                                requireActivity().toast("demo2/weights/${request.weight.toInt()}/activeRequests/${request.id}")

                                //set entry in request hashmap to collecting
                                val now3 = LocalDateTime.now(ZoneOffset.UTC)
                                val unixSeconds = now3.atZone(ZoneOffset.UTC)?.toEpochSecond()
                                val unixMilli =
                                    now3.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
                                        .toString()

                                //update id and type in requests to new request information
                                val currentDumbbellReq: Request = requests[request.id]!!.copy()
                                currentDumbbellReq.id = unixMilli
                                currentDumbbellReq.type = "collecting"
                                requests.remove(request.id) //remove the request with the delivering id
                                requests[currentDumbbellReq.id] = currentDumbbellReq
                                notifyDataSetChanged()
                                //request.id = unixMilli

                                //send request to firebase
                                val newRequest =
                                    ref.child("demo2/requests").child(currentDumbbellReq.id)
                                newRequest.child("bench").setValue(request.bench)
                                newRequest.child("time").setValue(unixSeconds)
                                newRequest.child("type").setValue("collecting")
                                newRequest.child("weight").setValue(request.weight)

                                notifyDataSetChanged()
                                dialog.cancel()
                            }
                        }
                    }
                }

                if (position == itemCount - 1) {
                    //give final holder a height of zero to make invisible?
                    holder.background.layoutParams =
                        LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, 0)
                }
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val weight: TextView = view.textView_current_dumbbell_weight
            var id: String = ""
            val description: TextView = view.textView_current_dumbbell_status
            val emptyText: TextView = view.text_no_current_dumbbells
            val button: Button = view.button_cancel_curr
            val divider: View = view.divider_current
            var timer: Chronometer = view.text_dumbbell_timer
            var background = view
        }


    }

//    inner class QueuedDumbbellAdapter : RecyclerView.Adapter<QueuedDumbbellAdapter.ViewHolder>() {
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
//            ViewHolder(
//                LayoutInflater.from(this@CurrentOrdersFragment.requireContext())
//                    .inflate(R.layout.item_queued_dumbbell, parent, false)
//            )
//
//        override fun getItemCount(): Int = 2 //change
//
//        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//            if (itemCount == 1){
//                holder.weight.visibility = View.INVISIBLE
//                holder.current_description.visibility = View.INVISIBLE
//                holder.button.visibility = View.INVISIBLE
//                holder.divider.visibility = View.INVISIBLE
//                holder.emptyText.visibility = View.VISIBLE
//                holder.background.setBackgroundColor(Color.rgb(242,242,242))
//            }
//            if (itemCount > 1) {
//                holder.background.setBackgroundColor(Color.WHITE)
//                if (position < itemCount -1) {
//                    holder.emptyText.visibility = View.INVISIBLE
//
//                    //todo change weight value
//                    //todo change queue value
//                    holder.button.setOnClickListener {
//                        val builder = AlertDialog.Builder(context)
//                        val cancelView = layoutInflater.inflate(R.layout.queued_dumbbell_cancellation_view, null)
//                        builder.setView(cancelView)
//
//                        val dialog = builder.create()
//                        dialog.show()
//
//                        cancelView.button_return_que.setOnClickListener {
//                            dialog.cancel()
//                        }
//                        cancelView.button_confirm_que.setOnClickListener{
//                            //TODO cancel queued request
//                            dialog.cancel()
//                        }
//                    }
//                }
//                if (position == itemCount -1){
//                    holder.background.layoutParams = LinearLayout.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, 0)
//
//                }
//            }
//        }
//
//        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//            val weight : TextView = view.textView_queued_dumbbell_weight
//            var id : String = ""
//            val current_description : TextView = view.textView_queued_dumbbell_status
//            val emptyText = view.text_no_queued_dumbbells
//            val button : Button = view.button_cancel_que
//            val divider = view.divider_queued
//            val background = view
//        }
//    }

}