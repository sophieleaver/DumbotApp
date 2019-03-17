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
import kotlinx.android.synthetic.main.nav_header_main.view.*
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.longToast
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

//todo - change requests in sharedpreferences on cancel/return
//todo - proper auth

var currentBench: String = "B7"
var isManagerMode: Boolean = false
var requests: MutableMap<String, Request> = HashMap() // id and request


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

    private var activeFragment: Fragment? = null

    private var ref = FirebaseDatabase.getInstance().reference
    private var logRef = ref.child("demo2").child("log")

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

                if (newRequest != null && requests.containsKey(newRequest.id)) {
                    requests[newRequest.id]!!.let {
                        when (it.type) {
                            "delivering" -> {
                                Log.d("MainActivity", it.id)
                                it.type = "current"
                                it.time = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)?.toEpochSecond()!!
                                currentOrdersFragment.currentDBRecyclerView.adapter!!.notifyDataSetChanged()
                            }
                            "collecting" -> {
                                Log.d("MainActivity", it.id)
                                val size = requests.size
                                requests.remove(it.id)
                                longToast(size - requests.size)
                                currentOrdersFragment.currentDBRecyclerView.adapter!!.notifyDataSetChanged()

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
//            todo - this next time
            /*requestsReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    requests = dataSnapshot.children
                        .map { it.getValue(Request::class.java)!! }
                        .filter { it.bench == currentBench  }
                        .associateBy { it.id }
                        .toMutableMap()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("MainActivity", "Failed to find request ${this@with}", databaseError.toException())
                    loadRequests(requestIds)
                }
            })*/

            with(requestIds.removeAt(0)) {
                requestsReference.child(this).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        dataSnapshot.getValue(Request::class.java)?.let { requests.put(this@with, it) }
                        loadRequests(requestIds)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.w("MainActivity", "Failed to find request ${this@with}", databaseError.toException())
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

        with(supportFragmentManager) {
            beginTransaction().add(R.id.content_frame, analyticsFragment, "fragment_analytics").hide(analyticsFragment)
                .commit()
            beginTransaction().add(R.id.content_frame, currentOrdersFragment, "fragment_current_orders")
                .hide(currentOrdersFragment).commit()
            beginTransaction().add(R.id.content_frame, overviewFragment, "fragment_overview").hide(overviewFragment)
                .commit()
            beginTransaction().add(R.id.content_frame, restrictedFragment, "fragment_restricted")
                .hide(restrictedFragment).commit()
            beginTransaction().add(R.id.content_frame, settingsFragment, "fragment_settings").hide(settingsFragment)
                .commit()
            beginTransaction().add(R.id.content_frame, timerFragment, "fragment_timer").hide(timerFragment).commit()
            beginTransaction().add(R.id.content_frame, weightsFragment, "fragment_weights").hide(weightsFragment)
                .commit()
            beginTransaction().add(R.id.content_frame, loginFragment, "fragment_login").hide(loginFragment).commit()
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
        val newFragment = when (item.itemId) {
            R.id.nav_order -> orderFragment
            R.id.nav_current_sessions -> currentOrdersFragment
            R.id.nav_timer -> timerFragment
            R.id.nav_overview -> if (isManagerMode) overviewFragment else restrictedFragment
            R.id.nav_analytics -> if (isManagerMode) analyticsFragment else restrictedFragment
            R.id.nav_weights -> if (isManagerMode) weightsFragment else restrictedFragment
            R.id.nav_settings -> if (isManagerMode) settingsFragment else restrictedFragment
            R.id.nav_login -> loginFragment
            else -> modeChangeFragment
        }

        if (newFragment == modeChangeFragment) changeMode() else showNewFragment(newFragment)

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

//    fun openFragment(fragment: Fragment) {
//        val transaction = supportFragmentManager.beginTransaction()
//        transaction.replace(R.id.content_frame, fragment)
//        transaction.addToBackStack(null)
//        transaction.commit()
//    }

    private fun showNewFragment(newFragment: Fragment?) {
        supportFragmentManager.beginTransaction()
            .hide(activeFragment!!)
            .show(newFragment!!)
            .commit()

        // set new fragment as active fragment
        activeFragment = newFragment
    }

    fun showTimeFragment() {
        showNewFragment(timerFragment)
    }

    fun showCurrentOrdersFragment() {
        showNewFragment(currentOrdersFragment)
    }

    fun changeMode() {
        isManagerMode = !isManagerMode
        getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean("mode", isManagerMode).apply()
        navigationView.menu.findItem(R.id.menu_manager).isVisible = isManagerMode
        modeText.text = getString(R.string.app_mode, modeString())
        mainLayout.snackbar("Switched to ${modeText.text}")
    }

    fun onSuccessfulOrder(weightValue: String) {
        getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putStringSet("requests", requests.keys).apply()

        mainLayout.longSnackbar("Successfully ordered $weightValue kg Dumbbells", "View") {
            mainToolbar.title = "Current Workout Session"
            showNewFragment(currentOrdersFragment)
        }
    }

    override fun onStop() {
        super.onStop()
        getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putStringSet("requests", requests.keys).apply()
    }


    override fun onDestroy() {
        super.onDestroy()
        getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putStringSet("requests", requests.keys).apply()
    }
}

data class Request(
    var id: String = "", var time: Long = 0L, var type: String = "", val weight: String = "",
    val bench: String = ""
)


data class LoggedRequest(
    var id: String = "", var time: Long = 0L, var type: String = "", val weight: String = "",
    val end_node: String = "", val start_node:String = ""
)
