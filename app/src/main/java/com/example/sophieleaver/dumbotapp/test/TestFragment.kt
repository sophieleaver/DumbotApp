package com.example.sophieleaver.dumbotapp.test


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.sophieleaver.dumbotapp.LoggedRequest
import com.example.sophieleaver.dumbotapp.R
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.fragment_test.view.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.random.Random


/**
 * A simple [Fragment] subclass.
 * Use the [TestFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class TestFragment : Fragment() {
    private val random: Random = Random(20081998)
    private val benches: List<String> = listOf("B7", "B9", "B10", "B12", "B13", "B15")
    private val storages: List<String> = listOf("SA1", "SA2", "SA3")
    private var timer: Timer? = Timer()

    private lateinit var requestDesc: TextView
    private lateinit var createRequestButton: Button
    private lateinit var stopRequestButton: Button


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestDesc = view.text_req_desc
        createRequestButton = view.btn_create_request
        stopRequestButton = view.btn_stop_requests



        createRequestButton.setOnClickListener {
            timer?.schedule(MyTimerTask(timer!!, random), random.nextLong(10000L))
        }


        stopRequestButton.setOnClickListener {
            timer?.cancel()
            timer = null
            requestDesc.text = getString(R.string.timer_stopped)
        }


    }


    private fun createRequest(): LoggedRequest {

        val now = LocalDateTime.now(ZoneOffset.UTC)
        val seconds = now.atZone(ZoneOffset.UTC).toEpochSecond() //request time is always in seconds
        val milliseconds = now.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val requestID = milliseconds.toString()
        val weightValue = (random.nextInt(1, 20) * 2.5).toString()
        val status = if (random.nextBoolean()) "delivering" else "collecting"
        val bench = benches[random.nextInt(benches.size)]
        val storage = storages[random.nextInt(storages.size)]


        return if (status == "delivering") LoggedRequest(
            requestID,
            seconds,
            status,
            weightValue,
            bench,
            storage
        )
        else LoggedRequest(requestID, seconds, status, weightValue, storage, bench)

    }

    inner class MyTimerTask(private val timer: Timer, private val random: Random) : TimerTask() {

        override fun run() {
            val newRequest = createRequest()
            FirebaseDatabase.getInstance().reference.child("demo2/log/${newRequest.id}")
                .setValue(newRequest)
                .addOnCompleteListener {
                    val nextTime = random.nextLong(10000L, 25000L)
                    requestDesc.text =
                        if (it.isSuccessful) newRequest.toString() + "\n next Request in ${nextTime / 1000}"
                        else "Error adding request ${newRequest.id}"
                    timer.schedule(MyTimerTask(timer, random), nextTime)
                }
        }
    }


    companion object {

        @JvmStatic
        fun newInstance() = TestFragment()
    }
}
