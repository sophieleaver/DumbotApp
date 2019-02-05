package com.example.sophieleaver.dumbotapp

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.renderscript.Sampler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View 
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.firebase.database.*
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
    val alert = false

    val movementStatus = ref.child("demo1").child("cur_cmd")
    val alertStatus = ref.child("demo1").child("alert")

//    val database = FirebaseFirestore.getInstance()
//    val ref = database.collection("demo1").document("motor")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {
        // inflate layout for demo fragment
        val view = inflater.inflate(R.layout.fragment_demo_fragment, container, false)

        val b1 : Button = view.findViewById(R.id.button_start_motor)
        b1.setOnClickListener(this)
        val b2 : Button = view.findViewById(R.id.button_stop_motor)
        b2.setOnClickListener(this)
        val b3 : Button = view.findViewById(R.id.button_reset_motor)
        b3.setOnClickListener(this)

        alertStatus.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = dataSnapshot.getValue(String::class.java)
                if (value.equals("True")){
                    Toast.makeText(context, "WARNING: Robot is obstructed from path", Toast.LENGTH_LONG).show()
                    b3.isEnabled = true
                    b2.isEnabled = false
                    b1.isEnabled = false
                }
                if (value.equals("False")){
                    b3.isEnabled = false
                    b1.isEnabled = true
                    b2.isEnabled = true
                }
                Log.d(demoTag, "Value is: " + value!!)

            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(demoTag, "Failed to read value from database.", error.toException())
            }
        })

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

            R.id.button_reset_motor -> {
                movementStatus.setValue("h")
                alertStatus.setValue("False")
                Log.d(demoTag, "Motor has been reset")
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

