package com.example.sophieleaver.dumbotapp

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDateTime
import java.time.ZoneOffset


/**
 * Displays information on the current session for the user
 *
 */
class CurrentSessionFragment : Fragment(){
    val database = FirebaseDatabase.getInstance()
    val ref = database.reference
    val fragTag = "CurrentSessionFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_current_session, container, false)
//        val currentBenchTextView : TextView = view.findViewById(R.id.textview_current_bench)
//        currentBenchTextView.text = currentBench.toString()

        val button : Button = view.findViewById(R.id.button_return_dumbbell)
        button.setOnClickListener {
            currentRequestExists = false // there is no longer a current request

            val now = LocalDateTime.now(ZoneOffset.UTC)
            val unixSeconds = now.atZone(ZoneOffset.UTC)?.toEpochSecond()
            val unixMilli = now.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

            //send request to firebase
            val request = ref.child("demo2").child("requests").child(unixMilli.toString())
            val benchID = convertBenchToID(currentBench)
            request.child("bench").setValue(benchID)
            request.child("time").setValue(unixSeconds)
            request.child("type").setValue("collecting")
            request.child("weight").setValue(currentDumbbellInUse)

            Log.d(fragTag, "Sending request $unixMilli to server (deliver dumbbells of ${currentDumbbellInUse}kg to bench $currentBench)")

            val orderFragment = OrderFragment.newInstance()
            (activity as MainActivity).openFragment(orderFragment)
        }
        return view
    }

    fun convertBenchToID(bench : Int) : String{
        var id = "B7"
        when (bench){
            2 -> id = "B10"
            3 -> id = "B13"
            4 -> id = "B9"
            5 -> id = "B12"
            6 -> id = "B15"
        }
        return id
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    companion object {
        @JvmStatic
        fun newInstance() = CurrentSessionFragment()
    }
}
