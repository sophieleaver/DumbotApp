package com.example.sophieleaver.dumbotapp

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat.getSystemService
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.sophieleaver.dumbotapp.test.TestFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.toast
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

var currentBench: String = "B7"
var isManagerMode: Boolean = false
var requests: MutableMap<String, Request> = HashMap() // id and request
var numberOfRequests = 0

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mainLayout: View
    private lateinit var mainToolbar: Toolbar
    private lateinit var navigationView: NavigationView
    private lateinit var modeText: TextView

    private lateinit var analyticsFragment: AnalyticsFragment
    private lateinit var currentOrdersFragment: CurrentOrdersFragment
    private lateinit var modeChangeFragment: ModeChangeFragment
    private lateinit var orderFragment: OrderFragment
    private lateinit var overviewFragment: OverviewFragment
    private lateinit var restrictedFragment: RestrictedFragment
    private lateinit var settingsFragment: SettingsFragment
    private lateinit var timerFragment: TimerFragment
    private lateinit var weightsFragment: WeightsFragment
    private lateinit var loginFragment: LoginFragment
    private lateinit var mapFragment: MapFragment
    private lateinit var testFragment: TestFragment


    private var activeFragment: Fragment? = null

    private var ref = FirebaseDatabase.getInstance().reference
    private var logRef = ref.child("demo2").child("log")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)
        mainLayout = drawer_layout
        mainToolbar = toolbar
        navigationView = nav_view
        modeText = nav_view.getHeaderView(0).text_app_mode
        setSupportActionBar(toolbar)

        with(getSharedPreferences("prefs", Context.MODE_PRIVATE)) {
            isManagerMode = getBoolean("mode", false)
            currentBench = getString("bench", "B7")!!
            modeText.text = getString(R.string.app_mode, modeString())
            loadRequests(getStringSet("requests", null)?.toMutableList())
        }

        /*for (i in 1..50){
            val id = 1549188300000 + (i * 1600000)
            val randomDB = (1..5).random()
            var weight = ""
            when (randomDB) {
                1 -> weight = "1.0"
                2 -> weight = "1.5"
                3 -> weight = "2.0"
                4 -> weight = "2.5"
                5 -> weight = "10"
            }

            val randomBench = (1..5).random()
            var bench = ""
            when (randomBench) {
                1 -> bench = "B7"
                2 -> bench = "B9"
                3 -> bench = "B10"
                4 -> bench = "B12"
                5 -> bench = "B13"
            }
            val newRequest = LoggedRequest(id.toString(),id/1000, "delivering", weight, bench, "SA1")
            //toast("$id added")
            ref.child("demo2/log/$id").setValue(newRequest)
        }
*/

    }

    private fun listenToCurrentRequestsStatus() {
        logRef.addChildEventListener(object : ChildEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val newRequest = dataSnapshot.getValue(Request::class.java)

                if (newRequest != null && requests.containsKey(newRequest.id)) {
                    requests[newRequest.id]!!.let {
                        when (it.type) {
                            "delivering" -> {
                                Log.d("MainActivity", it.id)
                                it.type = "current"
                                it.time =
                                    LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)?.toEpochSecond()!!
                                updateCurrentWorkout()

                            }
                            "collecting" -> { //detects when collection has been completed by the dumbot
                                Log.d("MainActivity", it.id)
                                requests.remove(it.id) //remove from hashmap
                                //increase availability on firebase
                                val formattedWeight = it.weight.replace('.', '-', true) //change the weight value from 4.0 to 4-0 for firebase
                                ref.child("demo2/weights/$formattedWeight/activeRequests/${it.ogID}").removeValue()
                                updateCurrentWorkout()

                            }
                        }
                    }
                }
            }

            override fun onChildRemoved(p0: DataSnapshot) {}

        })
    }

    private fun loadRequests(requestIds: MutableList<String>?) {

        if (requestIds.isNullOrEmpty()) setupActivity()
        else {
            val requestsReference = FirebaseDatabase.getInstance().reference.child("demo2/requests")
            with(requestIds.removeAt(0)) {
                requestsReference.child(this)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            dataSnapshot.getValue(Request::class.java)
                                ?.let { requests.put(this@with, it) }
                            loadRequests(requestIds)
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.w(
                                "MainActivity",
                                "Failed to find request ${this@with}",
                                databaseError.toException()
                            )
                            loadRequests(requestIds)
                        }

                    })
            }
        }
    }

    private fun setupActivity() {
        val toggle = ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        mainToolbar.title = "Order Dumbbells"
        setupFragments()
        listenToCurrentRequestsStatus()
        showNewFragment(orderFragment)
        navigationView.menu.findItem(R.id.nav_login).isVisible = !isManagerMode
    }

    private fun setupFragments() {
        analyticsFragment = AnalyticsFragment.newInstance()
        currentOrdersFragment = CurrentOrdersFragment.newInstance()
        modeChangeFragment = ModeChangeFragment.newInstance()
        orderFragment = OrderFragment.newInstance()
        overviewFragment = OverviewFragment.newInstance()
        restrictedFragment = RestrictedFragment.newInstance()
        settingsFragment = SettingsFragment.newInstance()
        timerFragment = TimerFragment.newInstance()
        weightsFragment = WeightsFragment.newInstance()
        loginFragment = LoginFragment.newInstance()
        mapFragment = MapFragment.newInstance()
        testFragment = TestFragment.newInstance()

        with(supportFragmentManager) {
            beginTransaction().add(R.id.content_frame, analyticsFragment, "fragment_analytics")
                .hide(analyticsFragment)
                .commit()
            beginTransaction().add(
                R.id.content_frame,
                currentOrdersFragment,
                "fragment_current_orders"
            )
                .hide(currentOrdersFragment).commit()
            beginTransaction().add(R.id.content_frame, overviewFragment, "fragment_overview")
                .hide(overviewFragment)
                .commit()
            beginTransaction().add(R.id.content_frame, restrictedFragment, "fragment_restricted")
                .hide(restrictedFragment).commit()
            beginTransaction().add(R.id.content_frame, settingsFragment, "fragment_settings")
                .hide(settingsFragment)
                .commit()
            beginTransaction().add(R.id.content_frame, timerFragment, "fragment_timer")
                .hide(timerFragment).commit()
            beginTransaction().add(R.id.content_frame, weightsFragment, "fragment_weights")
                .hide(weightsFragment)
                .commit()
            beginTransaction().add(R.id.content_frame, loginFragment, "fragment_login")
                .hide(loginFragment).commit()
            beginTransaction().add(R.id.content_frame, mapFragment, "fragment_map")
                .hide(mapFragment).commit()
            beginTransaction().add(R.id.content_frame, testFragment, "fragment_test")
                .hide(testFragment).commit()
            beginTransaction().add(R.id.content_frame, orderFragment, "fragment_order").commit()
        }

        activeFragment = orderFragment

        nav_view.setNavigationItemSelectedListener(this)
        nav_view.menu.findItem(R.id.menu_manager).isVisible = isManagerMode

    }

    override fun onBackPressed() {

        if (drawer_layout.isDrawerOpen(GravityCompat.START)) drawer_layout.closeDrawer(GravityCompat.START)
        else super.onBackPressed()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
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

    private fun modeString(): String = if (isManagerMode) "Manager" else "User"


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val newFragment: Fragment
        when (item.itemId) {
            R.id.nav_order -> {
                newFragment = orderFragment
                mainToolbar.title = "Order Dumbbells"
            }
            R.id.nav_current_sessions -> {
                //
                val tempWorkout = CurrentOrdersFragment.newInstance()
                newFragment = tempWorkout //pls do not remove it means we refresh when you go back onto it which is good for demo disasters
                supportFragmentManager.beginTransaction().add(R.id.content_frame, tempWorkout, "fragment_current").commit()
                supportFragmentManager.beginTransaction().show(tempWorkout).commit()
                supportFragmentManager.beginTransaction().hide(activeFragment!!).commit()
                activeFragment = tempWorkout
                currentOrdersFragment = tempWorkout
                mainToolbar.title = "Current Workout"
            }
            R.id.nav_timer -> {
                newFragment = timerFragment
                mainToolbar.title = "Set a Workout Timer"
            }

            R.id.nav_overview -> {
                if (isManagerMode) {
                    newFragment = overviewFragment
                    mainToolbar.title = "DumBot Status and Overview"
                } else newFragment = restrictedFragment
            }

            R.id.nav_analytics -> {
                if (isManagerMode) {
                    newFragment = analyticsFragment
                    mainToolbar.title = "Gym Analytics and Data"
                } else newFragment = restrictedFragment
            }

            R.id.nav_weights -> {
                if (isManagerMode) {
                    newFragment = weightsFragment
                    mainToolbar.title = "Edit Dumbbell Stock"
                } else newFragment = restrictedFragment
            }

            R.id.nav_map -> {
                if (isManagerMode) {
                    newFragment = mapFragment
                    mainToolbar.title = "Edit Gym Layout"
                } else newFragment = restrictedFragment
            }

            R.id.nav_settings -> {
                if (isManagerMode) {
                    newFragment = settingsFragment
                    mainToolbar.title = "App Settings"
                } else newFragment = restrictedFragment
            }

            R.id.nav_login -> {
                newFragment = loginFragment
                mainToolbar.title = "Manager Log In"
            }

//            R.id.nav_test -> {
//                newFragment = testFragment
//                mainToolbar.title = "Generate Log Requests"
//            }

            else -> newFragment = modeChangeFragment

        }

        if (newFragment == modeChangeFragment) {
            FirebaseAuth.getInstance().signOut()
            changeMode("USER")
            showNewFragment(orderFragment)
        } else showNewFragment(newFragment)

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }


    private fun showNewFragment(newFragment: Fragment?) {
        supportFragmentManager.beginTransaction()
            .hide(activeFragment!!)
            .show(newFragment!!)
            .commit()

        // set new fragment as active fragment
        activeFragment = newFragment

        //do not let user proceed if there is no network connection
        checkNetwork()
    }

    fun showTimeFragment() {
        mainToolbar.title = "Set a Workout Timer"
        showNewFragment(timerFragment)
    }

    fun showOrderFragment() {
        mainToolbar.title = "Order Weights"
        showNewFragment(orderFragment)
    }

    fun showCurrentOrdersFragment() {
        mainToolbar.title = "Current Workout"
        val tempWorkout = CurrentOrdersFragment.newInstance()
        supportFragmentManager.beginTransaction().add(R.id.content_frame, tempWorkout, "fragment_current").commit()
        supportFragmentManager.beginTransaction().show(tempWorkout).commit()
        supportFragmentManager.beginTransaction().hide(activeFragment!!).commit()
        activeFragment = tempWorkout
        currentOrdersFragment = tempWorkout
//        showNewFragment(currentOrdersFragment)
    }

    fun changeMode(mode: String) {
        when (mode) {
            "USER" -> isManagerMode = false
            "MANAGER" -> isManagerMode = true
        }
        getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean("mode", isManagerMode)
            .apply()
        navigationView.menu.findItem(R.id.menu_manager).isVisible = isManagerMode
        navigationView.menu.findItem(R.id.nav_login).isVisible = !isManagerMode
        modeText.text = getString(R.string.app_mode, modeString())
        mainLayout.snackbar("Switched to ${modeText.text}")
    }

    fun onSuccessfulOrder(weightValue: String, status : String) {
        getSharedPreferences("prefs", Context.MODE_PRIVATE).edit()
            .putStringSet("requests", requests.keys).apply()

        if (status == "delivering") {
            mainLayout.longSnackbar("Successfully ordered $weightValue kg Dumbbells", "View") {
                mainToolbar.title = "Current Workout Session"
                showNewFragment(currentOrdersFragment)
            }
        }
        if (status == "waiting"){
            mainLayout.longSnackbar("Successfully joined the queue for $weightValue kg Dumbbells", "View") {
                mainToolbar.title = "Current Workout Session"
                showNewFragment(currentOrdersFragment)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        getSharedPreferences("prefs", Context.MODE_PRIVATE).edit()
            .putStringSet("requests", requests.keys).apply()
    }


    override fun onDestroy() {
        super.onDestroy()
        getSharedPreferences("prefs", Context.MODE_PRIVATE).edit()
            .putStringSet("requests", requests.keys).apply()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    fun checkNetwork(){
        if (!isNetworkAvailable()){
            val builder = AlertDialog.Builder(this)
            builder.setTitle("NO NETWORK CONNECTION")
            builder.setMessage("The DumBot app requires an internet connection to function.\nPlease check your connection and try again.")
            builder.setNeutralButton("RETRY CONNECTION", null)
            val dialog = builder.create()

            dialog.setOnShowListener {
                val button : Button = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                button.setOnClickListener {
                    if (isNetworkAvailable()){
                        dialog.cancel()
                        toast("reconnecting...")
                    } else {
                        toast("No connection found, please try again.")
                    }
                }
            }
            dialog.show()
            dialog.setCanceledOnTouchOutside(false)
        }
    }

    fun checkRequestLimit() {
        numberOfRequests = 0
        for (request in requests.values){
            if (request.type == "delivering" || request.type == "current"){
                numberOfRequests++

            }
            //add queued weights
        }
       orderFragment.setupRecyclerView()
    }

    fun updateCurrentWorkout(){
        currentOrdersFragment.currentDBRecyclerView.adapter!!.notifyDataSetChanged()
        currentOrdersFragment.queuedDBRecyclerView.adapter!!.notifyDataSetChanged()
        currentOrdersFragment.setUpRecyclerViews()
    }
}

data class Request(
    var id: String = "", var ogID:String = "", var time: Long = 0L, var type: String = "", val weight: String = "",
    val bench: String = ""
)


data class LoggedRequest(
    var id: String = "", var time: Long = 0L, var type: String = "", val weight: String = "",
    val end_node: String = "", val start_node: String = ""
)
