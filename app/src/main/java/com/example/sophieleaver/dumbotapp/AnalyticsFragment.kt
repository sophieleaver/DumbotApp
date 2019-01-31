package com.example.sophieleaver.dumbotapp

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.sophieleaver.dumbotapp.R.styleable.Spinner
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.ValueDependentColor
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

//TODO create a login
class AnalyticsFragment : Fragment() {

    //stores the datapoints for the bargraphs
    private var mSeries1: BarGraphSeries<DataPoint>? = null
    private var mSeries2: BarGraphSeries<DataPoint>? = null
    private var mSeries3: BarGraphSeries<DataPoint>? = null
    private var mSeries4: BarGraphSeries<DataPoint>? = null
    private var mSeries5: BarGraphSeries<DataPoint>? = null
    private var mSeries6: BarGraphSeries<DataPoint>? = null

    //variable if we want to make hourly updates on dumbell and station usages
    private var time = 6


    private val TAG = "AnalyticsActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_analytics, container, false)

        //create spinner to hold selection box for period of time for graph
        val dropdown : Spinner = view.findViewById(R.id.spinner1)
        val items : Array<String> = arrayOf("Last Week", "Last Month", "Last Year")
        val adapter : ArrayAdapter<String> = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items)
        dropdown.adapter = adapter


        //the two bargraphs displayed on the screen
        val graph1 = view.findViewById(R.id.graph1) as? GraphView
        val graph2 = view.findViewById(R.id.graph2) as? GraphView


        // Set an on item selected listener for spinner object
        dropdown.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{

            override fun onItemSelected(parent:AdapterView<*>, view: View, position: Int, id: Long){

                //show last week analytics
                if(parent.getItemAtPosition(position).toString().equals("Last Week")){




                    //---------------------weekly dumbell counts-----------------------




                    graph1?.removeAllSeries()
                    if(mSeries1 == null){
                        mSeries1 = BarGraphSeries(
                            arrayOf(

                                //should be initialised to zero every day
                                //should be imported from firestore
                                //updated everytime a request is issued in the requests page
                                DataPoint(5.0, 12.0),
                                DataPoint(10.0, 14.0),
                                DataPoint(15.0, 17.0),
                                DataPoint(20.0, 11.0),
                                DataPoint(25.0, 12.0)
                            )
                        )
                    }

                    graph1?.addSeries(mSeries1)
                    graph1?.title = "Request counts for each dumbbell last week"

                    // styling
                    mSeries1!!.valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(
                            data.x.toInt() * 255 / 4,
                            Math.abs(data.y * 255 / 6).toInt(),
                            100
                        )
                    }

                    mSeries1!!.setSpacing(20)

                    // draw values on top
                    mSeries1!!.setDrawValuesOnTop(true)
                    mSeries1!!.valuesOnTopColor = Color.RED
                    //series.setValuesOnTopSize(50);





                    // ----------------weekly station counts------------------------



                    graph2?.removeAllSeries()
                    if(mSeries2 == null){
                        mSeries2 = BarGraphSeries(
                            arrayOf(

                                //should be initialised to zero every day
                                //should be imported from firestore
                                //updated everytime a request is issued in the requests page
                                DataPoint(1.0, 12.0),
                                DataPoint(2.0, 14.0),
                                DataPoint(3.0, 17.0),
                                DataPoint(4.0, 11.0),
                                DataPoint(5.0, 12.0)
                            )
                        )
                    }

                    graph2?.addSeries(mSeries2)
                    graph2?.title = "Request counts for each workout station last week"

                    // styling
                    mSeries2!!.valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(
                            data.x.toInt() * 255 / 4,
                            Math.abs(data.y * 255 / 6).toInt(),
                            100
                        )
                    }

                    mSeries2!!.spacing = 20

                    // draw values on top
                    mSeries2!!.setDrawValuesOnTop(true)
                    mSeries2!!.setValuesOnTopColor(Color.RED)
                    //series.setValuesOnTopSize(50);


                }
                //show last months analytics
                else if(parent.getItemAtPosition(position).toString().equals("Last Month")){





                    //-----------------monthly dumbell counts---------------------------




                    graph1?.removeAllSeries()
                    if(mSeries3 == null) {
                        mSeries3 = BarGraphSeries(
                            arrayOf(

                                //should be initialised to zero every month
                                //should be imported from firestore
                                //updated everytime a request is issued in the requests page
                                DataPoint(5.0, 120.0),
                                DataPoint(10.0, 140.0),
                                DataPoint(15.0, 170.0),
                                DataPoint(20.0, 110.0),
                                DataPoint(25.0, 120.0)
                            )
                        )
                    }
                    graph1?.addSeries(mSeries3)
                    graph1?.title = "Request counts for each dumbbell last month"

                    // styling
                    mSeries3!!.valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(
                            data.x.toInt() * 255 / 4,
                            Math.abs(data.y * 255 / 6).toInt(),
                            100
                        )
                    }

                    mSeries3!!.spacing = 20

                    // draw values on top
                    mSeries3!!.setDrawValuesOnTop(true)
                    mSeries3!!.valuesOnTopColor = Color.RED
                    //series.setValuesOnTopSize(50);




                    //-----------------monthly station counts---------------------------



                    graph2?.removeAllSeries()
                    if(mSeries4 == null) {
                        mSeries4 = BarGraphSeries(
                            arrayOf(

                                //should be initialised to zero every month
                                //should be imported from firestore
                                //updated everytime a request is issued in the requests page
                                DataPoint(1.0, 120.0),
                                DataPoint(2.0, 140.0),
                                DataPoint(3.0, 170.0),
                                DataPoint(4.0, 110.0),
                                DataPoint(5.0, 120.0)
                            )
                        )
                    }
                    graph2?.addSeries(mSeries4)
                    graph2?.title = "Request counts for each workout station last month"

                    // styling
                    mSeries4!!.valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(
                            data.x.toInt() * 255 / 4,
                            Math.abs(data.y * 255 / 6).toInt(),
                            100
                        )
                    }

                    mSeries4!!.spacing = 20

                    // draw values on top
                    mSeries4!!.setDrawValuesOnTop(true)
                    mSeries4!!.setValuesOnTopColor(Color.RED)
                    //series.setValuesOnTopSize(50);

                }

                //show last years analytics
                else if(parent.getItemAtPosition(position).toString().equals("Last Year")){




                    //-----------------yearly dumbell counts---------------------------



                    graph1?.removeAllSeries()
                    if(mSeries5 == null) {
                        mSeries5 = BarGraphSeries(
                            arrayOf(

                                //should be initialised to zero every year
                                //should be imported from firestore
                                //updated everytime a request is issued in the requests page
                                DataPoint(5.0, 1200.0),
                                DataPoint(10.0, 1400.0),
                                DataPoint(15.0, 1700.0),
                                DataPoint(20.0, 1100.0),
                                DataPoint(25.0, 1200.0)
                            )
                        )
                    }
                    graph1?.addSeries(mSeries5)
                    graph1?.title = "Request counts for each dumbbell last year"

                    // styling
                    mSeries5!!.valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(
                            data.x.toInt() * 255 / 4,
                            Math.abs(data.y * 255 / 6).toInt(),
                            100
                        )
                    }

                    mSeries5!!.spacing = 20

                    // draw values on top
                    mSeries5!!.isDrawValuesOnTop = true
                    mSeries5!!.valuesOnTopColor = Color.RED
                    //series.setValuesOnTopSize(50);






                    //-------------------yearly station counts-----------------------------



                    graph2?.removeAllSeries()
                    if(mSeries6 == null) {
                        mSeries6 = BarGraphSeries(
                            arrayOf(

                                //should be initialised to zero every year
                                //should be imported from firestore
                                //updated everytime a request is issued in the requests page
                                DataPoint(1.0, 1200.0),
                                DataPoint(2.0, 1400.0),
                                DataPoint(3.0, 1700.0),
                                DataPoint(4.0, 1100.0),
                                DataPoint(5.0, 1200.0)
                            )
                        )
                    }

                    graph2?.addSeries(mSeries6)
                    graph2?.title = "Request counts for each workout station last year"

                    // styling
                    mSeries6!!.valueDependentColor = ValueDependentColor { data ->
                        Color.rgb(
                            data.x.toInt() * 255 / 4,
                            Math.abs(data.y * 255 / 6).toInt(),
                            100
                        )
                    }

                    mSeries6!!.spacing = 20

                    // draw values on top
                    mSeries6!!.isDrawValuesOnTop = true
                    mSeries6!!.valuesOnTopColor = Color.RED
                    //series.setValuesOnTopSize(50);
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>){
                // Another interface callback

            }
        }



        return view
    }


    companion object {
        @JvmStatic
        fun newInstance() = AnalyticsFragment()
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }
}
