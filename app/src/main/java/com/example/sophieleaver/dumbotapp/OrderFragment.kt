package com.example.sophieleaver.dumbotapp


import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_order_weights.view.*
import kotlinx.android.synthetic.main.item_order_dumbbell.view.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.jetbrains.anko.find
import org.jetbrains.anko.toast
import java.lang.reflect.Array.get
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


private const val WEIGHTS = "WEIGHTS"
private const val STATIONS = "STATIONS"

/**
 * A simple [Fragment] subclass.
 * Use the [OrderFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class OrderFragment : Fragment(){

    private val database = FirebaseDatabase.getInstance()
    private val ref = database.reference

    private var weightList = ref.child("demo2").child("weights").orderByKey()

    private var weights: List<Any>? = null //= listOf("12kg", "14kg", "16kg", "18kg", "20kg", "22kg")
    private var stations: List<Int>? = null //= listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    private var orderDumbbellRecyclerView: RecyclerView? = null

    private val fragTag = "OrderFragment"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*arguments?.let {
            weights = it.getParcelableArrayList(WEIGHTS)
            stations = it.getStringArrayList(STATIONS)
        }*/

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(fragTag, "HELLO")
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_order_weights, container, false)

        weightList.addValueEventListener( object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(dataSnapshot : DataSnapshot) {
                for ( foo : DataSnapshot in dataSnapshot.children){
                    val weightValue = foo.getValue()
                    Log.d(fragTag, "${weightValue}")

                }
            }
        })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(fragTag, "HELLO")
        with(view) {
            orderDumbbellRecyclerView = order_dumbbell_list

            //when button pressed, alert dialog opened to change the bench
            btn_change_station.setOnClickListener {
                val builder = AlertDialog.Builder(context)
                val alertView = layoutInflater.inflate(R.layout.change_bench_layout, null)
                builder.setView(alertView)
                builder.setTitle("Change Workout Station")
                builder.setNegativeButton("Cancel"){dialog, _ -> dialog.cancel()} //if cancel then dialog is closed
                val dialog : AlertDialog = builder.create()
                dialog.show()

                //on click listeners for when a new bench is selected
                val b1 : Button = alertView.findViewById(R.id.button_bench_1)
                b1.setOnClickListener{ changeBench(1,dialog)}
                val b2 : Button = alertView.findViewById(R.id.button_bench_2)
                b2.setOnClickListener{ changeBench(2,dialog)}
                val b3 : Button = alertView.findViewById(R.id.button_bench_3)
                b3.setOnClickListener{ changeBench(3,dialog)}
                val b4 : Button = alertView.findViewById(R.id.button_bench_4)
                b4.setOnClickListener{ changeBench(4,dialog)}
                val b5 : Button = alertView.findViewById(R.id.button_bench_5)
                b5.setOnClickListener{ changeBench(5,dialog)}
                val b6 : Button = alertView.findViewById(R.id.button_bench_6)
                b6.setOnClickListener{ changeBench(6,dialog)}
            }
        }
        getWeightData()
    }

    fun changeBench(bench: Int, dialog: AlertDialog){ //function to change bench when button is clicked
        currentBench = bench
        val text : TextView = view!!.findViewById(R.id.textview_current_bench)
        text.text = currentBench.toString()
        dialog.cancel()
        Log.d(fragTag, "currentBench = $currentBench, set bench is $bench")
    }

    private fun getWeightData() {
        weights = listOf(0.5, 1, 1.5)
        stations = listOf(1, 2, 3, 4, 5, 6)

        orderDumbbellRecyclerView!!.layoutManager =
                LinearLayoutManager(this@OrderFragment.requireContext())
        orderDumbbellRecyclerView!!.adapter = DumbbellRequestAdapter()
    }


    companion object {
        @JvmStatic
        fun newInstance() = OrderFragment()
    }

    inner class DumbbellRequestAdapter : RecyclerView.Adapter<DumbbellRequestAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(this@OrderFragment.requireContext())
                    .inflate(R.layout.item_order_dumbbell, parent, false)
            )

        override fun getItemCount(): Int = weights!!.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.apply {
                val requestedWeight = weights!![position]
                weightValue.text = requestedWeight.toString() + "kg"

                val numAvailable = 1
                val totalUnits = 2

                if (numAvailable == totalUnits){
                    //weight is unavailable
                    orderButton.setOnClickListener {
                        this@OrderFragment.requireActivity()
                            .toast("Joined Weight Queue for $requestedWeight dumbbell")
                    }
                    orderButton.text = getString(R.string.join_wait_queue)
                    orderButton.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.colorDumbbellUnavailable
                        )
                    )
                    available.text = getString(R.string.unavailable)
                    val queueLength = 2
                    availabilityInfo.text = getString( R.string.wait_queue_info, totalUnits, queueLength)
                }
                else { //weight is available
                    available.text = getString(R.string.available)

                    orderButton.setOnClickListener {
                        this@OrderFragment.requireActivity()
                            .toast("Requested $requestedWeight dumbbell")

                        val requestID  = DateTimeFormatter.ofPattern("HH,mm,ss dd,MM,yy")
                        val formattedID = LocalDateTime.now().format(requestID)
                        val requestTime  = DateTimeFormatter.ofPattern("HHmmss")
                        val formattedTime = LocalDateTime.now().format(requestTime)

                        var request = ref.child("demo2").child("requests").child(formattedID)
                        request.child("bench").setValue(currentBench)
                        request.child("time").setValue(formattedTime) // TODO set to time of request
                        request.child("type").setValue("collect")
                        request.child("weight").setValue(requestedWeight) // TODO get weight from current weight
                    }

                    availabilityInfo.text = getString(R.string.available_dumbbells_info, numAvailable, totalUnits)
                }
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val weightValue: TextView = view.text_weight_value
            val available: TextView = view.text_available
            val availabilityInfo: TextView = view.text_wait_queue
            val orderButton: Button = view.btn_order_dumbbell
        }
    }
}
