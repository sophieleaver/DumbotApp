package com.example.sophieleaver.dumbotapp


import android.content.Intent
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
import kotlinx.android.synthetic.main.fragment_order_weights.view.*
import kotlinx.android.synthetic.main.item_order_dumbbell.*
import kotlinx.android.synthetic.main.item_order_dumbbell.view.*
import org.jetbrains.anko.toast
import com.example.sophieleaver.dumbotapp.R
import java.lang.Boolean.TRUE
import kotlin.random.Random


private const val WEIGHTS = "WEIGHTS"
private const val STATIONS = "STATIONS"

/**
 * A simple [Fragment] subclass.
 * Use the [OrderFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class OrderFragment : Fragment() {

    private var weights: List<String>? = listOf("12kg", "14kg", "16kg", "18kg", "20kg", "22kg")
    private var stations: List<Int>? = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    private var orderDumbbellRecyclerView: RecyclerView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*arguments?.let {
            weights = it.getParcelableArrayList(WEIGHTS)
            stations = it.getStringArrayList(STATIONS)
        }*/

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_order_weights, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(view) {
            orderDumbbellRecyclerView = order_dumbbell_list
            btn_change_station.setOnClickListener {
                this@OrderFragment.requireActivity().toast("Change Station")
            }
        }
        getWeightData()
    }

    private fun getWeightData() {
        weights = listOf(
            "12kg",
            "14kg",
            "16kg",
            "18kg",
            "20kg",
            "22kg",
            "24kg",
            "26kg",
            "28kg",
            "30kg"
        )
        stations = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        orderDumbbellRecyclerView!!.layoutManager =
                LinearLayoutManager(this@OrderFragment.requireContext())
        orderDumbbellRecyclerView!!.adapter = DumbbellRequestAdapter()
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment OrderFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
                /*fun newInstance(param1: String, param2: String) =
                    OrderFragment().apply {
                        arguments = Bundle().apply {
                            putString(WEIGHTS, param1)
                            putString(STATIONS, param2)
                        }
                    }*/

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
                weightValue.text = requestedWeight

                //dumbbell is unavailable
                if ((position == 2) or (position == 5) or (position == 9)) {
                    orderButton.setOnClickListener {
                        this@OrderFragment.requireActivity()
                            .toast("Joined Weight Queue for $requestedWeight dumbbell")
                        //create request and add to firebase
                        //state of request should be IN_QUEUE
                        //show wait queue page
                        val intent = Intent(context, ActiveSession::class.java);
                        intent.putExtra("Queue", true)
                        startActivity(intent);
                    }
                    orderButton.text = getString(R.string.join_wait_queue)
                    orderButton.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.colorDumbbellUnavailable
                        )
                    )
                    available.text = getString(R.string.unavailable)
                    availabilityInfo.text = getString(
                        R.string.wait_queue_info,
                        Random.nextInt(1, 10),
                        Random.nextInt(1, 10)
                    )
                }
                //dumbell is available
                else {
                    available.text = getString(R.string.available)
                    orderButton.setOnClickListener {
                        this@OrderFragment.requireActivity()
                            .toast("Requested $requestedWeight dumbbell")

                        //create request and add to firebase
                        //state of request should be WAITING_FOR_DELIVERY
                        //show "your weights are being delivered" page
                        val intent = Intent(context, ActiveSession::class.java);
                        intent.putExtra("Queue", false)
                        startActivity(intent);
                    }
                    val numAvailable = Random.nextInt(1, 10)
                    availabilityInfo.text = getString(
                        R.string.available_dumbbells_info,
                        numAvailable,
                        Random.nextInt(numAvailable, 10)
                    )
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
