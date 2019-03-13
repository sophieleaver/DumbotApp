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
import java.time.temporal.WeekFields
import java.util.*


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

    private var weightUsageToday: HashMap<Int, Int> = HashMap()
    private var weightUsageWeek: HashMap<Int, Int> = HashMap()
    private var weightUsageMonth: HashMap<Int, Int> = HashMap()
    private var weightUsageYear: HashMap<Int, Int> = HashMap()

    private lateinit var graph1: GraphView
    private lateinit var graph2: GraphView

    //variable if we want to make hourly updates on dumbbell and station usages
//    private var time = 6
    private val fragTag = "AnalyticsFragment"

    //firebase variables
    private val ref = FirebaseDatabase.getInstance().reference
    private val requestReference = ref.child("demo2").child("log")

    private var clickedToday = false
    private var clickedWeek = false
    private var clickedMonth = false
    private var clickedYear = false


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
                    arrayOf("Today", "This Week", "This Month", "This Year"))

            // Set an on item selected listener for spinner object
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

                    //show last week analytics
                    when (parent.getItemAtPosition(position).toString()) {

                        "Today" -> {

                            if(!clickedToday){
                                addListener("Today")
                                clickedToday = true
                            }else{

                                updateGraph("Today")
                                Log.d(fragTag, "clickedYear is true")
                            }
                        }
                        "This Week" -> {

                            if(!clickedWeek){
                                addListener("Week")
                                clickedWeek = true
                            }else{

                                updateGraph("Week")
                                Log.d(fragTag, "clickedYear is true")
                            }

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
                        "This Month" -> {


                            if(!clickedMonth){
                                addListener("Month")
                                clickedMonth = true
                            }else{
                                updateGraph("Month")
                                Log.d(fragTag, "clickedYear is true")
                            }

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
                        "This Year" -> {

                            if(!clickedYear){
                                addListener("Year")
                                clickedYear = true
                            }else{
                                updateGraph("Year")
                                Log.d(fragTag, "clickedYear is true")
                            }

                            //-----------------monthly station counts---------------------------


                            graph2.removeAllSeries()
                            if (mSeries6 == null) {
                                mSeries6 = BarGraphSeries(arrayOf(

                                    //should be initialised to zero every month
                                    //should be imported from firestore
                                    //updated everytime a request is issued in the requests page
                                    DataPoint(1.0, 120.0),
                                    DataPoint(2.0, 140.0),
                                    DataPoint(3.0, 170.0),
                                    DataPoint(4.0, 110.0),
                                    DataPoint(5.0, 120.0)))
                            }
                            graph2.addSeries(mSeries6)
                            graph2.title = "Request counts for each workout station last month"

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



    private fun addListener(type:String) {


        //listen for changes in the database
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(fragTag, "onChildAdded:" + dataSnapshot.key!!)
                //get current date
                val now = LocalDateTime.now(ZoneOffset.UTC)
                val nowDate = now.toLocalDate()
                val weekFields = WeekFields.of(Locale.getDefault())
                val nowWeek = nowDate.get(weekFields.weekOfWeekBasedYear())
                val nowMonth = now.monthValue
                val nowYear = now.year

                //get date of the added request
                val unixSeconds: Long = dataSnapshot.getValue(Request::class.java)!!.time
                val date = java.util.Date(unixSeconds * 1000)
                val requestDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                val requestWeek = requestDate.get(weekFields.weekOfWeekBasedYear())
                val requestMonth = requestDate.monthValue
                val requestYear = requestDate.year

                // check that the weight is valid
                if (dataSnapshot.getValue(Request::class.java)!!.weight.length < 4 && dataSnapshot.getValue(Request::class.java)!!.weight != "") {

                    val weight: Int = dataSnapshot.getValue(Request::class.java)!!.weight.toDouble().toInt()

                    when (type) {

                        "Today" -> {

                            Log.d(fragTag, "localDate: $requestDate nowDate: $nowDate")
                            if (requestDate == nowDate) {
                               // Log.d(fragTag, "onChildAdded: dataPoints.set called with: $weight " + "X: ${weight.toDouble()} Y: ${weightUsage[weight]!!.toDouble()}")
                                weightUsageToday[weight] = weightUsageToday.getOrDefault(weight, 0) + 1
                                updateGraph("Today")
                            }


                        }
                        "Week" -> {

                            Log.d(fragTag, "localDate: $requestDate nowDate: $nowDate")
                            if (requestWeek == nowWeek) {
                                //Log.d(fragTag, "onChildAdded: dataPoints.set called with: $weight " + "X: ${weight.toDouble()} Y: ${weightUsage[weight]!!.toDouble()}")
                                weightUsageWeek[weight] = weightUsageWeek.getOrDefault(weight, 0) + 1
                                updateGraph("Week")
                            }


                        }
                        "Month" -> {

                            Log.d(fragTag, "localDate: $requestDate nowDate: $nowDate")
                            if (requestMonth == nowMonth) {
                               // Log.d(fragTag, "onChildAdded: dataPoints.set called with: $weight " + "X: ${weight.toDouble()} Y: ${weightUsage[weight]!!.toDouble()}")
                                weightUsageMonth[weight] = weightUsageMonth.getOrDefault(weight, 0) + 1
                                updateGraph("Month")
                            }


                        }
                        "Year" -> {
                            Log.d(fragTag, "localDate: $requestDate nowDate: $nowDate")
                            if (requestYear == nowYear) {
                                //Log.d(fragTag, "onChildAdded: dataPoints.set called with: $weight " + "X: ${weight.toDouble()} Y: ${weightUsage[weight]!!.toDouble()}")
                                weightUsageYear[weight] = weightUsageYear.getOrDefault(weight, 0) + 1
                                updateGraph("Year")
                            }


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

    private fun updateGraph(type:String){
        graph1.removeAllSeries()

//                        Log.d(fragTag, "ARRAYELEMENT: " + dataPoints.get(weight))
//                        dataPoints[weight] = DataPoint(weight.toDouble(), weightUsage.get(weight)!!.toDouble())

        when (type) {

            "Today" -> {
                mSeries7 = BarGraphSeries(weightUsageToday.map {
                    DataPoint(it.key.toDouble(), it.value.toDouble()) }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = 30
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }
                graph1.let {
                    it.title = "Request counts for each dumbbell today"
                    it.removeAllSeries()
                    it.addSeries(mSeries7)
                }
            }
            "Week" -> {
                mSeries1 = BarGraphSeries(weightUsageWeek.map {
                    DataPoint(it.key.toDouble(), it.value.toDouble()) }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = 30
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }
                graph1.let {
                    it.title = "Request counts for each dumbbell this week"
                    it.removeAllSeries()
                    it.addSeries(mSeries1)
                }
            }
            "Month" -> {
                mSeries3 = BarGraphSeries(weightUsageMonth.map {
                    DataPoint(it.key.toDouble(), it.value.toDouble()) }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = 30
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }
                graph1.let {
                    it.title = "Request counts for each dumbbell this month"
                    it.removeAllSeries()
                    it.addSeries(mSeries3)
                }
            }
            "Year" -> {
                mSeries5 = BarGraphSeries(weightUsageYear.map {
                    DataPoint(it.key.toDouble(), it.value.toDouble()) }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = 30
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }
                graph1.let {
                    it.title = "Request counts for each dumbbell this year"
                    it.removeAllSeries()
                    it.addSeries(mSeries5)
                }
            }
        }


        graph1.let {
            it.viewport.setMinY(0.0)
//            it.viewport.setMaxY(weightUsage.values.max()!!.toDouble())
//            it.viewport.setMinX(weightUsage.keys.min()!!.toDouble())
//            it.viewport.setMaxX(weightUsage.keys.max()!!.toDouble())
            it.viewport.setMaxY(200.0)
            it.viewport.setMinX(0.0)
            it.viewport.setMaxX(20.0)
            it.viewport.isScalable = true
            it.viewport.isScrollable = true
            it.viewport.setScalableY(true)
            it.viewport.setScalableY(true)
        }
    }



    companion object {
        @JvmStatic
        fun newInstance() = AnalyticsFragment()
    }


}
