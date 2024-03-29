package com.example.sophieleaver.dumbotapp

import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_timer.*
import kotlinx.android.synthetic.main.fragment_timer.view.*
import java.util.*


class TimerFragment : Fragment() {

    companion object {

        fun setAlarm(nowSeconds: Long, secondsRemaining: Long): Long {
            return (nowSeconds + secondsRemaining) * 1000
        }

        fun newInstance() = TimerFragment()

        val nowSeconds: Long
            get() = Calendar.getInstance().timeInMillis / 1000
    }

    enum class TimerState {
        Stopped, Paused, Running
    }

    /*enum class PagerState {
        Timer, Collection
    }*/


    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds: Long = 0
    private var timerState = TimerState.Stopped

    private var secondsRemaining: Long = 0

//    private var page: PagerState = PagerState.Timer


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_timer, container, false)

        view.textView_countdown.bringToFront()
        view.textView_countdown.visibility = View.INVISIBLE
        view.progress_countdown.visibility = View.INVISIBLE


        //Number picker

        view.input_total_stock.minValue = 0
        view.input_total_stock.maxValue = 60
        view.input_total_stock.value = 1


        view.input_total_stock.setOnValueChangedListener { _, _, newVal ->

            if (newVal.toString() != "") {
                timer.cancel()
                timerState = TimerState.Stopped
                timerLengthSeconds = newVal * 60L
                onTimerFinished()
                setNewTimerLength()
                updateCountdownUI()
            }
        }


        view.fab_start.setOnClickListener {

            if (timerState == TimerState.Stopped) {

                textView_countdown.visibility = View.VISIBLE
                progress_countdown.visibility = View.VISIBLE
                input_total_stock.visibility = View.INVISIBLE
                minutesTextView.visibility = View.INVISIBLE
                setTimer.visibility = View.INVISIBLE

            }
            startTimer()
            timerState = TimerState.Running
            updateButtons()
        }

        view.fab_pause.setOnClickListener {
            timer.cancel()
            timerState = TimerState.Paused
            updateButtons()
        }

        view.fab_stop.setOnClickListener {
            timer.cancel()
            timerState = TimerState.Stopped
            onTimerFinished()
            updateButtons()

            //show timer
            input_total_stock.visibility = View.VISIBLE
            minutesTextView.visibility = View.VISIBLE
            setTimer.visibility = View.VISIBLE
            textView_countdown.visibility = View.INVISIBLE
            progress_countdown.visibility = View.INVISIBLE

        }

        view.finish_workout_button.setOnClickListener { returnToCurrentSession() }

        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()

            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
                updateCountdownUI()
            }
        }

        return view

    }

    private fun returnToCurrentSession() {
        (activity as MainActivity).showCurrentOrdersFragment()
    }

    override fun onResume() {
        super.onResume()
        initTimer()
    }

    override fun onPause() {
        super.onPause()

        if (timerState == TimerState.Running) {
            timer.cancel()
            setAlarm(nowSeconds, secondsRemaining)
        }

    }

    override fun onStop() {
        super.onStop()
        onTimerFinished()
    }


    private fun initTimer() {
        timerState = TimerState.Stopped
        timerLengthSeconds = 60
        secondsRemaining = timerLengthSeconds
        setNewTimerLength()
        updateButtons()
        updateCountdownUI()
    }

    private fun onTimerFinished() {
        timerState = TimerState.Stopped
        setNewTimerLength()
        progress_countdown.progress = 0
        secondsRemaining = timerLengthSeconds

        updateButtons()
        updateCountdownUI()
    }

    private fun startTimer() {
        timerState = TimerState.Running

        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()

            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
                updateCountdownUI()
            }
        }.start()
    }

    private fun setNewTimerLength() {
        progress_countdown.max = timerLengthSeconds.toInt()
    }


    private fun updateCountdownUI() {
        val minutesUntilFinished = secondsRemaining / 60
        val secondsInMinuteUntilFinished = secondsRemaining - minutesUntilFinished * 60
        val secondsStr = secondsInMinuteUntilFinished.toString()
        textView_countdown.text = getString(R.string.timer_format, minutesUntilFinished,
                if (secondsStr.length == 2) secondsStr else "0$secondsStr")
        progress_countdown.progress = (timerLengthSeconds - secondsRemaining).toInt()
    }

    private fun updateButtons() {
        when (timerState) {
            TimerState.Running -> {
                fab_start.isEnabled = false
                fab_pause.isEnabled = true
                fab_stop.isEnabled = true
            }
            TimerState.Stopped -> {
                fab_start.isEnabled = true
                fab_pause.isEnabled = false
                fab_stop.isEnabled = false
            }
            TimerState.Paused -> {
                fab_start.isEnabled = true
                fab_pause.isEnabled = false
                fab_stop.isEnabled = true
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return if (item.itemId == R.id.action_settings) true
        else super.onOptionsItemSelected(item)
    }


}