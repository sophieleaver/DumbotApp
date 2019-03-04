//package com.example.sophieleaver.dumbotapp
//
//import android.content.Intent
//import android.os.Bundle
//import android.support.v7.app.AppCompatActivity
//import android.util.Log
//import android.widget.Button
//import android.widget.TextView
//import com.google.firebase.firestore.DocumentSnapshot
//import com.google.firebase.firestore.EventListener
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.ListenerRegistration
//import org.jetbrains.anko.alert
//import org.jetbrains.anko.noButton
//import org.jetbrains.anko.yesButton
//import android.view.KeyEvent
//import kotlinx.android.synthetic.main.active_session.*
//
//
//class ActiveSession : AppCompatActivity() {
//
//    private val TAG = "ActiveSession"
//
//    private var queue: Boolean = true
//    private lateinit var cancelButton: Button
//    private lateinit var activeSessionButton:Button
//
//    private val database:FirebaseFirestore = FirebaseFirestore.getInstance()
//
//    private var listener:ListenerRegistration? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.active_session)
//
//        setSupportActionBar(queue_toolbar)
//        supportActionBar?.setIcon(R.drawable.ic_android)
//        supportActionBar?.title = "      Dumbot App"
//
//
//
//        queue = intent.getBooleanExtra("Queue", true)
//
//        var message: TextView = findViewById(R.id.message)
//
//        //check if user is in queue or if the dumbbell is available
//        if (queue) {
//            //get queue number from firebase
//            var queueNumber = null
//            message.text = "You are currently number " + queueNumber + " in the wait queue."
//
//        } else {
//            //get request number from firebase
//            var requestNumber = 2
//
//            if (requestNumber.equals(1)) {
//
//                message.text = "Your dumbbells are being delivered."
//
//            } else {
//
//                message.text = "Dumbot has received your order but is currently busy. You are number " +
//                        requestNumber + " in line."
//            }
//
//        }
//
//
//        cancelButton = findViewById(R.id.cancel_button)
//        cancelButton.setOnClickListener {
//
//            alert("Are you sure you want to cancel your request?") {
//                yesButton {
//                    // update request status to CANCELLED
//                    goBackToOrderPage()
//                }
//                noButton { }
//            }.show()
//        }
//
//        activeSessionButton = findViewById(R.id.active_session_button)
//        activeSessionButton.setOnClickListener {
//
//            goToWorkoutPage()
//        }
//
//
//    }
//
//
//
//    override fun onStart() {
//        super.onStart()
//
//        //listen for request status in firebase
//
//        val status = HashMap<String, Any>()
//        status["status"] = "WAITING_FOR_DELIVERY"
//
//
//        //database.collection("requests").document("XGbJP6alPiC5muzDIHSt")
//        //    .set(status)
//        //    .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
//        //    .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
//
//
//        val docRef = database.collection("requests").document("XGbJP6alPiC5muzDIHSt")
//        listener = docRef.addSnapshotListener(EventListener<DocumentSnapshot> { snapshot, e ->
//            if (e != null) {
//                Log.w(TAG, "Listen failed.", e)
//                return@EventListener
//            }
//
//            if (snapshot != null && snapshot.exists()) {
//                if (snapshot.getString("status")!!.equals("IN_PROGRESS")) {
//                    goToWorkoutPage()
//                }
//                Log.d(TAG, "Current data: " + snapshot.getString("status"))
//            } else {
//                Log.d(TAG, "Current data: null")
//            }
//        })
//
//    }
//
//    override fun onStop() {
//        super.onStop()
//        listener?.remove()
//    }
//
//
//    fun goBackToOrderPage() {
//
//        val intent = Intent(this, MainActivity::class.java);
//        intent.putExtra("frgToLoad", "Order")
//        startActivity(intent);
//
//    }
//
//
//    fun goToWorkoutPage() {
//
//        val intent = Intent(this, TimerActivity::class.java);
//        startActivity(intent);
//
//
//    }
//
//    //handle back-key presses
//
//    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            exitByBackKey()
//
//
//            return true
//        }
//        return super.onKeyDown(keyCode, event)
//    }
//
//    protected fun exitByBackKey() {
//
//        alert("Are you sure you want to cancel your request?") {
//            yesButton {
//                // update request status to CANCELLED
//                goBackToOrderPage()
//            }
//            noButton { }
//        }.show()
//
//    }
//
//}