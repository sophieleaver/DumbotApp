package com.example.sophieleaver.dumbotapp

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_current_orders.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.toast
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

//todo - change requests in sharedpreferences on cancel/return
//todo - proper auth

var currentBench = "B7"
var isManagerMode = false
var requests = HashMap<String, Request>() // id and request


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mainLayout: View
    private lateinit var mainToolbar: Toolbar
    private lateinit var navigationView: NavigationView
    private lateinit var modeText: TextView

    var ref = FirebaseDatabase.getInstance().reference
    var logRef = ref.child("demo2").child("log")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)
        mainLayout = drawer_layout
        mainToolbar = toolbar
        navigationView = nav_view
        modeText = nav_view.getHeaderView(0).text_app_mode
        setSupportActionBar(toolbar)

        with(getSharedPreferences("prefs", Context.MODE_PRIVATE)) {
            isManagerMode = getBoolean("mode", false)
            currentBench = getString("bench", "B7")
            modeText.text = getString(R.string.app_mode, modeString())
            loadRequests(getStringSet("requests", null)?.toMutableList())
        }

        //        listenToCurrentRequestsStatus()


    }

    private fun listenToCurrentRequestsStatus() {
        logRef.addChildEventListener(object : ChildEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val newRequest = dataSnapshot.getValue(Request::class.java)

                if (newRequest != null) {
                    requests[newRequest.id]?.let {
                        when (it.type) {
                            "delivering" -> {
                                toast("delivering")
                                Log.d("MainActivity", it.id)
                                it.type = "current"
                                it.time = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)?.toEpochSecond()!!
                                recyclerView_current_dumbbells.adapter!!.notifyDataSetChanged()
                            }
                            "collecting" -> {
                                //toast("collecting")
                                toast(it.id)
                                Log.d("MainActivity", it.id)
                                requests.remove(it.id)
                                recyclerView_current_dumbbells.adapter!!.notifyDataSetChanged()

                            }
                        }
                    }
                }
            }

            override fun onChildRemoved(p0: DataSnapshot) {}

        })
        logRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (request in requests.values) {
                    if (dataSnapshot.hasChild(request.id)) {
                        when (request.type) {
                            "delivering" ->  {
                                toast("delivering")
                                Log.d("MainActivity", "${request.id}")
                                request.type = "current"
                                request.time = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)?.toEpochSecond()!!
                                recyclerView_current_dumbbells.adapter!!.notifyDataSetChanged()
                            }
                            "collecting" -> {
                                //toast("collecting")
                                toast(request.id)
                                Log.d("MainActivity", "${request.id}")
                                requests.remove(request.id)
                                recyclerView_current_dumbbells.adapter!!.notifyDataSetChanged()

                            }
                        }
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

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
        nav_view.setNavigationItemSelectedListener(this)
        nav_view.menu.findItem(R.id.menu_manager).isVisible = isManagerMode
        mainToolbar.title = "Order Dumbells"
        openFragment(OrderFragment.newInstance())
        listenToCurrentRequestsStatus()
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
        when (item.itemId) {
            R.id.nav_login -> {
                changeMode(!isManagerMode)
            }
            R.id.nav_order -> {
                val orderFragment = OrderFragment.newInstance()
                mainToolbar.title = "Order Dumbbells"
                openFragment(orderFragment)

            }
            R.id.nav_current_sessions -> {
                val currentWorkoutFragment = CurrentOrdersFragment.newInstance()
                mainToolbar.title = "Current Workout Session"
                openFragment(currentWorkoutFragment)
            }
            R.id.nav_overview -> {
                if (isManagerMode) {
                    val overviewFragment = OverviewFragment.newInstance()
                    mainToolbar.title = "DumBot Overview"
                    openFragment(overviewFragment)
                } else {
                    val restrictedFragment = RestrictedFragment.newInstance()
                    mainToolbar.title = "Cannot access page"
                    openFragment(restrictedFragment)
                }
            }
            R.id.nav_analytics -> {
                if (isManagerMode) {
                    val analyticsFragment = AnalyticsFragment.newInstance()
                    mainToolbar.title = "Analytics"
                    openFragment(analyticsFragment)
                } else {
                    val restrictedFragment = RestrictedFragment.newInstance()
                    mainToolbar.title = "Cannot access page"
                    openFragment(restrictedFragment)
                }
            }
            R.id.nav_weights -> {
                if (isManagerMode) {
                    val weightsFragment = WeightsFragment.newInstance()
                    mainToolbar.title = "Dumbbell Inventory"
                    openFragment(weightsFragment)
                } else {
                    val restrictedFragment = RestrictedFragment.newInstance()
                    mainToolbar.title = "Cannot access page"
                    openFragment(restrictedFragment)
                }
            }
            R.id.nav_settings -> {
                if (isManagerMode) {
                    val settingsFragment = SettingsFragment.newInstance()
                    mainToolbar.title = "App Settings"
                    openFragment(settingsFragment)
                } else {
                    val restrictedFragment = RestrictedFragment.newInstance()
                    mainToolbar.title = "Cannot access page"
                    openFragment(restrictedFragment)
                }
            }
            R.id.nav_timer -> {
                val timerFragment = TimerFragment.newInstance()
                mainToolbar.title = "Workout Timer"
                openFragment(timerFragment)
            }

        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.content_frame, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun changeMode(managerMode: Boolean) {
        isManagerMode = managerMode
        getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("mode", isManagerMode)
            .apply()
        navigationView.menu.findItem(R.id.menu_manager).isVisible = isManagerMode
        modeText.text = getString(R.string.app_mode, modeString())
        mainLayout.snackbar("Switched to ${modeText.text}")
    }

    fun onSuccessfulOrder(weightValue: String) {
        getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("requests", requests.keys)
            .apply()

        mainLayout.longSnackbar("Successfully ordered $weightValue kg Dumbbells", "View") {
            mainToolbar.title = "Current Workout Session"
            openFragment(CurrentOrdersFragment.newInstance())
        }
    }
}

data class Request(
    var id: String = "",
    var time: Long = 0L,
    var type: String = "",
    val weight: String = "",
    val bench: String = ""
)
