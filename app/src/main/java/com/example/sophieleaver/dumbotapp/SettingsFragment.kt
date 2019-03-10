package com.example.sophieleaver.dumbotapp

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button


class SettingsFragment : Fragment() {
    val fragTag = "SettingsFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        //when button pressed, alert dialog opened to change the bench
        val button : Button = view.findViewById(R.id.button_change_bench)
        button.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            val alertView = layoutInflater.inflate(R.layout.change_bench_layout, null)
            builder.setView(alertView)
            builder.setTitle("Change Workout Station")
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() } //if cancel then dialog is closed
            val dialog: AlertDialog = builder.create()
            dialog.show()

            //on click listeners for when a new bench is selected
            val b1: Button = alertView.findViewById(R.id.button_bench_1)
            b1.setOnClickListener { changeBench(1, dialog) }
            val b2: Button = alertView.findViewById(R.id.button_bench_2)
            b2.setOnClickListener { changeBench(2, dialog) }
            val b3: Button = alertView.findViewById(R.id.button_bench_3)
            b3.setOnClickListener { changeBench(3, dialog) }
            val b4: Button = alertView.findViewById(R.id.button_bench_4)
            b4.setOnClickListener { changeBench(4, dialog) }
            val b5: Button = alertView.findViewById(R.id.button_bench_5)
            b5.setOnClickListener { changeBench(5, dialog) }
            val b6: Button = alertView.findViewById(R.id.button_bench_6)
            b6.setOnClickListener { changeBench(6, dialog) }
        }

        return view
    }

    private fun changeBench(bench: Int, dialog: AlertDialog) {
        currentBench = bench
        dialog.cancel()
        Log.d(fragTag, "currentBench = $currentBench, set bench is $bench")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}
