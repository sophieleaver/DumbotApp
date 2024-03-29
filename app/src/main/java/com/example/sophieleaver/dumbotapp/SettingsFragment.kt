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
import kotlinx.android.synthetic.main.change_opening_hours.view.*
import kotlinx.android.synthetic.main.change_opening_hours_spinners.view.*
import kotlinx.android.synthetic.main.fragment_current_orders.*
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.fragment_settings.view.*

@SuppressLint("InflateParams")
class SettingsFragment : Fragment() {
    private val fragTag = "SettingsFragment"


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

        view.button_change_opening_hours.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            val alertView = layoutInflater.inflate(R.layout.change_opening_hours, null)
            builder.setView(alertView)
            builder.setTitle("Change Opening Hours")
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            val dialog: AlertDialog = builder.create()
            dialog.show()

            with(alertView) {
                button_monday.setOnClickListener { changeWorkingHours(1, dialog, view) }
                button_tuesday.setOnClickListener { changeWorkingHours(2, dialog, view) }
                button_wednesday.setOnClickListener { changeWorkingHours(3, dialog, view) }
                button_thursday.setOnClickListener { changeWorkingHours(4, dialog, view) }
                button_friday.setOnClickListener { changeWorkingHours(5, dialog, view) }
                button_saturday.setOnClickListener { changeWorkingHours(6, dialog, view) }
                button_sunday.setOnClickListener { changeWorkingHours(7, dialog, view) }
            }
        }

        //get opening hours from sharedprefs
        val sharedPref =
            with(requireActivity()) { getSharedPreferences("prefs", Context.MODE_PRIVATE) }

        for (i in 1..7) {
            val openingHour = sharedPref.getInt(i.toString() + "OpenHour", 7)
            val openingMinute = sharedPref.getInt(i.toString() + "OpenMinute", 0)
            val closingHour = sharedPref.getInt(i.toString() + "CloseHour", 21)
            val closingMinute = sharedPref.getInt(i.toString() + "CloseMinute", 0)

            setText(i, openingHour, openingMinute, closingHour, closingMinute, view)

        }

        view.button_complete_reset.setOnClickListener {//removes all requests for robot in case for complete testing
            val builder = AlertDialog.Builder(context)
            builder.apply {
                setTitle("Are you sure you would like to completely reset the app?")
                setMessage("Completely resetting the app means all current dumbbells will be picked up and these requests will be hidden from the user")
                setPositiveButton("CONFIRM"){dialog, which ->
                    requests = HashMap<String,Request>()
                    (activity as MainActivity).showOrderFragment()
                    dialog.cancel()
                }
                setNegativeButton("CANCEL"){_,_ ->}
                create().show()
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
                .putString("bench", currentBench)
                .apply()
            dialog.cancel()
            Log.d(fragTag, "currentBench = $currentBench")
        }
    }

    //    todo - refactor
    private fun changeWorkingHours(day: Int, dialog: AlertDialog, view: View) {
        dialog.cancel()
        val builder = AlertDialog.Builder(context)
        val alertView = layoutInflater.inflate(R.layout.change_opening_hours_spinners, null)
        builder.setView(alertView)
        builder.setTitle("Change Opening Hours")
        builder.setNegativeButton("Cancel") { newDialog, _ -> newDialog.cancel() }
        val newDialog: AlertDialog = builder.create()
        newDialog.show()

        alertView.open_hour.minValue = 0
        alertView.open_hour.maxValue = 23

        alertView.open_minute.minValue = 0
        alertView.open_minute.maxValue = 59

        alertView.closing_hour.minValue = 0
        alertView.closing_hour.maxValue = 23

        alertView.closing_minute.minValue = 0
        alertView.closing_minute.maxValue = 59

        var openHour = 0
        var openMinute = 0
        var closingHour = 0
        var closingMinute = 0

        alertView.open_hour.setOnValueChangedListener { _, _, newVal ->
            openHour = newVal
        }
        alertView.open_minute.setOnValueChangedListener { _, _, newVal ->
            openMinute = newVal
        }
        alertView.closing_hour.setOnValueChangedListener { _, _, newVal ->
            closingHour = newVal
        }
        alertView.closing_minute.setOnValueChangedListener { _, _, newVal ->
            closingMinute = newVal
        }

        //when done button is pressed add time to sharedpref and update the display in settings
        alertView.button_done.setOnClickListener {
            //check if closing time is before opening time
            if (closingHour < openHour || closingHour == openHour && closingMinute < openMinute) {
                val builder3 = AlertDialog.Builder(context)
                builder3.setMessage("Error: closing time needs to be after opening time")
                builder3.setPositiveButton("OK") { dialog3, _ -> dialog3.cancel() }
                val dialog3: AlertDialog = builder3.create()
                dialog3.show()
            } else {
                //update the display in settings
                setText(day, openHour, openMinute, closingHour, closingMinute, view)
                //put open hour and minutes in shared preferences

                with(requireActivity()) {
                    getSharedPreferences("prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("${day}OpenHour", openHour)
                        .putInt("${day}OpenMinute", openMinute)
                        .putInt("${day}CloseHour", closingHour)
                        .putInt("${day}CloseMinute", closingMinute)
                        .apply()
                }

                val builder2 = AlertDialog.Builder(context)
                builder2.setMessage("Opening hours will be updated next time the app is started")
                builder2.setPositiveButton("OK") { dialog2, _ -> dialog2.cancel() }
                val dialog2: AlertDialog = builder2.create()
                dialog2.show()
                newDialog.cancel()
            }

        }

    }

    @SuppressLint("SetTextI18n")
    private fun setText(
        day: Int,
        openHour: Int,
        openMinute: Int,
        closingHour: Int,
        closingMinute: Int,
        view: View
    ) {
        val days: Map<Int, String> = mapOf(
            1 to "Monday", 2 to "Tuesday", 3 to "Wednesday",
            4 to "Thursday", 5 to "Friday", 6 to "Saturday", 7 to "Sunday"
        )

        when (day) {
            1 -> {
                if (openMinute < 10 && closingMinute < 10) {
                    view.text_monday.text =
                        "Monday: " + openHour + ":0" + openMinute + " to " + closingHour + ":0" + closingMinute

                } else if (closingMinute < 10) {
                    view.text_monday.text =
                        "Monday: " + openHour + ":" + openMinute + " to " + closingHour + ":0" + closingMinute

                } else if (openMinute < 10) {
                    view.text_monday.text =
                        "Monday: " + openHour + ":0" + openMinute + " to " + closingHour + ":" + closingMinute

                }
            }
            2 -> {
                if (openMinute < 10 && closingMinute < 10) {
                    view.text_tuesday.text =
                        "Tuesday: " + openHour + ":0" + openMinute + " to " + closingHour + ":0" + closingMinute

                } else if (closingMinute < 10) {
                    view.text_tuesday.text =
                        "Tuesday: " + openHour + ":" + openMinute + " to " + closingHour + ":0" + closingMinute

                } else if (openMinute < 10) {
                    view.text_tuesday.text =
                        "Tuesday: " + openHour + ":0" + openMinute + " to " + closingHour + ":" + closingMinute

                }
            }
            3 -> {
                if (openMinute < 10 && closingMinute < 10) {
                    view.text_wednesday.text =
                        "Wednesday: " + openHour + ":0" + openMinute + " to " + closingHour + ":0" + closingMinute

                } else if (closingMinute < 10) {
                    view.text_wednesday.text =
                        "Wednesday: " + openHour + ":" + openMinute + " to " + closingHour + ":0" + closingMinute

                } else if (openMinute < 10) {
                    view.text_wednesday.text =
                        "Wednesday: " + openHour + ":0" + openMinute + " to " + closingHour + ":" + closingMinute

                }
            }
            4 -> {
                if (openMinute < 10 && closingMinute < 10) {
                    view.text_thursday.text =
                        "Thursday: " + openHour + ":0" + openMinute + " to " + closingHour + ":0" + closingMinute

                } else if (closingMinute < 10) {
                    view.text_thursday.text =
                        "Thursday: " + openHour + ":" + openMinute + " to " + closingHour + ":0" + closingMinute

                } else if (openMinute < 10) {
                    view.text_thursday.text =
                        "Thursday: " + openHour + ":0" + openMinute + " to " + closingHour + ":" + closingMinute

                }
            }
            5 -> {
                if (openMinute < 10 && closingMinute < 10) {
                    view.text_friday.text =
                        "Friday: " + openHour + ":0" + openMinute + " to " + closingHour + ":0" + closingMinute

                } else if (closingMinute < 10) {
                    view.text_friday.text =
                        "Friday: " + openHour + ":" + openMinute + " to " + closingHour + ":0" + closingMinute

                } else if (openMinute < 10) {
                    view.text_friday.text =
                        "Friday: " + openHour + ":0" + openMinute + " to " + closingHour + ":" + closingMinute

                }
            }
            6 -> {
                if (openMinute < 10 && closingMinute < 10) {
                    view.text_saturday.text =
                        "Saturday: " + openHour + ":0" + openMinute + " to " + closingHour + ":0" + closingMinute

                } else if (closingMinute < 10) {
                    view.text_saturday.text =
                        "Saturday: " + openHour + ":" + openMinute + " to " + closingHour + ":0" + closingMinute

                } else if (openMinute < 10) {
                    view.text_saturday.text =
                        "Saturday: " + openHour + ":0" + openMinute + " to " + closingHour + ":" + closingMinute

                }
            }
            7 -> {
                if (openMinute < 10 && closingMinute < 10) {
                    view.text_sunday.text =
                        "Sunday: $openHour:0$openMinute to $closingHour:0$closingMinute"

                } else if (closingMinute < 10) {
                    view.text_sunday.text =
                        "Sunday: $openHour:$openMinute to $closingHour:0$closingMinute"

                } else if (openMinute < 10) {
                    view.text_sunday.text =
                        "Sunday: $openHour:0$openMinute to $closingHour:$closingMinute"

                }
            }
        }
    }


    private fun benchNumberToFirebaseID(bench: Int): String =
        when (bench) {
            1 -> "B7"; 2 -> "B10"; 3 -> "B13"
            4 -> "B9"; 5 -> "B12"; else -> "B15"
        }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}
