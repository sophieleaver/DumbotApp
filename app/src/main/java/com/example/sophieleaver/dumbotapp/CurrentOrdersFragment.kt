package com.example.sophieleaver.dumbotapp

import android.app.ActionBar
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.cancel_collection_view.view.*
import kotlinx.android.synthetic.main.cancel_delivery_view.view.*
import kotlinx.android.synthetic.main.current_dumbbell_view.view.*
import kotlinx.android.synthetic.main.fragment_current_orders.view.*
import kotlinx.android.synthetic.main.item_current_dumbbell.view.*
import kotlinx.android.synthetic.main.item_queued_dumbbell.view.*
import android.view.ViewGroup.LayoutParams.FILL_PARENT
import android.widget.LinearLayout
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_timer.view.*
import kotlinx.android.synthetic.main.reset_warning_view.view.*


class CurrentOrdersFragment : Fragment() {
    private lateinit var currentDBRecyclerView : RecyclerView
    private lateinit var queuedDBRecyclerView : RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_current_orders, container, false).apply {
            currentDBRecyclerView = recyclerView_current_dumbbells.apply {
                layoutManager = LinearLayoutManager(this@CurrentOrdersFragment.requireContext())
                adapter = CurrentDumbbellAdapter()
            }
            queuedDBRecyclerView = recyclerView_queued_dumbbells.apply {
                layoutManager = LinearLayoutManager(this@CurrentOrdersFragment.requireContext())
                adapter = CurrentDumbbellAdapter()
            }
        }

        val cancelButton : Button = view.findViewById(R.id.button_reset_workout_session)
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

            warningView.button_confirm_reset.setOnClickListener{
                //TODO cancel all requests
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

    private fun setUpRecyclerViews(){
        currentDBRecyclerView.layoutManager = LinearLayoutManager(this.requireContext())
        currentDBRecyclerView.adapter = CurrentDumbbellAdapter()

        queuedDBRecyclerView.layoutManager = LinearLayoutManager(this.requireContext())
        queuedDBRecyclerView.adapter = QueuedDumbbellAdapter()
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    companion object {
        @JvmStatic
        fun newInstance() = CurrentOrdersFragment()
    }

    inner class CurrentDumbbellAdapter : RecyclerView.Adapter<CurrentDumbbellAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(this@CurrentOrdersFragment.requireContext())
                    .inflate(R.layout.item_current_dumbbell, parent, false)
            )

        override fun getItemCount(): Int = 3 + 1 //change

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val type = "current"
            if (itemCount == 1){
                holder.weight.visibility = View.INVISIBLE
                holder.description.visibility = View.INVISIBLE
                holder.button.visibility = View.INVISIBLE
                holder.divider.visibility = View.INVISIBLE
                holder.emptyText.visibility = View.VISIBLE
                holder.background.setBackgroundColor(Color.rgb(242,242,242))
            }
            if ( itemCount > 1) {
                if (position < itemCount) {
                    holder.weight.visibility = View.VISIBLE
                    holder.description.visibility = View.VISIBLE
                    holder.button.visibility = View.VISIBLE
                    holder.divider.visibility = View.VISIBLE
                    holder.emptyText.visibility = View.INVISIBLE
                    holder.background.setBackgroundColor(Color.WHITE)

                    if (type == "delivery") {
                        holder.description.setText("Dumbbell is being delivered now")
                        holder.button.setText("Cancel")
                        holder.button.setOnClickListener {
                            val builder = AlertDialog.Builder(context)
                            val cancelDeliveryView =
                                layoutInflater.inflate(R.layout.cancel_delivery_view, null)
                            builder.setView(cancelDeliveryView)

                            cancelDeliveryView.text_delivery_cancel.setText("Are you sure you would like to cancel collection of the ${holder.weight.text} dumbbell?")

                            val dialog: AlertDialog = builder.create()
                            dialog.show()

                            cancelDeliveryView.button_return_del.setOnClickListener {
                                dialog.cancel()
                            }
                            cancelDeliveryView.button_cancel_delivery.setOnClickListener {
                                //Todo cancel the request
                                dialog.cancel()
                            }
//
                        }
                    }
                    if (type == "collection") {
                        holder.description.setText("Dumbbell is being collected now")
                        holder.button.setText("Cancel")

                        holder.button.setOnClickListener {
                            val builder = AlertDialog.Builder(context)
                            val cancelCollectionView =
                                layoutInflater.inflate(R.layout.cancel_collection_view, null)
                            builder.setView(cancelCollectionView)

                            cancelCollectionView.text_collection_cancel.setText("Are you sure you would like to cancel delivery of the ${holder.weight.text} dumbbell?")

                            val dialog: AlertDialog = builder.create()
                            dialog.show()

                            cancelCollectionView.button_return_col.setOnClickListener {
                                dialog.cancel()
                            }
                            cancelCollectionView.button_cancel_collection.setOnClickListener {
                                //Todo cancel the request
                                dialog.cancel()
                            }
//
                        }
                    }
                    if (type == "current") {
                        holder.description.setText("Your current dumbbell. Press the button below to return.")
                        //holder.description.textSize("16sp")
                        holder.button.setText("More Info")

                        holder.button.setOnClickListener {
                            val builder = AlertDialog.Builder(context)
                            val currentSessionView =
                                layoutInflater.inflate(R.layout.current_dumbbell_view, null)
                            builder.setView(currentSessionView)
                            currentSessionView.text_title_currentDB.text = "${holder.weight.text} DUMBBELL"
                            //todo set timer -> set it as time from request??
                            //SystemClock.elapsedRealtime().
                            //currentSessionView.text_timer.text = SystemClock.elapsedRealtime().toString()
                            val dialog = builder.create()
                            dialog.show()

                            currentSessionView.button_return_cur.setOnClickListener {
                                dialog.cancel()
                            }

                            currentSessionView.button_end_workout.setOnClickListener {
                                //TODO send return request
                                dialog.cancel()
                            }
                        }
                    }
                }
                if (position == itemCount - 1) {
                    //give final holder a height of zero to make invisible
                    holder.background.layoutParams = LinearLayout.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, 0)
                }
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val weight : TextView = view.textView_current_dumbbell_weight
            val description : TextView = view.textView_current_dumbbell_status
            val emptyText = view.text_no_current_dumbbells
            val button : Button = view.button_current_dumbbell
            val divider = view.divider_current
            var background = view
        }
    }

    inner class QueuedDumbbellAdapter : RecyclerView.Adapter<QueuedDumbbellAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(this@CurrentOrdersFragment.requireContext())
                    .inflate(R.layout.item_queued_dumbbell, parent, false)
            )

        override fun getItemCount(): Int = 1 //change

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (itemCount == 1){
                holder.weight.visibility = View.INVISIBLE
                holder.description.visibility = View.INVISIBLE
                holder.button.visibility = View.INVISIBLE
                holder.divider.visibility = View.INVISIBLE
                holder.emptyText.visibility = View.VISIBLE
                holder.background.setBackgroundColor(Color.rgb(242,242,242))
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val weight : TextView = view.textView_queued_dumbbell_weight
            val description : TextView = view.textView_queued_dumbbell_status
            val emptyText = view.text_no_queued_dumbbells
            val button : Button = view.button_cancel_queued_dumbbell
            val divider = view.divider_queued
            val background = view
        }
    }
}
