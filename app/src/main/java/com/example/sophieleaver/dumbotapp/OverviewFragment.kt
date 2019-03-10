package com.example.sophieleaver.dumbotapp

import android.app.AlertDialog
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



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(view){ overviewDumbotRecyclerView = recycler_dumbots_overview }
        overviewDumbotRecyclerView!!.layoutManager =
            LinearLayoutManager(this@OverviewFragment.requireContext())
        overviewDumbotRecyclerView!!.adapter = DumbotOverviewAdapter()
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
                dumbot.text = "#${position + 1}"

                if (position == 0){ //only changing dumbot #1 status

                    val alertListener = object : ValueEventListener {
                        override fun onDataChange(snap : DataSnapshot) {
                            //if there is an alert then set the dumbots status to error
                            if (snap.value!! == "True") {
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
                    val id = snap.child("status").value.toString()
                    //if the id is null then there is no current request -> the dumbot is idle
                    if (id.equals("False")){
                        setIdleRequestStatus(holder, 1)
                    }
                    //else, set the status to information about the request
                    else {
                        val requestType = snap.child("type").value.toString() // delivering or collecting
                        val weight = snap.child("weight").value.toString()
                        var bench = ""
                        if (requestType.equals("collecting")){
                            bench = snap.child("s_point").value.toString()
                            bench = convertIDtoBench(bench)
                        } else if (requestType.equals("delivering")){
                            bench = snap.child("e_point").value.toString()
                            bench = convertIDtoBench(bench)
                        }

                        setActiveRequestStatus(holder, dumbotNo, requestType, weight, bench)
                    }
                }
            }

            ref.child("cur_request").addValueEventListener(reqListener)
        }

        fun setErrorStatus(holder: ViewHolder, dumbotNo: Int) {
            holder.apply {
                dumbotStatus.text = "EMERGENCY STOP"
                //set button to red with ! logo
                overviewButton.text = "!"
                overviewButton.setBackgroundResource(R.drawable.red_circle)

                //button can reset the dumbot
                overviewButton.setOnClickListener {
                    val builder = AlertDialog.Builder(itemView.context)
                    builder.setTitle("DumBot #$dumbotNo")
                    builder.setMessage("DumBot #$dumbotNo has come to emergency stop due to obstruction or manual override. Please click the button below to reset the Dumbot.")
                    builder.setPositiveButton("RESET") { _, _ ->
                        ref.child("alert").setValue("False")
                        checkRequests(holder, 1)
                    }
                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }
            }
        }


        fun setActiveRequestStatus(holder : ViewHolder, dumbotNo: Int, requestType : String, weight : String, bench : String){
            holder.apply {
                //set button to ? and orange
                overviewButton.text = "?"
                overviewButton.setBackgroundResource(R.drawable.orange_circle)

                //set text to about the request
                dumbotStatus.text = "${requestType.toUpperCase()} DUMBBELL"

                //set dialog to more information about request
                overviewButton.setOnClickListener {
                    val builder = AlertDialog.Builder(itemView.context)
                    builder.setTitle("Dumbot #$dumbotNo")
                    builder.setMessage("Dumbot #$dumbotNo is currently $requestType a ${weight}kg weight for Bench $bench")
                    builder.setNeutralButton("OKAY") { dialog, _ -> dialog.cancel()}
                    builder.setPositiveButton("EMERGENCY STOP") { _, _ ->
                        setErrorStatus(holder, 1)
                        ref.child("alert").setValue("True")
                    }
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

        fun convertIDtoBench( id : String) : String {
            var bench = "1"
            when (id){
                "B10" -> bench = "2"
                "B13" -> bench = "3"
                "B9" -> bench = "4"
                "B12" -> bench = "5"
                "B15" -> bench = "6"
            }
            return bench
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var dumbot: TextView = view.text_dumbot_number
            val dumbotStatus: TextView = view.text_total_stock
            val overviewButton: Button = view.button_dumbot_information
        }
    }
}


