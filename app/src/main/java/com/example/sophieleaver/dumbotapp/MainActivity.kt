package com.example.sophieleaver.dumbotapp

import android.content.Context
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import org.jetbrains.anko.design.longSnackbar

//todo - clear all requests eventually
//todo - change requests in sharedpreferences on cancel/return
//todo - string resources everywhere
//todo - proper auth

var currentBench = 1
var isManagerMode = false
var requests = HashMap<String, Request>() // id and request

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mainLayout: View
    private lateinit var mainToolbar: Toolbar
    private lateinit var navigationView: NavigationView
    private lateinit var modeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainLayout = drawer_layout
        mainToolbar = toolbar
        navigationView = nav_view
        modeText = nav_view.getHeaderView(0).text_app_mode
        setSupportActionBar(toolbar)

        with(getSharedPreferences("prefs", Context.MODE_PRIVATE)) {
            isManagerMode = getBoolean("mode", false)
            currentBench = getInt("bench", 1)
            modeText.text = getString(R.string.app_mode, modeString())
            loadRequests(getStringSet("requests", null)?.toMutableList())

        }


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
                val modeFragment = ModeChangeFragment.newInstance()
                mainToolbar.title = "Change App Mode"
                openFragment(modeFragment)
            }
            R.id.nav_order -> {
//                if (!currentRequestExists) { //if the user does not have a current request open, the weight list is opened
                val orderFragment = OrderFragment.newInstance()
                mainToolbar.title = "Order Dumbbells"
                openFragment(orderFragment)
//                } else {
//                    val currentSession = CurrentSessionFragment.newInstance()
//                    mainToolbar.title = "Active Workout Session"
//                    openFragment(currentSession)
//                }
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

//            R.id.nav_demo -> {
//                if (globalState.equals("user")){
//                    val restrictedFragment  = RestrictedFragment.newInstance()
//                    mainToolbar.title = "Cannot access page"
//                    openFragment(restrictedFragment)
//                } else {
//                    val demoFragment = DemoFragment.newInstance()
//                    supportActionBar!!.title = "Demo"
//                    openFragment(demoFragment)
//                }
//            }

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
    val id: String = "",
    var time: Long = 0L,
    var type: String = "",
    val weight: String = "",
    val benchID: String = ""
)
