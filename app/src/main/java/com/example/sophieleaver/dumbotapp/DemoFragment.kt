package com.example.sophieleaver.dumbotapp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_demo_fragment.view.*


/**
 * A simple [Fragment] subclass.
 * Demo subclass for Demo 1
 * Contains simple buttons to control the robot.
 *
 */
class DemoFragment : Fragment(), View.OnClickListener {
    val demoTag = "DemoFragment"
    val database = FirebaseDatabase.getInstance()
    val ref = database.getReference()
    val movementStatus = ref.child("demo1").child("cur_cmd")
//    val database = FirebaseFirestore.getInstance()
//    val ref = database.collection("demo1").document("motor")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {
        // inflate layout for demo fragment
        val view = inflater.inflate(R.layout.fragment_demo_fragment, container, false)

        val b1 : Button = view!!.findViewById(R.id.button_start_motor)
        b1.setOnClickListener(this)
        val b2 : Button = view!!.findViewById(R.id.button_stop_motor)
        b2.setOnClickListener(this)

        return view
    }

    override fun onClick(v: View?) {
        when (v?.id){
            R.id.button_start_motor -> { //on clicking START button the database sets the motor status to ON
//                ref.update("motor_is_on", true)
                movementStatus.setValue("g")
                Log.d(demoTag,"Motor status set to start")
            }

            R.id.button_stop_motor -> { //on clicking STOP button the database sets the motor status to OFF
//                ref.update("motor_is_on", false)
                movementStatus.setValue("h")
                Log.d(demoTag,"Motor status set to stop")
            }
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }


    companion object {
        @JvmStatic
        fun newInstance() = DemoFragment()
    }
}

