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
import com.jjoe64.graphview.series.LineGraphSeries
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.*


//TODO create a login
class AnalyticsFragment : Fragment() {

    private var mSeries1: BarGraphSeries<DataPoint>? = null
    private var mSeries2: BarGraphSeries<DataPoint>? = null
    private var mSeries3: BarGraphSeries<DataPoint>? = null
    private var mSeries4: BarGraphSeries<DataPoint>? = null
    private var mSeries5: BarGraphSeries<DataPoint>? = null
    private var mSeries6: BarGraphSeries<DataPoint>? = null
    private var mSeries7: BarGraphSeries<DataPoint>? = null
    private var mSeries8: BarGraphSeries<DataPoint>? = null

    private var mSeries9: LineGraphSeries<DataPoint> = LineGraphSeries()

    private var weightUsageToday: HashMap<Int, Int> = HashMap()
    private var weightUsageWeek: HashMap<Int, Int> = HashMap()
    private var weightUsageMonth: HashMap<Int, Int> = HashMap()
    private var weightUsageYear: HashMap<Int, Int> = HashMap()

    private var benchUsageToday: HashMap<Int, Int> = HashMap()
    private var benchUsageWeek: HashMap<Int, Int> = HashMap()
    private var benchUsageMonth: HashMap<Int, Int> = HashMap()
    private var benchUsageYear: HashMap<Int, Int> = HashMap()

    private var requestsPerHour: HashMap<Int, Int> = HashMap()

    private lateinit var graph1: GraphView
    private lateinit var graph2: GraphView
    private lateinit var graph3: GraphView

    //get from sharedpref
    private var openingHour:Int = 6
    private var openingMinute:Int = 30
    private var closingHour:Int = 21
    private var closingMinute:Int = 0

    private var myTimer = Timer()


    private val fragTag = "AnalyticsFragment"

    private val ref = FirebaseDatabase.getInstance().reference
    private val requestReference = ref.child("demo2").child("log")

    private var type= "Today"


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)

        //the two bargraphs displayed on the screen
        graph1 = view.findViewById(R.id.graph1)
        graph2 = view.findViewById(R.id.graph2)
        //line graph
        graph3 = view.findViewById(R.id.graph3)


        graph3.addSeries(mSeries9)
        graph3.viewport.setMinX(0.0)
        graph3.viewport.setMinY(0.0)
        graph3.viewport.setMaxX(40.0)
        graph3.title = "Number of requests throughout the day"

        mSeries9.appendData(DataPoint(timeToDecimal(openingHour, openingMinute), 0.0), true, 24)


        //just for displaying
        requestsPerHour[7] = 5
        requestsPerHour[8] = 4
        requestsPerHour[9] = 7
        requestsPerHour[10] = 12
        requestsPerHour[11] = 8
        requestsPerHour[12] = 7


        //add firebase listener
        addListener()

        //create spinner to hold selection box for period of time for graph
        val dropdown: Spinner = view.findViewById(R.id.spinner1)

        dropdown.apply {
            adapter = ArrayAdapter(this@AnalyticsFragment.requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    arrayOf("Today", "This Week", "This Month", "This Year"))

            // Set an on item selected listener for spinner object
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {


                    when (parent.getItemAtPosition(position).toString()) {

                        "Today" -> {
                            type = "Today"
                            updateGraph1()
                            updateGraph2()

                        }
                        "This Week" -> {
                            type = "Week"
                            updateGraph1()
                            updateGraph2()


                        }
                        //show last months analytics
                        "This Month" -> {

                            type = "Month"
                            updateGraph1()
                            updateGraph2()


                        }
                        //show last years analytics
                        "This Year" -> {
                            type = "Year"
                            updateGraph1()
                            updateGraph2()


                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Another interface callback

                }
            }
        }

        //--------------for graph 3------------------

        val date = Date()
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val nowHour = now.hour

        //update graph3 with the requests issued in passed hours
        for (element in requestsPerHour) {
            //don't add before hour is over - the timertask will take care of this
            if (nowHour != element.key) {
                mSeries9.appendData(DataPoint(element.key.toDouble(), element.value.toDouble()), true, 24)
            }
        }


        //update graph3 every hour with requests made during that hour starting from next :00 hour

        val updateGraphTask = setTimerTask()

        val calNextHour = Calendar.getInstance()
        calNextHour.set(Calendar.HOUR_OF_DAY, nowHour + 1)
        calNextHour.set(Calendar.MINUTE, 0)
        val nextHour = calNextHour.time

        val delay = nextHour.time - date.time
        //start timer at next hour for every hour
        myTimer.scheduleAtFixedRate(updateGraphTask, delay, 3600000)



        //reset requestsPerHour and mSeries9 each midnight

        val calMidnight = Calendar.getInstance()
        calMidnight.set(Calendar.HOUR_OF_DAY, 24)
        calMidnight.set(Calendar.MINUTE, 0)
        val midnight = calMidnight.time

        val timeToMidnight = midnight.time - date.time

        val midnightTimer = Timer()
        val task = object : TimerTask() {
            override fun run() {
                requestsPerHour.clear()
                mSeries9 = LineGraphSeries()
            }
        }
        midnightTimer.scheduleAtFixedRate(task, timeToMidnight, 86400000)

        return view
    }



    private fun addListener() {
        Log.d(fragTag, "addListener")

        //listen for changes in the database
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(fragTag, "onChildAdded:" + dataSnapshot.key!!)

                //get current date
                val now = LocalDateTime.now(ZoneOffset.UTC)
                val nowDate = now.toLocalDate()
                val weekFields = WeekFields.of(Locale.FRANCE)
                val nowWeek = nowDate.get(weekFields.weekOfWeekBasedYear())
                val nowMonth = now.monthValue
                val nowYear = now.year
                val nowHour = now.hour

                //get date and time of the added request
                val unixSeconds: Long = dataSnapshot.getValue(Request::class.java)!!.time
                val date = java.util.Date(unixSeconds * 1000)
                val requestDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                val requestWeek = requestDate.get(weekFields.weekOfWeekBasedYear())
                val requestMonth = requestDate.monthValue
                val requestYear = requestDate.year
                val requestDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                val requestHour = requestDateTime.hour
                val requestMinute = requestDateTime.minute

                Log.d(fragTag, "requestDate: $requestDate nowDate: $nowDate requestWeek: $requestWeek nowWeek: $nowWeek")

                val type: String = dataSnapshot.getValue(LoggedRequest::class.java)!!.type


                // check that the weight is valid and that the weight has been delivered
                if (dataSnapshot.getValue(LoggedRequest::class.java)!!.weight.length < 4 && dataSnapshot.getValue(LoggedRequest::class.java)!!.weight != "" && type == "delivering") {


                    val weight: Int = dataSnapshot.getValue(LoggedRequest::class.java)!!.weight.toDouble().toInt()
                    val bench: Int = dataSnapshot.getValue(LoggedRequest::class.java)!!.end_node.substring(1).toInt()

                    if (requestDate == nowDate) {
                        weightUsageToday[weight] = weightUsageToday.getOrDefault(weight, 0) + 1
                        benchUsageToday[bench] = benchUsageToday.getOrDefault(bench, 0) + 1

                    }

                    if (requestWeek == nowWeek) {
                        weightUsageWeek[weight] = weightUsageWeek.getOrDefault(weight, 0) + 1
                        benchUsageWeek[bench] = benchUsageWeek.getOrDefault(bench, 0) + 1

                    }

                    if (requestMonth == nowMonth) {
                        weightUsageMonth[weight] = weightUsageMonth.getOrDefault(weight, 0) + 1
                        benchUsageMonth[bench] = benchUsageMonth.getOrDefault(bench, 0) + 1

                    }

                    if (requestYear == nowYear) {
                        weightUsageYear[weight] = weightUsageYear.getOrDefault(weight, 0) + 1
                        benchUsageYear[bench] = benchUsageYear.getOrDefault(bench, 0) + 1

                    }
                    //if closing time is 21:30 requestsPerHour will store requests made in last 30 mins under key 22
                    if(requestHour == nowHour && isInOpenHours(requestHour, requestMinute) ){

                        requestsPerHour[requestHour + 1] = requestsPerHour.getOrDefault(requestHour + 1, 0) + 1

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
        updateGraph1()
        updateGraph2()
    }

    private fun updateGraph1(){
        Log.d(fragTag, "updateGraph1")
        graph1.removeAllSeries()

        when (type) {

            "Today" -> {
                val spc = getSpacingAccordingToSize(weightUsageToday.keys.size)

                mSeries7 = BarGraphSeries(weightUsageToday.map {
                    DataPoint(it.key.toDouble(), it.value.toDouble()) }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = spc
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }


                graph1.let {
                    it.title = "Request counts for each dumbbell today"
                    it.removeAllSeries()
                    it.addSeries(mSeries7)
                }

                if(!weightUsageToday.isEmpty()){
                    graph1.viewport.setMaxX(weightUsageToday.keys.max()!!.toDouble() + 2 )
                }
            }
            "Week" -> {
                val spc = getSpacingAccordingToSize(weightUsageWeek.keys.size)

                mSeries1 = BarGraphSeries(weightUsageWeek.map {
                    DataPoint(it.key.toDouble(), it.value.toDouble()) }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = spc
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }


                graph1.let {
                    it.title = "Request counts for each dumbbell this week"
                    it.removeAllSeries()
                    it.addSeries(mSeries1)
                }

                if(!weightUsageWeek.isEmpty()){
                    graph1.viewport.setMaxX(weightUsageWeek.keys.max()!!.toDouble() + 2)
                }
            }
            "Month" -> {
                val spc = getSpacingAccordingToSize(weightUsageMonth.keys.size)

                mSeries3 = BarGraphSeries(weightUsageMonth.map {
                    DataPoint(it.key.toDouble(), it.value.toDouble()) }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = spc
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }


                graph1.let {
                    it.title = "Request counts for each dumbbell this month"
                    it.removeAllSeries()
                    it.addSeries(mSeries3)
                }
                if(!weightUsageMonth.isEmpty()){
                    graph1.viewport.setMaxX(weightUsageMonth.keys.max()!!.toDouble() + 2)
                }
            }
            "Year" -> {
                val spc = getSpacingAccordingToSize(weightUsageYear.keys.size)

                mSeries5 = BarGraphSeries(weightUsageYear.map {
                    DataPoint(it.key.toDouble(), it.value.toDouble()) }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = spc
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }


                graph1.let {
                    it.title = "Request counts for each dumbbell this year"
                    it.removeAllSeries()
                    it.addSeries(mSeries5)
                }
                if(!weightUsageYear.isEmpty()){
                    graph1.viewport.setMaxX(weightUsageYear.keys.max()!!.toDouble() + 2)
                }
            }
        }


        graph1.let {
            it.viewport.setMinY(0.0)
            it.viewport.setMinX(0.0)
            it.viewport.isScalable = true
            it.viewport.isScrollable = true
            it.viewport.setScalableY(true)
            it.viewport.setScalableY(true)
        }
    }

    private fun updateGraph2() {
        Log.d(fragTag, "updateGraph2")
        graph2.removeAllSeries()

        when (type) {

            "Today" -> {
                val spc = getSpacingAccordingToSize(benchUsageToday.keys.size)

                mSeries8 = BarGraphSeries(benchUsageToday.map {
                    DataPoint(it.key.toDouble(), it.value.toDouble()) }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = spc
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }

                graph2.let {
                    it.title = "Request counts from each bench today"
                    it.removeAllSeries()
                    it.addSeries(mSeries8)
                }
                if(!benchUsageToday.isEmpty()){
                    graph2.viewport.setMaxX(benchUsageToday.keys.max()!!.toDouble() + 2)
                }

            }
            "Week" -> {
                val spc = getSpacingAccordingToSize(benchUsageWeek.keys.size)

                mSeries2 = BarGraphSeries(benchUsageWeek.map {
                    DataPoint(it.key.toDouble(), it.value.toDouble()) }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = spc
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }

                graph2.let {
                    it.title = "Request counts from each bench this week"
                    it.removeAllSeries()
                    it.addSeries(mSeries2)
                }
                if(!benchUsageWeek.isEmpty()){
                    graph2.viewport.setMaxX(benchUsageWeek.keys.max()!!.toDouble() + 2)
                }
            }
            "Month" -> {
                val spc = getSpacingAccordingToSize(benchUsageMonth.keys.size)

                mSeries4 = BarGraphSeries(benchUsageMonth.map {
                    DataPoint(it.key.toDouble(), it.value.toDouble()) }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = spc
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }



                graph2.let {
                    it.title = "Request counts from each bench this month"
                    it.removeAllSeries()
                    it.addSeries(mSeries4)
                }
                if(!benchUsageMonth.isEmpty()){
                    graph2.viewport.setMaxX(benchUsageMonth.keys.max()!!.toDouble() + 2)
                }
            }
            "Year" -> {
                val spc = getSpacingAccordingToSize(benchUsageYear.keys.size)

                mSeries6 = BarGraphSeries(benchUsageYear.map {
                    DataPoint(it.key.toDouble(), it.value.toDouble()) }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = spc
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }


                graph2.let {
                    it.title = "Request counts from each bench this year"
                    it.removeAllSeries()
                    it.addSeries(mSeries6)
                }
                if(!benchUsageYear.isEmpty()){
                    graph2.viewport.setMaxX(benchUsageYear.keys.max()!!.toDouble() + 2)
                }
            }

        }

        graph2.let {
            it.viewport.setMinY(0.0)
            it.viewport.setMinX(0.0)
            it.viewport.isScalable = true
            it.viewport.isScrollable = true
            it.viewport.setScalableY(true)
            it.viewport.setScalableY(true)
        }
    }
    private fun getSpacingAccordingToSize(size:Int):Int{
        when (size) {

            1 -> return 80
            2 -> return 70
        }
        return 30
    }

    //returns true to exact opening time and exact closing time
    private fun isInOpenHours(hour:Int, minutes:Int):Boolean{
        if(hour > openingHour && hour < closingHour){
            return true
        }
        if(hour == openingHour && minutes >= openingMinute && hour == closingHour && minutes <= closingMinute){
            return true
        }
        return false
    }

    private fun isClosingHour(nowHour:Int, nowMinute:Int):Boolean{
        // less than 5 minutes until closing hour or timer went past closingHour
        return nowHour == closingHour - 1 && nowMinute > 55 || nowHour == closingHour && nowMinute < 5
    }


    private fun timeToDecimal(hour:Int, minutes:Int): Double{
        return (hour + (minutes /60.toDouble()) )

    }

    private fun setTimerTask():TimerTask {

        val task = object : TimerTask() {
            override fun run() {
                val now = LocalDateTime.now(ZoneOffset.UTC)
                val nowHour = now.hour
                val nowMinute = now.minute

                if(isInOpenHours(nowHour,nowMinute)) {

                    if (isClosingHour(nowHour, nowMinute) && closingMinute != 0 ) {
                            //set new timer: when the last minutes til closing have gone update graph
                            val timer2 = Timer()
                            val task2 = object : TimerTask() {
                                override fun run() {

                                    //update graph for last minutes
                                    //use timeToDecimal to get x coordinate of datapoint to put on graph
                                    mSeries9.appendData(
                                        DataPoint(
                                            timeToDecimal(closingHour, closingMinute),
                                            requestsPerHour.getOrDefault(closingHour + 1, 0).toDouble()
                                        ), true, 24
                                    )
                                    timer2.cancel()
                                }
                            }
                            //execute after last minutes have gone
                            val delay = closingMinute - nowMinute
                            timer2.scheduleAtFixedRate(task2, delay.toLong(), 1000)

                    }else{
                        //check if timer is early
                        if(nowMinute > 55){
                            //update graph
                            mSeries9.appendData(
                                DataPoint(
                                    nowHour.toDouble(),
                                    requestsPerHour.getOrDefault(nowHour + 1, 0).toDouble()
                                ), true, 24
                            )
                        }else{
                            //update graph
                            mSeries9.appendData(
                                DataPoint(
                                    nowHour.toDouble(),
                                    requestsPerHour.getOrDefault(nowHour, 0).toDouble()
                                ), true, 24
                            )
                        }

                    }
                 //if time is outside opening hours, append zeros
                }else {
                    //update graph
                    mSeries9.appendData(
                        DataPoint(
                            nowHour.toDouble(),
                            0.0
                        ), true, 24
                    )

                }
                Log.v("TImer", "repeated")

            }
        }

        return task

    }

    companion object {
        @JvmStatic
        fun newInstance() = AnalyticsFragment()
    }


}
