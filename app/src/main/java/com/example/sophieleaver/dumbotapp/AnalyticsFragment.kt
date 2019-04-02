package com.example.sophieleaver.dumbotapp

import android.content.Context
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
import com.jjoe64.graphview.*
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter
import com.jjoe64.graphview.helper.StaticLabelsFormatter
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.jjoe64.graphview.series.PointsGraphSeries
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.*
import java.util.concurrent.TimeUnit


class AnalyticsFragment : Fragment() {

    private var mSeries: BarGraphSeries<DataPoint>? = null

    private var lineSeries: LineGraphSeries<DataPoint> = LineGraphSeries()

    private var weightUsageToday: HashMap<Double, Int> = HashMap()
    private var weightUsageWeek: HashMap<Double, Int> = HashMap()
    private var weightUsageMonth: HashMap<Double, Int> = HashMap()
    private var weightUsageYear: HashMap<Double, Int> = HashMap()

    private var benchUsageToday: HashMap<Int, Int> = HashMap()
    private var benchUsageWeek: HashMap<Int, Int> = HashMap()
    private var benchUsageMonth: HashMap<Int, Int> = HashMap()
    private var benchUsageYear: HashMap<Int, Int> = HashMap()

    //reset every day
    private var requestsPerHour: HashMap<Double, Int> = HashMap()
    private var requestsPerWeekDay: HashMap<Int, Int> = HashMap()
    private var requestsPerDayOfMonth: HashMap<Int, Int> = HashMap()
    private var requestsPerMonth: HashMap<Int, Int> = HashMap()


    private lateinit var graph1: GraphView
    private lateinit var graph2: GraphView
    private lateinit var graph3: GraphView

    private var openingHour: Int = 7
    private var openingMinute: Int = 0
    private var closingHour: Int = 21
    private var closingMinute: Int = 0

    private val fragTag = "AnalyticsFragment"

    private val ref = FirebaseDatabase.getInstance().reference
    private val requestReference = ref.child("demo2").child("log")

    private var type = "Today"


    private lateinit var graph3DefaultLabelFormatter: DefaultLabelFormatter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)

        //the two bargraphs displayed on the screen
        graph1 = view.findViewById(R.id.graph1)
        graph2 = view.findViewById(R.id.graph2)
        //line graph
        graph3 = view.findViewById(R.id.graph3)

        graph3DefaultLabelFormatter = DefaultLabelFormatter()



        //get opening hours from sharedprefs
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val weekDayNr: Int = now.dayOfWeek.value
        val weekFields = WeekFields.of(Locale.FRANCE)
        val nowWeek = now.get(weekFields.weekOfWeekBasedYear())
        val nowMonth = now.month.value
        val nowYear = now.year

        val sharedPref =
            with(requireActivity()) { getSharedPreferences("prefs", Context.MODE_PRIVATE) }
        openingHour = sharedPref.getInt(weekDayNr.toString() + "OpenHour", 7)
        openingMinute = sharedPref.getInt(weekDayNr.toString() + "OpenMinute", 0)
        closingHour = sharedPref.getInt(weekDayNr.toString() + "CloseHour", 21)
        closingMinute = sharedPref.getInt(weekDayNr.toString() + "CloseMinute", 0)


        //add firebase listener
        addListener()

        //create spinner to hold selection box for period of time for graph
        val dropdown: Spinner = view.findViewById(R.id.spinner1)

        dropdown.apply {
            adapter = ArrayAdapter(
                this@AnalyticsFragment.requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                arrayOf("Today", "This Week", "This Month", "This Year")
            )

            // Set an on item selected listener for spinner object
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {

                    type = parent.getItemAtPosition(position).toString()
                    updateGraph1()
                    updateGraph2()
                    updateGraph3()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Another interface callback

                }
            }
        }

        //reset requestsPerHour every midnight
        val midnight = Calendar.getInstance()
        midnight.set(Calendar.HOUR_OF_DAY, 0)
        midnight.set(Calendar.MINUTE, 0)
        midnight.set(Calendar.SECOND, 0)

        val timerTaskDaily = object : TimerTask() {
            override fun run() {
                requestsPerHour.clear()
                weightUsageToday.clear()
                benchUsageToday.clear()

                //check if a new week/ month/ year has started
                val newNow = LocalDateTime.now(ZoneOffset.UTC)
                val newWeek = newNow.get(weekFields.weekOfWeekBasedYear())
                val newMonth = newNow.month.value
                val newYear = newNow.year

                if (newWeek != nowWeek) {
                    requestsPerWeekDay.clear()
                    benchUsageWeek.clear()
                    weightUsageWeek.clear()
                }
                if (newMonth != nowMonth) {
                    requestsPerDayOfMonth.clear()
                    benchUsageMonth.clear()
                    weightUsageMonth.clear()
                }
                if (newYear != nowYear) {
                    requestsPerMonth.clear()
                    weightUsageYear.clear()
                    benchUsageYear.clear()

                }
                //update opening hours
                val newWeekDayNr: Int = newNow.dayOfWeek.value
                openingHour = sharedPref.getInt(newWeekDayNr.toString() + "OpenHour", 7)
                openingMinute = sharedPref.getInt(newWeekDayNr.toString() + "OpenMinute", 0)
                closingHour = sharedPref.getInt(newWeekDayNr.toString() + "CloseHour", 21)
                closingMinute = sharedPref.getInt(newWeekDayNr.toString() + "CloseMinute", 0)

                updateGraph1()
                updateGraph2()
                updateGraph3()

            }
        }

        val timer = Timer()
        timer.schedule(
            timerTaskDaily,
            midnight.time,
            TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)
        )


        return view
    }


    private fun addListener() {
        Log.d(fragTag, "addListener")

        //listen for changes in the database
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(fragTag, "onChildAdded:" + dataSnapshot.key!!)

                //get current date and time
                val now = LocalDateTime.now(ZoneOffset.UTC)
                val nowDate = now.toLocalDate()
                val weekFields = WeekFields.of(Locale.FRANCE)
                val nowWeek = nowDate.get(weekFields.weekOfWeekBasedYear())
                val nowMonth = now.monthValue
                val nowYear = now.year
//                val nowWeekDayNr: Int = now.dayOfWeek.value
//                val nowDayOfMonth = now.dayOfMonth
//                val nowHour = now.hour

                //get date and time of the added request
                val unixSeconds: Long = dataSnapshot.getValue(Request::class.java)!!.time
                val date = java.util.Date(unixSeconds * 1000)
                val requestDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                val requestWeek = requestDate.get(weekFields.weekOfWeekBasedYear())
                val requestMonth = requestDate.monthValue
                val requestYear = requestDate.year
                val requestDateTime =
                    date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                val requestHour = requestDateTime.hour
                val requestMinute = requestDateTime.minute
                val requestWeekDayNr = requestDateTime.dayOfWeek.value
                val requestDayOfMonth = requestDateTime.dayOfMonth

                Log.d(
                    fragTag,
                    "requestDate: $requestDate nowDate: $nowDate requestWeek: $requestWeek nowWeek: $nowWeek"
                )

                val type: String = dataSnapshot.getValue(LoggedRequest::class.java)!!.type


                // check that the weight is valid and that the weight is being delivered,
                // and that the request was made during the open hours of the gym
                if (isInOpenHours(
                        requestHour,
                        requestMinute,
                        requestWeekDayNr
                    ) && dataSnapshot.getValue(LoggedRequest::class.java)!!.weight.length < 4 && dataSnapshot.getValue(
                        LoggedRequest::class.java
                    )!!.weight != "" && type == "delivering"
                ) {


                    val weight: Double =
                        dataSnapshot.getValue(LoggedRequest::class.java)!!.weight.toDouble()
                    val bench: Int =
                        dataSnapshot.getValue(LoggedRequest::class.java)!!.end_node.substring(1)
                            .toInt()

                    if (requestDate == nowDate) {
                        weightUsageToday[weight] = weightUsageToday.getOrDefault(weight, 0) + 1
                        benchUsageToday[bench] = benchUsageToday.getOrDefault(bench, 0) + 1

                        //if closing time is 21:30 requestsPerHour will store requests made in last 30 mins under key 22
                        requestsPerHour[requestHour + 1.0] =
                                requestsPerHour.getOrDefault(requestHour + 1.0, 0) + 1

                    }

                    if (requestWeek == nowWeek) {
                        weightUsageWeek[weight] = weightUsageWeek.getOrDefault(weight, 0) + 1
                        benchUsageWeek[bench] = benchUsageWeek.getOrDefault(bench, 0) + 1

                        requestsPerWeekDay[requestWeekDayNr] =
                                requestsPerWeekDay.getOrDefault(requestWeekDayNr, 0) + 1

                    }

                    if (requestMonth == nowMonth) {
                        weightUsageMonth[weight] = weightUsageMonth.getOrDefault(weight, 0) + 1
                        benchUsageMonth[bench] = benchUsageMonth.getOrDefault(bench, 0) + 1

                        requestsPerDayOfMonth[requestDayOfMonth] =
                                requestsPerDayOfMonth.getOrDefault(requestDayOfMonth, 0) + 1


                    }

                    if (requestYear == nowYear) {
                        weightUsageYear[weight] = weightUsageYear.getOrDefault(weight, 0) + 1
                        benchUsageYear[bench] = benchUsageYear.getOrDefault(bench, 0) + 1
                        requestsPerMonth[requestMonth] =
                                requestsPerMonth.getOrDefault(requestMonth, 0) + 1


                    }

                    updateGraph1()
                    updateGraph2()
                    updateGraph3()
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

    private fun updateGraph1() {
        Log.d(fragTag, "updateGraph1")
        graph1.removeAllSeries()
        graph1.gridLabelRenderer.setTextSize(22f)

        val defaultLabelFormatter = object : DefaultLabelFormatter() {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                if (isValueX) {
                    // show kg for x values
                    return super.formatLabel(value, isValueX) + "kg"
                } else {
                    // show normal y values
                    return super.formatLabel(value, isValueX)
                }
            }
        }
        graph1.gridLabelRenderer.labelFormatter= defaultLabelFormatter

        when (type) {

            "Today" -> {
                val spc = getSpacingAccordingToSize(weightUsageToday.keys.size)

                mSeries = BarGraphSeries(weightUsageToday.toSortedMap().map {
                    DataPoint(it.key, it.value.toDouble())
                }.toTypedArray()).apply {
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
                    it.addSeries(mSeries)
                }

                if (!weightUsageToday.isEmpty()) {
                    graph1.viewport.isXAxisBoundsManual = true
                    graph1.viewport.setMaxX(weightUsageToday.keys.max()!!.toDouble() + 2)
                    graph1.viewport.setMaxY(weightUsageToday.values.max()!!.toDouble() + 5)
                }else {
                    graph1.let {
                        it.viewport.isXAxisBoundsManual = true
                        it.viewport.setMaxX(10.0)
                    }

                }
            }
            "This Week" -> {
                val spc = getSpacingAccordingToSize(weightUsageWeek.keys.size)

                mSeries = BarGraphSeries(weightUsageWeek.toSortedMap().map {
                    DataPoint(it.key, it.value.toDouble())
                }.toTypedArray()).apply {
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
                    it.addSeries(mSeries)
                }

                if (!weightUsageWeek.isEmpty()) {
                    graph1.viewport.isXAxisBoundsManual = true
                    graph1.viewport.setMaxX(weightUsageWeek.keys.max()!!.toDouble() + 2)
                    graph1.viewport.setMaxY(weightUsageWeek.values.max()!!.toDouble() + 5)
                }else{
                    graph1.let {
                        it.viewport.isXAxisBoundsManual = true
                        it.viewport.setMaxX(10.0)
                    }
                }

            }
            "This Month" -> {
                val spc = getSpacingAccordingToSize(weightUsageMonth.keys.size)

                mSeries = BarGraphSeries(weightUsageMonth.toSortedMap().map {
                    DataPoint(it.key, it.value.toDouble())
                }.toTypedArray()).apply {
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
                    it.addSeries(mSeries)
                }
                if (!weightUsageMonth.isEmpty()) {
                    graph1.viewport.isXAxisBoundsManual = true
                    graph1.viewport.setMaxX(weightUsageMonth.keys.max()!!.toDouble() + 2)
                    graph1.viewport.setMaxY(weightUsageMonth.values.max()!!.toDouble() + 5)
                }else{
                    graph1.let {
                        it.viewport.isXAxisBoundsManual = true
                        it.viewport.setMaxX(10.0)
                    }
                }
            }
            "This Year" -> {
                val spc = getSpacingAccordingToSize(weightUsageYear.keys.size)

                mSeries = BarGraphSeries(weightUsageYear.toSortedMap().map {
                    DataPoint(it.key, it.value.toDouble())
                }.toTypedArray()).apply {
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
                    it.addSeries(mSeries)
                }
                if (!weightUsageYear.isEmpty()) {
                    graph1.viewport.isXAxisBoundsManual = true
                    graph1.viewport.setMaxX(weightUsageYear.keys.max()!!.toDouble() + 2)
                    graph1.viewport.setMaxY(weightUsageYear.values.max()!!.toDouble() + 5)
                }else{
                    graph1.let {
                        it.viewport.isXAxisBoundsManual = true
                        it.viewport.setMaxX(10.0)
                    }
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
        graph2.gridLabelRenderer.resetStyles()
        graph2.gridLabelRenderer.setTextSize(22f)


        val defaultLabelFormatter = object : DefaultLabelFormatter() {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                if (isValueX) {
                    // show bench for x values
                    return "B" + super.formatLabel(value, isValueX)
                } else {
                    // show normal y values
                    return super.formatLabel(value, isValueX)
                }
            }
        }
        graph2.gridLabelRenderer.labelFormatter= defaultLabelFormatter

        when (type) {

            "Today" -> {
                val spc = getSpacingAccordingToSize(benchUsageToday.keys.size)

                mSeries = BarGraphSeries(benchUsageToday.toSortedMap().map {
                    DataPoint(it.key.toDouble(), it.value.toDouble())
                }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = spc
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }

                graph2.let {
                    it.title = "Request counts from each bench today"
                    it.addSeries(mSeries)

                }

                if (!benchUsageToday.isEmpty()) {
                    graph2.viewport.isXAxisBoundsManual = true
                    graph2.viewport.setMaxX(benchUsageToday.keys.max()!!.toDouble() + 2)
                    graph2.viewport.setMaxY(benchUsageToday.values.max()!!.toDouble() + 5)


                } else {
                    graph2.let {
                        it.viewport.isXAxisBoundsManual = true
                        it.viewport.setMaxX(10.0)
                    }

                }
            }
            "This Week" -> {
                val spc = getSpacingAccordingToSize(benchUsageWeek.keys.size)

                mSeries = BarGraphSeries(benchUsageWeek.toSortedMap().map {
                    DataPoint(it.key.toDouble(), it.value.toDouble())
                }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = spc
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }

                graph2.let {
                    it.title = "Request counts from each bench this week"
                    it.addSeries(mSeries)
                }
                if (!benchUsageWeek.isEmpty()) {
                    graph2.viewport.isXAxisBoundsManual = true
                    graph2.viewport.setMaxX(benchUsageWeek.keys.max()!!.toDouble() + 2)
                    graph2.viewport.setMaxY(benchUsageWeek.values.max()!!.toDouble() + 5)


                } else {
                    graph2.let {
                        it.viewport.isXAxisBoundsManual = true
                        it.viewport.setMaxX(10.0)
                    }

                }

            }
            "This Month" -> {
                val spc = getSpacingAccordingToSize(benchUsageMonth.keys.size)

                mSeries = BarGraphSeries(benchUsageMonth.toSortedMap().map {
                    DataPoint(it.key.toDouble(), it.value.toDouble())
                }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = spc
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }



                graph2.let {
                    it.title = "Request counts from each bench this month"
                    it.addSeries(mSeries)
                }
                if (!benchUsageMonth.isEmpty()) {
                    graph2.viewport.isXAxisBoundsManual = true
                    graph2.viewport.setMaxX(benchUsageMonth.keys.max()!!.toDouble() + 2)
                    graph2.viewport.setMaxY(benchUsageMonth.values.max()!!.toDouble() + 5)


                } else {
                    graph2.let {
                        it.viewport.isXAxisBoundsManual = true
                        it.viewport.setMaxX(10.0)
                    }

                }


            }
            "This Year" -> {
                val spc = getSpacingAccordingToSize(benchUsageYear.keys.size)

                mSeries = BarGraphSeries(benchUsageYear.toSortedMap().map {
                    DataPoint(it.key.toDouble(), it.value.toDouble())
                }.toTypedArray()).apply {
                    valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(data.x.toInt() * 255 / 4, Math.abs(data.y * 255 / 6).toInt(), 100)
                    }
                    spacing = spc
                    isDrawValuesOnTop = true
                    valuesOnTopColor = Color.RED
                }


                graph2.let {
                    it.title = "Request counts from each bench this year"
                    it.addSeries(mSeries)

                }
                if (!benchUsageYear.isEmpty()) {
                    graph2.viewport.isXAxisBoundsManual = true
                    graph2.viewport.setMaxX(benchUsageYear.keys.max()!!.toDouble() + 2)
                    graph2.viewport.setMaxY(benchUsageYear.values.max()!!.toDouble() + 5)


                } else {
                    graph2.let {
                        it.viewport.isXAxisBoundsManual = true
                        it.viewport.setMaxX(10.0)
                    }

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

    private fun updateGraph3() {
        Log.d(fragTag, "updateGraph3")
        graph3.removeAllSeries()
        graph3.gridLabelRenderer.labelFormatter = graph3DefaultLabelFormatter
        graph3.gridLabelRenderer.setTextSize(22f)
        val now = LocalDateTime.now(ZoneOffset.UTC)

        when (type) {

            "Today" -> {

                //no requests have been made at opening time so add 0 to graph at that time
                requestsPerHour[timeToDecimal(openingHour, openingMinute)] = 0

                //hours where no requests were made should have zero datapoints
                val nowHour = now.hour

                //if we're in open hours then append zeros until now
                if(nowHour<= closingHour){
                    for (hour in openingHour + 1..nowHour) {
                        if (!requestsPerHour.containsKey(hour.toDouble())) {
                            requestsPerHour[hour.toDouble()] = 0
                        }
                    }
                }
                //if were after open hours just append zeros up until closing hour
                else{
                    for (hour in openingHour + 1..closingHour) {
                        if (!requestsPerHour.containsKey(hour.toDouble())) {
                            requestsPerHour[hour.toDouble()] = 0
                        }
                    }
                }


                //if closing hour is 7:30 requests made between 7:00 and 7:30 should be added at 7:30 coordinate, not 8
                if (requestsPerHour.containsKey(closingHour.toDouble() + 1.0)) {

                    val requestsMadeInLastMinutes =
                        requestsPerHour.getOrDefault(closingHour.toDouble() + 1.0, 0)
                    requestsPerHour.remove(closingHour.toDouble() + 1.0)

                    lineSeries = LineGraphSeries(requestsPerHour.toSortedMap().map {
                        DataPoint(it.key, it.value.toDouble())
                    }.toTypedArray()).apply {
                        isDrawDataPoints = true
                    }
                    lineSeries.appendData(
                        DataPoint(
                            timeToDecimal(closingHour, closingMinute),
                            requestsMadeInLastMinutes.toDouble()
                        ), true, 25
                    )
                    requestsPerHour[closingHour.toDouble() + 1.0] = requestsMadeInLastMinutes

                } else {

                    lineSeries = LineGraphSeries(requestsPerHour.toSortedMap().map {
                        DataPoint(it.key, it.value.toDouble())
                    }.toTypedArray()).apply {
                        isDrawDataPoints = true
                    }
                }

                val defaultLabelFormatter = object : DefaultLabelFormatter() {
                    override fun formatLabel(value: Double, isValueX: Boolean): String {
                        if (isValueX) {
                            // show time for x values
                            return super.formatLabel(value, isValueX) + ":00"
                        } else {
                            // show normal y values
                            return super.formatLabel(value, isValueX)
                        }
                    }
                }
                graph3.gridLabelRenderer.labelFormatter = defaultLabelFormatter
                graph3.gridLabelRenderer.numHorizontalLabels = 5


                graph3.let {
                    it.addSeries(lineSeries)
                    it.title = "Request counts throughout today"
                    it.viewport.isXAxisBoundsManual = true
                    it.viewport.setMinX(0.0)
                    it.viewport.setMaxX(24.0)
                    it.viewport.setMinY(0.0)
                }

                //show opening and closing time on graph
                val series: PointsGraphSeries<DataPoint> = PointsGraphSeries(
                    arrayOf(
                        DataPoint(timeToDecimal(openingHour, openingMinute), 0.0),
                        DataPoint(timeToDecimal(closingHour, closingMinute), 0.0)
                    )
                )
                series.shape = PointsGraphSeries.Shape.POINT
                series.size = 10f
                graph3.addSeries(series)


                if (!requestsPerHour.isEmpty()) {
                    graph3.viewport.setMaxY(requestsPerHour.values.max()!!.toDouble() + 5)
                }




            }
            "This Week" -> {
                //days where no requests were made should have zero datapoints
                val nowDayOfWeek = now.dayOfWeek.value

                for (day in 1..nowDayOfWeek) {
                    if (!requestsPerWeekDay.containsKey(day)) {
                        requestsPerWeekDay[day] = 0
                    }
                }

                lineSeries = LineGraphSeries(requestsPerWeekDay.toSortedMap().map {
                    DataPoint(it.key.toDouble(), it.value.toDouble())
                }.toTypedArray()).apply {
                    isDrawDataPoints = true
                }

                graph3.let {
                    it.addSeries(lineSeries)
                    it.title = "Request counts throughout the week"
                    it.viewport.isXAxisBoundsManual = true
                    it.viewport.setMinX(0.0)
                    it.viewport.setMaxX(7.0)
                    it.viewport.setMinY(0.0)
                }
                if (!requestsPerWeekDay.isEmpty()) {
                    graph3.viewport.setMaxY(requestsPerWeekDay.values.max()!!.toDouble() + 5)
                }


                // set week day names as labels on x axis
                val weekStaticLabelsFormatter = StaticLabelsFormatter(graph3)
                weekStaticLabelsFormatter.setHorizontalLabels(
                    arrayOf(
                        "",
                        "Mon",
                        "Tue",
                        "Wed",
                        "Thu",
                        "Fri",
                        "Sat",
                        "Sun"
                    )
                )
                graph3.gridLabelRenderer.labelFormatter = weekStaticLabelsFormatter

            }
            "This Month" -> {

                //days where no requests were made should have zero datapoints
                val nowDayOfMonth = now.dayOfMonth
                val nowMonthOfYear = now.monthValue


                for (day in 1..nowDayOfMonth) {
                    if (!requestsPerDayOfMonth.containsKey(day)) {
                        requestsPerDayOfMonth[day] = 0
                    }
                }

                lineSeries = LineGraphSeries(requestsPerDayOfMonth.toSortedMap().map {
                    DataPoint(it.key.toDouble(), it.value.toDouble())
                }.toTypedArray()).apply {
                    isDrawDataPoints = true
                }

                val monthStaticLabelsFormatter = StaticLabelsFormatter(graph3)
                val dates = Array(31) { (it + 1).toString() + "/" +nowMonthOfYear }
                monthStaticLabelsFormatter.setHorizontalLabels(dates)
                graph3.gridLabelRenderer.labelFormatter = monthStaticLabelsFormatter
                graph3.gridLabelRenderer.numHorizontalLabels = 6

                graph3.let {
                    it.addSeries(lineSeries)
                    it.title = "Request counts throughout the month"
                    it.viewport.isXAxisBoundsManual = true
                    it.viewport.setMinX(0.0)
                    if(nowMonthOfYear == 1 || nowMonthOfYear ==3||nowMonthOfYear ==5||nowMonthOfYear ==7||nowMonthOfYear ==8|| nowMonthOfYear ==10 || nowMonthOfYear ==12){
                        it.viewport.setMaxX(30.0)
                    }else{
                        it.viewport.setMaxX(29.0)
                    }
                    it.viewport.setMinY(0.0)
                }
                if (!requestsPerDayOfMonth.isEmpty()) {
                    graph3.viewport.setMaxY(requestsPerDayOfMonth.values.max()!!.toDouble() + 5)
                }



            }

            "This Year" -> {

                //months where no requests were made should have zero datapoints
                val nowMonthOfYear = now.monthValue

                for (month in 1..nowMonthOfYear) {
                    if (!requestsPerMonth.containsKey(month)) {
                        requestsPerMonth[month] = 0
                    }
                }

                lineSeries = LineGraphSeries(requestsPerMonth.toSortedMap().map {
                    DataPoint(it.key.toDouble(), it.value.toDouble())
                }.toTypedArray()).apply {
                    isDrawDataPoints = true
                }

                graph3.let {
                    it.addSeries(lineSeries)
                    it.title = "Request counts throughout the year"
                    it.viewport.isXAxisBoundsManual = true
                    it.viewport.setMinX(0.0)
                    it.viewport.setMaxX(12.0)
                    it.viewport.setMinY(0.0)
                    //it.viewport.maxXAxisSize = 13.0
                }
                if (!requestsPerMonth.isEmpty()) {
                    graph3.viewport.setMaxY(requestsPerMonth.values.max()!!.toDouble() + 5)
                }

                // set months as labels on x axis
                val staticLabelsFormatter = StaticLabelsFormatter(graph3)
                staticLabelsFormatter.setHorizontalLabels(
                    arrayOf(
                        "",
                        "J",
                        "F",
                        "M",
                        "A",
                        "M",
                        "J",
                        "J",
                        "A",
                        "S",
                        "O",
                        "N",
                        "D"
                    )
                )
                graph3.gridLabelRenderer.labelFormatter = staticLabelsFormatter
                graph3.gridLabelRenderer.numHorizontalLabels = 13


            }

        }

    }



    //returns true to exact opening time and exact closing time
    private fun isInOpenHours(hour: Int, minutes: Int, dayOfWeek: Int): Boolean {

        val sharedPref =
            with(requireActivity()) { getSharedPreferences("prefs", Context.MODE_PRIVATE) }
        val requestOpeningHour = sharedPref.getInt(dayOfWeek.toString() + "OpenHour", 7)
        val requestOpeningMinute = sharedPref.getInt(dayOfWeek.toString() + "OpenMinute", 0)
        val requestClosingHour = sharedPref.getInt(dayOfWeek.toString() + "CloseHour", 21)
        val requestClosingMinute = sharedPref.getInt(dayOfWeek.toString() + "CloseMinute", 0)

        if (((hour == requestOpeningHour && minutes >= requestOpeningMinute) || (hour > requestOpeningHour)) && ((hour < requestClosingHour) || (hour == requestClosingHour && minutes <= requestClosingMinute))) {
            return true
        }
        return false
    }


    private fun timeToDecimal(hour: Int, minutes: Int): Double {
        return (hour + (minutes / 60.toDouble()))

    }

    private fun getSpacingAccordingToSize(size: Int): Int {
        when (size) {

            1 -> return 80
            2 -> return 70
        }
        return 30
    }


    companion object {
        @JvmStatic
        fun newInstance() = AnalyticsFragment()
    }


}

