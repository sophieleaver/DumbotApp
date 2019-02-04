package com.example.sophieleaver.dumbotapp

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


/**
 * A simple [Fragment] subclass.
 * Demo subclass for Demo 1
 * Contains simple buttons to control the robot.
 *
 */
class DemoFragment : Fragment(), View.OnClickListener {
    private val demoTag = "DemoFragment"
    private val database = FirebaseDatabase.getInstance()
    private val ref = database.reference
    private val motorStatus : DatabaseReference = ref.child("motor").child("status")


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {
        // inflate layout for demo fragment
        val view = inflater.inflate(R.layout.fragment_demo_fragment, container, false)

        val b1 : Button = view!!.findViewById(R.id.button_start_motor)
        b1.setOnClickListener(this)
        val b2 : Button = view.findViewById(R.id.button_stop_motor)
        b2.setOnClickListener(this)

        return view
    }

    override fun onClick(v: View?) {
        when (v?.id){
            R.id.button_start_motor -> { //on clicking START button the database sets the motor status to ON
                motorStatus.setValue("ON")
                Log.d(demoTag,"Motor status set to start")
            }

            R.id.button_stop_motor -> { //on clicking STOP button the database sets the motor status to OFF
                motorStatus.setValue("OFF")
                Log.d(demoTag,"Motor status set to stop")
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = DemoFragment()
    }
}

