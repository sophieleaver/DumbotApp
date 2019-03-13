package com.example.sophieleaver.dumbotapp

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.ValueDependentColor
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.DataPoint
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset


//TODO create a login
class AnalyticsFragment : Fragment() {

    //stores the datapoints for the bargraphs
    private var mSeries1: BarGraphSeries<DataPoint>? = null
    private var mSeries2: BarGraphSeries<DataPoint>? = null
    private var mSeries3: BarGraphSeries<DataPoint>? = null
    private var mSeries4: BarGraphSeries<DataPoint>? = null
    private var mSeries5: BarGraphSeries<DataPoint>? = null
    private var mSeries6: BarGraphSeries<DataPoint>? = null
    private var mSeries7: BarGraphSeries<DataPoint>? = null
    //key: weight maps to usage and place in dataPoints
    private var weightUsage: HashMap<Int, Int> = HashMap()

    private lateinit var graph1: GraphView
    private lateinit var graph2: GraphView

    //variable if we want to make hourly updates on dumbbell and station usages
//    private var time = 6
    private val fragTag = "AnalyticsFragment"

    //firebase variables
    private val ref = FirebaseDatabase.getInstance().reference
    private val requestReference = ref.child("demo2").child("log")


//    data class Request(var bench: String = "", var time: Long = 0, var type: String = "", var weight: String = "")


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)

        //the two bargraphs displayed on the screen
        graph1 = view.findViewById(R.id.graph1)
        graph2 = view.findViewById(R.id.graph2)

        //create spinner to hold selection box for period of time for graph
        val dropdown: Spinner = view.findViewById(R.id.spinner1)

        dropdown.apply {
            adapter = ArrayAdapter(this@AnalyticsFragment.requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    arrayOf("Today", "Last Week", "Last Month", "Last Year"))

            // Set an on item selected listener for spinner object
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

                    //show last week analytics
                    when (parent.getItemAtPosition(position).toString()) {

                        "Today" -> showDailyStats()
                        "Last Week" -> {


                            //---------------------weekly dumbbell counts-----------------------


                            graph1.removeAllSeries()
                            if (mSeries1 == null) {
                                mSeries1 = BarGraphSeries(arrayOf(

                                        //should be initialised to zero every day
                                        //should be imported from firestore
                                        //updated everytime a request is issued in the requests page
                                        DataPoint(5.0, 12.0),
                                        DataPoint(10.0, 14.0),
                                        DataPoint(15.0, 17.0),
                                        DataPoint(20.0, 11.0),
                                        DataPoint(25.0, 12.0)))
                            }

                            graph1.addSeries(mSeries1)
                            graph1.title = "Request counts for each dumbbell last week"

                            // styling
                            mSeries1!!.valueDependentColor = ValueDependentColor { data ->
                                Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                            }

                            mSeries1!!.spacing = 20

                            // draw values on top
                            mSeries1!!.isDrawValuesOnTop = true
                            mSeries1!!.valuesOnTopColor = Color.RED
                            //series.setValuesOnTopSize(50);


                            // ----------------weekly station counts------------------------


                            graph2.removeAllSeries()
                            if (mSeries2 == null) {
                                mSeries2 = BarGraphSeries(arrayOf(

                                        //should be initialised to zero every day
                                        //should be imported from firestore
                                        //updated everytime a request is issued in the requests page
                                        DataPoint(1.0, 12.0),
                                        DataPoint(2.0, 14.0),
                                        DataPoint(3.0, 17.0),
                                        DataPoint(4.0, 11.0),
                                        DataPoint(5.0, 12.0)))
                            }

                            graph2.addSeries(mSeries2)
                            graph2.title = "Request counts for each workout station last week"

                            // styling
                            mSeries2!!.valueDependentColor = ValueDependentColor { data ->
                                Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                            }

                            mSeries2!!.spacing = 20

                            // draw values on top
                            mSeries2!!.isDrawValuesOnTop = true
                            mSeries2!!.valuesOnTopColor = Color.RED
                            //series.setValuesOnTopSize(50);


                        }
                        //show last months analytics
                        "Last Month" -> {


                            //-----------------monthly dumbbell counts---------------------------


                            graph1.removeAllSeries()
                            if (mSeries3 == null) {
                                mSeries3 = BarGraphSeries(arrayOf(

                                        //should be initialised to zero every month
                                        //should be imported from firestore
                                        //updated everytime a request is issued in the requests page
                                        DataPoint(5.0, 120.0),
                                        DataPoint(10.0, 140.0),
                                        DataPoint(15.0, 170.0),
                                        DataPoint(20.0, 110.0),
                                        DataPoint(25.0, 120.0)))
                            }
                            graph1.addSeries(mSeries3)
                            graph1.title = "Request counts for each dumbbell last month"

                            // styling
                            mSeries3!!.valueDependentColor = ValueDependentColor { data ->
                                Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                            }

                            mSeries3!!.spacing = 20

                            // draw values on top
                            mSeries3!!.isDrawValuesOnTop = true
                            mSeries3!!.valuesOnTopColor = Color.RED
                            //series.setValuesOnTopSize(50);


                            //-----------------monthly station counts---------------------------


                            graph2.removeAllSeries()
                            if (mSeries4 == null) {
                                mSeries4 = BarGraphSeries(arrayOf(

                                        //should be initialised to zero every month
                                        //should be imported from firestore
                                        //updated everytime a request is issued in the requests page
                                        DataPoint(1.0, 120.0),
                                        DataPoint(2.0, 140.0),
                                        DataPoint(3.0, 170.0),
                                        DataPoint(4.0, 110.0),
                                        DataPoint(5.0, 120.0)))
                            }
                            graph2.addSeries(mSeries4)
                            graph2.title = "Request counts for each workout station last month"

                            // styling
                            mSeries4!!.valueDependentColor = ValueDependentColor { data ->
                                Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                            }

                            mSeries4!!.spacing = 20

                            // draw values on top
                            mSeries4!!.isDrawValuesOnTop = true
                            mSeries4!!.valuesOnTopColor = Color.RED
                            //series.setValuesOnTopSize(50);

                        }
                        //show last years analytics
                        "Last Year" -> {


                            //-----------------yearly dumbbell counts---------------------------


                            graph1.removeAllSeries()
                            if (mSeries5 == null) {
                                mSeries5 = BarGraphSeries(arrayOf(

                                        //should be initialised to zero every year
                                        //should be imported from firestore
                                        //updated everytime a request is issued in the requests page
                                        DataPoint(5.0, 1200.0),
                                        DataPoint(10.0, 1400.0),
                                        DataPoint(15.0, 1700.0),
                                        DataPoint(20.0, 1100.0),
                                        DataPoint(25.0, 1200.0)))
                            }
                            graph1.addSeries(mSeries5)
                            graph1.title = "Request counts for each dumbbell last year"

                            // styling
                            mSeries5!!.valueDependentColor = ValueDependentColor { data ->
                                Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                            }

                            mSeries5!!.spacing = 20

                            // draw values on top
                            mSeries5!!.isDrawValuesOnTop = true
                            mSeries5!!.valuesOnTopColor = Color.RED
                            //series.setValuesOnTopSize(50);


                            //-------------------yearly station counts-----------------------------


                            graph2.removeAllSeries()
                            if (mSeries6 == null) {
                                mSeries6 = BarGraphSeries(arrayOf(

                                        //should be initialised to zero every year
                                        //should be imported from firestore
                                        //updated everytime a request is issued in the requests page
                                        DataPoint(1.0, 1200.0),
                                        DataPoint(2.0, 1400.0),
                                        DataPoint(3.0, 1700.0),
                                        DataPoint(4.0, 1100.0),
                                        DataPoint(5.0, 1200.0)))
                            }

                            graph2.addSeries(mSeries6)
                            graph2.title = "Request counts for each workout station last year"

                            // styling
                            mSeries6!!.valueDependentColor = ValueDependentColor { data ->
                                Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                            }

                            mSeries6!!.spacing = 20

                            // draw values on top
                            mSeries6!!.isDrawValuesOnTop = true
                            mSeries6!!.valuesOnTopColor = Color.RED
                            //series.setValuesOnTopSize(50);
                        }
                    }

                }


                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Another interface callback

                }
            }
        }


        return view
    }

    private fun showDailyStats() {

        //---------------------dumbbell counts for that day-----------------------

        // reset current graph
        graph1.removeAllSeries()
        if (mSeries7 == null) {
            mSeries7 = BarGraphSeries(arrayOf(DataPoint(0.0, 0.0)))
        }
        graph1.addSeries(mSeries7)

        //get counts for that day from firebase
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val nowDate = now.toLocalDate()


        //listen for changes in the database
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(fragTag, "onChildAdded:" + dataSnapshot.key!!)


                val unixSeconds: Long = dataSnapshot.getValue(Request::class.java)!!.time
                val date = java.util.Date(unixSeconds) // * 1000L)
                val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                if (dataSnapshot.getValue(Request::class.java)!!.weight.length < 4 && dataSnapshot.getValue(Request::class.java)!!.weight != "") {

                    val weight: Int = dataSnapshot.getValue(Request::class.java)!!.weight.toDouble().toInt()

                    Log.d(fragTag, "localDate: $localDate nowDate: $nowDate")
                    if (/*localDate == nowDate*/true) {

                        weightUsage[weight] = weightUsage.getOrDefault(weight, 0) + 1
                        Log.d(fragTag,
                                "onChildAdded: dataPoints.set called with: $weight " +
                                        "X: ${weight.toDouble()} Y: ${weightUsage[weight]!!.toDouble()}")

//                        Log.d(fragTag, "ARRAYELEMENT: " + dataPoints.get(weight))
//                        dataPoints[weight] = DataPoint(weight.toDouble(), weightUsage.get(weight)!!.toDouble())
                        mSeries7 = BarGraphSeries(weightUsage.map {
                            DataPoint(it.key.toDouble(), it.value.toDouble())
                        }.toTypedArray()).apply {
                            // styling
                            valueDependentColor = ValueDependentColor { data ->
                                Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                            }
                            spacing = 30
                            isDrawValuesOnTop = true
                            valuesOnTopColor = Color.RED
                        }

                        graph1.let {
                            it.removeAllSeries()
                            it.addSeries(mSeries7)
                            it.title = "Request counts for each dumbbell today"
                            it.viewport.setMinY(0.0)
                            it.viewport.setMaxY(weightUsage.values.max()!!.toDouble())
                            it.viewport.setMinX(weightUsage.keys.min()!!.toDouble())
                            it.viewport.setMaxX(weightUsage.keys.max()!!.toDouble())
                            it.viewport.isScalable = true
                            it.viewport.isScrollable = true
                            it.viewport.setScalableY(true)
                            it.viewport.setScalableY(true)
                        }


                    }
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(fragTag, "onChildChanged: ${dataSnapshot.key}")

                // A comment has changed, use the key to determine if we are displaying this
                // comment and if so displayed the changed comment.
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                Log.d(fragTag, "onChildRemoved:" + dataSnapshot.key!!)

                // A comment has changed, use the key to determine if we are displaying this
                // comment and if so remove it.
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(fragTag, "onChildMoved:" + dataSnapshot.key!!)

                // A comment has changed position, use the key to determine if we are
                // displaying this comment and if so move it.
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(fragTag, "postComments:onCancelled", databaseError.toException())
            }
        }

        requestReference.addChildEventListener(childEventListener)

    }

    companion object {
        @JvmStatic
        fun newInstance() = AnalyticsFragment()
    }


}
