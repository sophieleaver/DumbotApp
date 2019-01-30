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
    val motorStatus : DatabaseReference = ref.child("motor").child("status")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {
        val view = inflater.inflate(R.layout.fragment_demo_fragment, container, false)
        val b1 : Button = view!!.findViewById(R.id.button_start_motor)
        val b2 : Button = view!!.findViewById(R.id.button_stop_motor)
        b1.setOnClickListener(this)
        b2.setOnClickListener(this)
        // Inflate the layout for this fragment

        return view
    }

    override fun onClick(v: View?) {
        when (v?.id){
            R.id.button_start_motor -> {
                motorStatus.setValue("ON")
                Log.d(demoTag,"Motor status set to start")
            }
            R.id.button_stop_motor -> {
                motorStatus.setValue("OFF")
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
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment DemoFragmenty.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            DemoFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
                }
            }
    }

