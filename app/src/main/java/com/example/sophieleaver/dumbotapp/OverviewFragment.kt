package com.example.sophieleaver.dumbotapp

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_overview.view.*
import kotlinx.android.synthetic.main.item_overview_dumbot.view.*


/**
 * A fragment that acts as an overview for any dumbots connected to the users gym.
 * A MANAGER fragment that lists all dumbots and a summary of their current state.
 *
 */
class OverviewFragment : Fragment() {
    private var overviewDumbotRecyclerView: RecyclerView? = null
    val database = FirebaseDatabase.getInstance()
    var ref = database.reference.child("demo2")

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

                if (position == 0){ //only changing dumbot #1 status

                    val alertListener = object : ValueEventListener {
                        override fun onDataChange(snap : DataSnapshot) {
                            //if there is an alert then set the dumbots status to error
                            if (snap.value!!.equals("True")){
                                setErrorStatus(holder, 1)
                            }
                            //else get information on the current request
                            else {
                                checkRequests(holder, 1)
                            }
                        }
                        override fun onCancelled(p0: DatabaseError) {/** do nothing **/}
                    }

                    ref.child("alert").addValueEventListener(alertListener) //listen for alert changes
                }

                if (position == 1){ //Dummy robot entry to show what other added dumbots would look like
                    setIdleRequestStatus(holder, 2)
                }
            }
        }

        fun checkRequests(holder : ViewHolder, dumbotNo: Int){
            val reqListener = object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {/** do nothing **/}

                override fun onDataChange(snap: DataSnapshot) {
                    val id = snap.child("id").value.toString()
                    //if the id is null then there is no current request -> the dumbot is idle
                    if (id.equals("null")){
                        setIdleRequestStatus(holder, 1)
                    }
                    //else, set the status to information about the request
                    else {
                        val requestType = snap.child("type").value.toString() // delivering or collecting
                        val weight = snap.child("weight").value.toString()
                        val bench = snap.child("bench").value.toString()

                        setActiveRequestStatus(holder, dumbotNo, id, requestType, weight, bench)
                    }
                }
            }

            ref.child("cur_request").addValueEventListener(reqListener)
        }

        fun setErrorStatus(holder: ViewHolder, dumbotNo: Int) {
            holder.apply {
                dumbotStatus.setText("ERROR - OBSTRUCTION")
                //set button to red with ! logo
                overviewButton.text = "!"
                overviewButton.setBackgroundResource(R.drawable.red_circle)

                //button can reset the dumbot
                overviewButton.setOnClickListener {
                    val builder = AlertDialog.Builder(itemView.context)
                    builder.setTitle("Dumbot #$dumbotNo")
                    builder.setMessage("Dumbot #$dumbotNo is currently obstructed from movement. Please remove the obstruction and click the reset button.")
                    builder.setPositiveButton("RESET") { dialog, which ->
                        ref.child("alert").setValue("False")
                    }
                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }
            }
        }


        fun setActiveRequestStatus(holder : ViewHolder, dumbotNo: Int, requestID : String, requestType : String, weight : String, bench : String){
            holder.apply {
                //set button to ? and orange
                overviewButton.text = "?"
                overviewButton.setBackgroundResource(R.drawable.orange_circle)

                //set text to about the request
                dumbotStatus.text = "${requestType.toUpperCase()} DUMBBELL #$requestID"

                //set dialog to more information about request
                overviewButton.setOnClickListener {
                    val builder = AlertDialog.Builder(itemView.context)
                    builder.setTitle("Dumbot #$dumbotNo - Request $requestID")
                    builder.setMessage("Dumbot #$dumbotNo is currently completing request:\n${requestType.capitalize()} ${weight}kg weight (Bench $bench)")
                    builder.setNeutralButton("OKAY") { dialog, _ -> dialog.cancel()}
                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }
            }
        }

        fun setIdleRequestStatus(holder : ViewHolder, dumbotNo: Int){
            holder.apply {
                dumbotStatus.text = "IDLE"
                overviewButton.text = "?"
                overviewButton.setBackgroundResource(R.drawable.orange_circle)

                overviewButton.setOnClickListener {
                    val builder = AlertDialog.Builder(itemView.context)
                    builder.setTitle("Dumbot #$dumbotNo")
                    builder.setMessage("Dumbot #$dumbotNo is currently idle and awaiting a request.")
                    builder.setNeutralButton("OKAY") { dialog, which -> }
                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var dumbot: TextView = view.text_dumbot_number
            val dumbotStatus: TextView = view.text_dumbot_information
            val overviewButton: Button = view.button_dumbot_information
        }
    }
}


