package com.example.sophieleaver.dumbotapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.os.SystemClock
import android.content.Intent
import android.graphics.Color
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Chronometer
import android.widget.TimePicker
import kotlinx.android.synthetic.main.activity_workout.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.dumbbell_collection.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
import java.util.*
import android.widget.NumberPicker
import android.widget.TextView
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDateTime
import java.time.ZoneOffset
import android.view.Gravity
import android.graphics.drawable.ColorDrawable
import android.widget.PopupWindow
import android.view.LayoutInflater




class WorkoutActivity : AppCompatActivity(){

    companion object{

        fun setAlarm(context: Context, nowSeconds: Long, secondsRemaining: Long): Long{
            val wakeUpTime = (nowSeconds + secondsRemaining)* 1000
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            return wakeUpTime
        }

        fun removeAlarm(context: Context){

        }

        val nowSeconds: Long
            get() = Calendar.getInstance().timeInMillis / 1000
    }

    enum class TimerState{
        Stopped, Paused, Running
    }

    enum class PagerState{
        Timer, Collection
    }


    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds: Long = 0
    private var timerState = TimerState.Stopped

    private var secondsRemaining: Long = 0

    private var page:PagerState = PagerState.Timer

    val database = FirebaseDatabase.getInstance()
    val ref = database.reference
    val fragTag = "WorkoutActivity"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        setSupportActionBar(timer_toolbar)
        supportActionBar?.setIcon(R.drawable.ic_timer)
        supportActionBar?.title = "      Timer"



        textView_countdown.bringToFront()
        textView_countdown.visibility = View.INVISIBLE
        progress_countdown.visibility = View.INVISIBLE


        //Number picker

        numberPicker.setMinValue(0)
        numberPicker.setMaxValue(15)
        numberPicker.value = 1
        numberPicker.setOnValueChangedListener { picker, oldVal, newVal ->

            if(!newVal.equals(null) and !newVal.toString().equals("")) {
                timer.cancel()
                timerState = TimerState.Stopped
                timerLengthSeconds = newVal * 60L
                onTimerFinished()

                setNewTimerLength()
                updateCountdownUI()


            }
        }


        fab_start.setOnClickListener{v ->

            if(timerState == TimerState.Stopped){

                textView_countdown.visibility = View.VISIBLE
                progress_countdown.visibility = View.VISIBLE
                numberPicker.visibility = View.INVISIBLE
                minutesTextView.visibility = View.INVISIBLE
                setTimer.visibility = View.INVISIBLE

            }
            startTimer()
            timerState =  TimerState.Running
            updateButtons()
        }

        fab_pause.setOnClickListener { v ->
            timer.cancel()
            timerState = TimerState.Paused
            updateButtons()
        }

        fab_stop.setOnClickListener { v ->
            timer.cancel()
            timerState = TimerState.Stopped
            onTimerFinished()
            updateButtons()

            //show timer
            numberPicker.visibility = View.VISIBLE
            minutesTextView.visibility = View.VISIBLE
            setTimer.visibility = View.VISIBLE
            textView_countdown.visibility = View.INVISIBLE
            progress_countdown.visibility = View.INVISIBLE

        }


        finish_workout_button.setOnClickListener {
            alert("Are you sure you are finished with your weights?") {
                yesButton {
                    currentRequestExists = false // there is no longer a current request

                    val now = LocalDateTime.now(ZoneOffset.UTC)
                    val unix = now.atZone(ZoneOffset.UTC)?.toEpochSecond()

                    //send request to firebase
                    val request = ref.child("demo2").child("requests").child(unix.toString())
                    request.child("bench").setValue(currentBench)
                    request.child("time").setValue(unix)
                    request.child("type").setValue("collecting")
                    request.child("weight").setValue(currentDumbbellInUse)

                    Log.d(fragTag, "Sending request $unix to server (deliver dumbbells of ${currentDumbbellInUse}kg to bench $currentBench)")

                    //update layout
                    setContentView(R.layout.dumbbell_collection);
                    setSupportActionBar(collection_toolbar)
                    page = PagerState.Collection
                    supportActionBar?.setIcon(R.drawable.ic_fitness_center)
                    supportActionBar?.title = "      Dumbbell Collection"
                    val buttonFinish:Button = findViewById(R.id.buttonFinish)
                    buttonFinish.setOnClickListener {
                        goBackToOrderPage()
                    }

                }
                noButton { }
            }.show()

        }


        floatingActionButton.setOnClickListener {
            val inflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val layout:View = inflater.inflate(R.layout.fragment_current_session,null)
            val window = PopupWindow(layout, 500, 600, true)

            window.elevation = 10.0F
            window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
            window.isOutsideTouchable = true
            window.showAtLocation(layout, Gravity.CENTER, 0, 0)

        }


        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()

            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
                updateCountdownUI()
            }
        }



    }

    fun goBackToOrderPage(){

        val intent = Intent(this, MainActivity::class.java);
        intent.putExtra("frgToLoad", "Order")
        startActivity(intent);
    }

    override fun onResume() {
        super.onResume()

        initTimer()

        removeAlarm(this)

    }

    override fun onPause() {
        super.onPause()

        if (timerState == TimerState.Running){
            timer.cancel()
            val wakeUpTime = setAlarm(this, nowSeconds, secondsRemaining)
        }
        else if (timerState == TimerState.Paused){

        }

    }

    override fun onStop() {
        super.onStop()
        onTimerFinished()
    }



    private fun initTimer(){
        timerState = TimerState.Stopped
        timerLengthSeconds = 60
        secondsRemaining = timerLengthSeconds
        setNewTimerLength()

        updateButtons()
        updateCountdownUI()
    }

    private fun onTimerFinished(){
        timerState = TimerState.Stopped
        setNewTimerLength()
        progress_countdown.progress = 0
        secondsRemaining = timerLengthSeconds

        updateButtons()
        updateCountdownUI()
    }

    private fun startTimer(){
        timerState = TimerState.Running

        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()

            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
                updateCountdownUI()
            }
        }.start()
    }

    private fun setNewTimerLength(){
        progress_countdown.max = timerLengthSeconds.toInt()
    }


    private fun updateCountdownUI(){
        val minutesUntilFinished = secondsRemaining / 60
        val secondsInMinuteUntilFinished = secondsRemaining - minutesUntilFinished * 60
        val secondsStr = secondsInMinuteUntilFinished.toString()
        textView_countdown.text = "$minutesUntilFinished:${if (secondsStr.length == 2) secondsStr else "0" + secondsStr}"
        progress_countdown.progress = (timerLengthSeconds - secondsRemaining).toInt()
    }

    private fun updateButtons(){
        when (timerState) {
            TimerState.Running ->{
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
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


    //handle back-key presses

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exitByBackKey()

            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    protected fun exitByBackKey() {

        if(page.equals(PagerState.Timer)) {

            alert("Are you sure you are finished with your weights?") {
                yesButton {
                    // update request status to CANCELLED
                    setContentView(R.layout.dumbbell_collection);
                    setSupportActionBar(collection_toolbar)
                    page = PagerState.Collection
                    supportActionBar?.setIcon(R.drawable.ic_fitness_center)
                    supportActionBar?.title = "      Dumbbell Collection"
                    val buttonFinish: Button = findViewById(R.id.buttonFinish)
                    buttonFinish.setOnClickListener {
                        goBackToOrderPage()
                    }

                }
                noButton { }
            }.show()

        }else{
            goBackToOrderPage()
        }
    }





}