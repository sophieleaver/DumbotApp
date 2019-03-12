package com.example.sophieleaver.dumbotapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.change_bench_layout.view.*
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.fragment_settings.view.*
import org.jetbrains.anko.toast


class SettingsFragment : Fragment() {
    private val fragTag = "SettingsFragment"

    @SuppressLint("InflateParams")
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        view.text_current_bench.text = currentBench

        //when button pressed, alert dialog opened to change the bench
        view.button_change_bench.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            val alertView = layoutInflater.inflate(R.layout.change_bench_layout, null)
            builder.setView(alertView)
            builder.setTitle("Change Workout Station")
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            val dialog: AlertDialog = builder.create()
            dialog.show()

            //on click listeners for when a new bench is selected
            with(alertView) {
                button_bench_1.setOnClickListener { changeBench(1, dialog) }
                button_bench_2.setOnClickListener { changeBench(2, dialog) }
                button_bench_3.setOnClickListener { changeBench(3, dialog) }
                button_bench_4.setOnClickListener { changeBench(4, dialog) }
                button_bench_5.setOnClickListener { changeBench(5, dialog) }
                button_bench_6.setOnClickListener { changeBench(6, dialog) }
            }

        }

        return view
    }

    private fun changeBench(bench: Int, dialog: AlertDialog) {
        currentBench = benchNumberToFirebaseID(bench)
        text_current_bench.text = currentBench
        with(requireActivity()) {
            getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("bench", bench)
                .apply()
            dialog.cancel()
            Log.d(fragTag, "currentBench = $currentBench, set bench is $bench")
            toast("Updated to Bench #$bench")
        }
    }

    private fun benchNumberToFirebaseID(bench: Int): String = when (bench) {
        1 -> "B7"; 2 -> "B10"; 3 -> "B13"
        4 -> "B9"; 5 -> "B12"; else -> "B15"
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}
