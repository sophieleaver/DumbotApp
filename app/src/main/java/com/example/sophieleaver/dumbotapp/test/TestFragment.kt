package com.example.sophieleaver.dumbotapp.test


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.sophieleaver.dumbotapp.R
import com.example.sophieleaver.dumbotapp.Request
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.fragment_test.view.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.random.Random

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TestFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class TestFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private val random: Random = Random(20081998)
    private val benches: List<String> = listOf("B7", "B9", "B10", "B12", "B13", "B15")
    private val timer = Timer()

    private lateinit var requestDesc: TextView
    private lateinit var createRequestButton: Button
    private lateinit var stopRequestButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

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
            timer.scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        createRequest()
                    }

                },
                0, 10000
            )
        }


        stopRequestButton.setOnClickListener {
            timer.cancel()
            requestDesc.text = getString(R.string.timer_stopped)
        }


    }


    private fun createRequest() {

        val now = LocalDateTime.now(ZoneOffset.UTC)
        val seconds = now.atZone(ZoneOffset.UTC).toEpochSecond() //request time is always in seconds
        val milliseconds = now.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val requestID = milliseconds.toString()
        val weightValue = (random.nextInt(1, 20) * 2.5).toString()
        val status = if (random.nextBoolean()) "delivering" else "collecting"
        val bench = benches[random.nextInt(benches.size)]

        val newRequest = Request(requestID, requestID, seconds, status, weightValue, bench)

        FirebaseDatabase.getInstance().reference.child("demo2/log/$requestID").setValue(newRequest)
            .addOnCompleteListener {
                requestDesc.text =
                    if (it.isSuccessful) newRequest.toString() else "Error adding request $requestID"
            }


    }


    companion object {

        @JvmStatic
        fun newInstance() = TestFragment()
    }
}
