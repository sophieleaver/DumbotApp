package com.example.sophieleaver.dumbotapp

import android.app.ActionBar
import android.app.AlertDialog
import android.graphics.Color
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
import kotlinx.android.synthetic.main.fragment_current_orders.*
import kotlinx.android.synthetic.main.fragment_current_orders.view.*
import kotlinx.android.synthetic.main.item_current_dumbbell.view.*
import kotlinx.android.synthetic.main.reset_warning_view.view.*
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * TODO loophole needs to be fixed - has a weight, returns it, orders two more, cancels return
 * TODO - add way to access timer & stopwatch (eventually)
 *
 */

class CurrentOrdersFragment : Fragment() {
    private lateinit var currentDBRecyclerView: RecyclerView
    private lateinit var queuedDBRecyclerView: RecyclerView

    val ref = FirebaseDatabase.getInstance().reference

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_current_orders, container, false).apply {
            currentDBRecyclerView = recyclerView_current_dumbbells.apply {
                layoutManager = LinearLayoutManager(this@CurrentOrdersFragment.requireContext())
                adapter = CurrentDumbbellAdapter()
            }
//            queuedDBRecyclerView = recyclerView_queued_dumbbells.apply {
//                layoutManager = LinearLayoutManager(this@CurrentOrdersFragment.requireContext())
//                adapter = QueuedDumbbellAdapter()
//            }
//            adapter.notifyDataSetChanged()
        }

        view.fab_timer.setOnClickListener {
            (activity as MainActivity).openFragment(TimerFragment.newInstance())
        }
        view.btn_debug.setOnClickListener {
            requests.forEach {
                it.value.type = if (it.value.type == "delivering") "current" else "delivering"
            }
            currentDBRecyclerView.adapter?.notifyDataSetChanged()
        }

        val cancelButton: Button = view.findViewById(R.id.button_reset_workout_session)
        cancelButton.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            val warningView = layoutInflater.inflate(R.layout.reset_warning_view, null)
            builder.setView(warningView)

            val dialog = builder.create()
            dialog.show()

            warningView.button_cancel_warning.setOnClickListener {
                dialog.cancel()
                Toast.makeText(context, "Workout reset has been cancelled", Toast.LENGTH_SHORT).show()
            }

            warningView.button_confirm_reset.setOnClickListener {
                //TODO cancel all requests

                for (req in requests.values) {
                    if (req.type == "delivering") {
                        requests.remove(req.id)
                        currentDBRecyclerView.adapter?.notifyDataSetChanged()
                    }
                    if (req.type == "current") {
                        req.type = "collecting"

                        val now = LocalDateTime.now(ZoneOffset.UTC)
                        val unixSeconds = now.atZone(ZoneOffset.UTC)?.toEpochSecond()

                        //send request to firebase
                        val newRequest = ref.child("demo2").child("requests").child(req.id)
                        newRequest.child("bench").setValue(req.bench)
                        newRequest.child("time").setValue(unixSeconds)
                        newRequest.child("type").setValue("collecting")
                        newRequest.child("weight").setValue(req.weight)

                        currentDBRecyclerView.adapter?.notifyDataSetChanged()
                    }
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

    private fun setUpRecyclerViews() {
        currentDBRecyclerView.layoutManager = LinearLayoutManager(this.requireContext())
        currentDBRecyclerView.adapter = CurrentDumbbellAdapter()

//        queuedDBRecyclerView.layoutManager = LinearLayoutManager(this.requireContext())
//        queuedDBRecyclerView.adapter = QueuedDumbbellAdapter()
    }


    companion object {
        @JvmStatic
        fun newInstance() = CurrentOrdersFragment()
    }

    //    dont need two different adapters, two different views and two different dialogs, can just change text on same view
    inner class CurrentDumbbellAdapter : RecyclerView.Adapter<CurrentDumbbellAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
                ViewHolder(
                        LayoutInflater.from(this@CurrentOrdersFragment.requireContext())
                                .inflate(R.layout.item_current_dumbbell, parent, false)
                )

        override fun getItemCount(): Int = requests.size + 1 //change

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (itemCount == 1) {
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
                    val now = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)!!.toInstant()!!.toEpochMilli()
                    val baseTime = SystemClock.elapsedRealtime() - (now - (request.time * 1000))

                    val type = request.type
                    holder.id = request.id
                    holder.weight.text = getString(R.string.weight, request.weight.toInt())

                    holder.emptyText.visibility = View.INVISIBLE
                    holder.background.setBackgroundColor(Color.WHITE)

                    if (type == "delivering") {
                        holder.description.text = getString(R.string.dumbbell_being_delivered)
                        holder.button.text = getString(R.string.cancel)
                        holder.button.setOnClickListener {
                            val builder = AlertDialog.Builder(context)
                            val cancelDeliveryView =
                                    layoutInflater.inflate(R.layout.cancel_delivery_view, null)
                            builder.setView(cancelDeliveryView)

                            cancelDeliveryView.text_delivery_cancel.text =
                                    getString(R.string.cancel_dumbbell, "delivery", holder.weight.text)

                            val dialog: AlertDialog = builder.create()
                            dialog.show()

                            cancelDeliveryView.button_return_del.setOnClickListener {
                                dialog.cancel()
                            }
                            cancelDeliveryView.button_confirm_del.setOnClickListener {
                                val now = LocalDateTime.now(ZoneOffset.UTC)
                                val unixSeconds = now.atZone(ZoneOffset.UTC)?.toEpochSecond()

                                ref.child("demo2").child("cancelledRequests").child(request.id)
                                        .setValue(unixSeconds)

                                requests.remove(holder.id)
                                recyclerView_current_dumbbells.removeViewAt(position)

                                dialog.cancel()
                            }
//
                        }
                    }
                    if (type == "collecting") {
                        holder.description.text = getString(R.string.dumbbell_being_collected)
                        holder.button.text = getString(R.string.cancel)
                        holder.button.setOnClickListener {
                            val builder = AlertDialog.Builder(context)
                            val cancelCollectionView =
                                    layoutInflater.inflate(R.layout.cancel_collection_view, null)
                            builder.setView(cancelCollectionView)

                            cancelCollectionView.text_collection_cancel.text =
                                    getString(R.string.cancel_dumbbell, "collection", holder.weight.text)

                            val dialog: AlertDialog = builder.create()
                            dialog.show()

                            cancelCollectionView.button_return_col.setOnClickListener {
                                dialog.cancel()
                            }
                            cancelCollectionView.button_confirm_col.setOnClickListener {
                                val now = LocalDateTime.now(ZoneOffset.UTC)
                                val unixSeconds = now.atZone(ZoneOffset.UTC)?.toEpochSecond()

                                ref.child("demo2").child("cancelledRequests").child(request.id)
                                        .setValue(unixSeconds)

                                requests[holder.id]!!.type = "current"
//                                todo - why?
                                onBindViewHolder(holder, position)
                                dialog.cancel()
                            }
//
                        }

                    }
                    if (type == "current") {
                        holder.description.text = getString(R.string.current_dumbbell)
                        holder.button.text = getString(R.string.more_info)
                        holder.timer.base = baseTime
                        holder.timer.start()
                        holder.button.setOnClickListener {
                            val builder = AlertDialog.Builder(context)
                            val currentSessionView =
                                    layoutInflater.inflate(R.layout.current_dumbbell_view, null)
                            builder.setView(currentSessionView)
                            currentSessionView.text_title_currentDB.text = getString(R.string.dumbbell, holder.weight.text)
                            //todo set timer -> set it as time from request??
                            currentSessionView.text_session_time.base = baseTime
                            currentSessionView.text_session_time.start()
                            val dialog = builder.create()
                            dialog.show()

                            currentSessionView.button_return_cur.setOnClickListener {
                                dialog.cancel()
                            }

                            currentSessionView.button_end_workout.setOnClickListener {
                                //set entry in request hashmap to collecting
                                requests[holder.id]!!.type = "collecting"

                                val now = LocalDateTime.now(ZoneOffset.UTC)
                                val unixSeconds = now.atZone(ZoneOffset.UTC)?.toEpochSecond()

                                //send request to firebase
                                val newRequest =
                                        ref.child("demo2").child("requests").child(request.id)
                                newRequest.child("bench").setValue(request.bench)
                                newRequest.child("time").setValue(unixSeconds)
                                newRequest.child("type").setValue("collecting")
                                newRequest.child("weight").setValue(request.weight)

                                onBindViewHolder(holder, position)
                                dialog.cancel()
                            }
                        }
                    }
                }
                if (position == itemCount - 1) {
//                    todo - why?
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
//                holder.description.visibility = View.INVISIBLE
//                holder.button.visibility = View.INVISIBLE
//                holder.divider.visibility = View.INVISIBLE
//                holder.emptyText.visibility = View.VISIBLE
//                holder.background.setBackgroundColor(Color.rgb(242,242,242))
//            }
//            if (itemCount > 1){
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
//            val description : TextView = view.textView_queued_dumbbell_status
//            val emptyText = view.text_no_queued_dumbbells
//            val button : Button = view.button_cancel_que
//            val divider = view.divider_queued
//            val background = view
//        }
//    }
}
