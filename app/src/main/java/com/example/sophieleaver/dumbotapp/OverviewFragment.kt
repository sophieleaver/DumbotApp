package com.example.sophieleaver.dumbotapp

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_overview.view.*
import kotlinx.android.synthetic.main.item_order_dumbbell.view.*
import kotlinx.android.synthetic.main.item_overview_dumbot.view.*
import org.jetbrains.anko.toast
import kotlin.random.Random


/**
 * A fragment that acts as an overview for any dumbots connected to the users gym.
 * A MANAGER fragment that lists all dumbots and a summary of their current state.
 *
 */
class OverviewFragment : Fragment() {
    private var overviewDumbotRecyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_overview, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(view){
            overviewDumbotRecyclerView = recycler_dumbots_overview
        }

        overviewDumbotRecyclerView!!.layoutManager =
                LinearLayoutManager(this@OverviewFragment.requireContext())
        overviewDumbotRecyclerView!!.adapter = DumbotOverviewAdapter()
    }



    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    companion object {
        @JvmStatic
        fun newInstance() = OverviewFragment()
    }


    /**
     * Custom adapter for each dumbot to be placed in recycler view
     */
    inner class DumbotOverviewAdapter : RecyclerView.Adapter<DumbotOverviewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(this@OverviewFragment.requireContext())
                    .inflate(R.layout.item_overview_dumbot, parent, false)
            )

        override fun getItemCount(): Int = 2

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.apply {
                dumbot.setText("#${position+1}")
                if (position == 0){
                    overviewButton.setOnClickListener {
                        //if request queue is empty and alert is false -> idle
                        //if completing request then

                        val builder = AlertDialog.Builder(itemView.context)
                        builder.setTitle("Dumbot #1")
                        builder.setMessage("Insert text.")
                        builder.setNeutralButton("OKAY") {dialog, which ->  }
                        val dialog: AlertDialog = builder.create()
                        dialog.show()
                    }
                }
                if (position == 1){
                    dumbotStatus.setText("IDLE")
                    overviewButton.setOnClickListener {
                        val builder = AlertDialog.Builder(itemView.context)
                        builder.setTitle("Dumbot #2")
                        builder.setMessage("Dumbot #2 is currently idle and awaiting a request to complete.")
                        builder.setNeutralButton("OKAY") {dialog, which ->  }
                        val dialog: AlertDialog = builder.create()
                        dialog.show()
                    }
                }
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            var dumbot: TextView = view.text_dumbot_number // TODO dumbot number needs to change
            val dumbotStatus: TextView = view.text_dumbot_information
            val overviewButton: Button = view.button_dumbot_information
        }
    }
}


