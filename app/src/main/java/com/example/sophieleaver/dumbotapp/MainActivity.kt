package com.example.sophieleaver.dumbotapp

import android.content.Context
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

//todo - be able to see what mode we are in
//todo - string resources everywhere

var globalState = "manager" //TODO change to dependent on login
var currentBench = 1 // TODO save the value
var currentRequestExists = false
var currentDumbbellInUse = "0"
var userMode = false

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {


    lateinit var navigationView: NavigationView
    private lateinit var mainToolbar: Toolbar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainToolbar = toolbar
        navigationView = nav_view
        setSupportActionBar(toolbar)

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
        nav_view.menu.findItem(R.id.menu_manager).isVisible = !userMode
        mainToolbar.title = "Order Dumbells"
        openFragment(OrderFragment.newInstance())

        if (intent != null) {

            val fragment = intent.getStringExtra("frgToLoad")

            openFragment(
                when (fragment) {
                    //TODO do we need the rest?
                    //"CurrentSession" -> CurrentSessionFragment.newInstance()
                    "Demo" -> DemoFragment.newInstance()
                    "Weights" -> WeightsFragment.newInstance()
                    "Analytics" -> AnalyticsFragment.newInstance()
                    else -> OrderFragment.newInstance()
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        userMode = getSharedPreferences("prefs", Context.MODE_PRIVATE).getBoolean("mode", true)
        globalState = if (userMode) "user" else "manager"
    }


    override fun onBackPressed() {

        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }

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

    //TODO - fixe bug where it crashes if you go onto analytics in user mode using back button

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_login -> {
                val modeFragment = ModeChangeFragment.newInstance()
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
                openFragment(currentWorkoutFragment)

            }
            R.id.nav_overview -> {
                if (globalState == "user") {
                    val restrictedFragment = RestrictedFragment.newInstance()
                    mainToolbar.title = "Cannot access page"
                    openFragment(restrictedFragment)
                } else {
                    val overviewFragment = OverviewFragment.newInstance()
                    mainToolbar.title = "DumBot Overview"
                    openFragment(overviewFragment)
                }
            }
            R.id.nav_analytics -> {
                if (globalState == "user") {
                    val restrictedFragment = RestrictedFragment.newInstance()
                    mainToolbar.title = "Cannot access page"
                    openFragment(restrictedFragment)
                } else {
                    val analyticsFragment = AnalyticsFragment.newInstance()
                    mainToolbar.title = "Analytics"
                    openFragment(analyticsFragment)
                }
            }
            R.id.nav_weights -> {
                if (globalState == "user") {
                    val restrictedFragment = RestrictedFragment.newInstance()
                    mainToolbar.title = "Cannot access page"
                    openFragment(restrictedFragment)
                } else {
                    val weightsFragment = WeightsFragment.newInstance()
                    mainToolbar.title = "Weight Inventory"
                    openFragment(weightsFragment)
                }
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

    fun changeMode(isUserMode: Boolean) {
        userMode = isUserMode
        globalState = if (userMode) "user" else "manager"
        getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("mode", isUserMode)
            .apply()
        navigationView.menu.findItem(R.id.menu_manager).isVisible = !userMode
    }
}
