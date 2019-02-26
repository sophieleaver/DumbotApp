package com.example.sophieleaver.dumbotapp

import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.view.*

var globalState = "manager" //TODO change to dependent on login
var currentBench = 1 // TODO save the value
var currentRequestExists = false
var currentDumbbellInUse = "0"
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        getSupportActionBar()!!.setDisplayShowTitleEnabled(false)

        var toggle = ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        openFragment(OverviewFragment.newInstance())

        if(!intent.equals(null)){

            val fragment = intent.getStringExtra("frgToLoad")

            when (fragment) {
                "Order"-> {
                    val orderFragment = OrderFragment.newInstance()
                    openFragment(orderFragment)
                }
                "Demo"-> {
                    val demoFragment = DemoFragment.newInstance()
                    openFragment(demoFragment)
                }
                "Weights"-> {
                    val weightsFragment = WeightsFragment.newInstance()
                    openFragment(weightsFragment)
                }
                "Analytics" -> {
                    val analyticsFragment = AnalyticsFragment.newInstance()
                    openFragment(analyticsFragment)
                }

            }
        }
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_login -> {
                val modeFragment = ModeChangeFragment.newInstance()
                openFragment(modeFragment)
            }
            R.id.nav_order -> {
                if (!currentRequestExists) { //if the user does not have a current request open, the weight list is opened
                    val orderFragment = OrderFragment.newInstance()
                    openFragment(orderFragment)
                } else {
                    val currentSession = CurrentSessionFragment.newInstance()
                    openFragment(currentSession)
                }
            }
            R.id.nav_overview -> {
                if (globalState.equals("user")){
                    val restrictedFragment  = RestrictedFragment.newInstance()
                    openFragment(restrictedFragment)
                } else {
                    val overviewFragment = OverviewFragment.newInstance()
                    openFragment(overviewFragment)
                }
            }
            R.id.nav_analytics -> {
                if (globalState.equals("user")){
                    val restrictedFragment  = RestrictedFragment.newInstance()
                    openFragment(restrictedFragment)
                } else {
                    val analyticsFragment = AnalyticsFragment.newInstance()
                    openFragment(analyticsFragment)
                }
            }
            R.id.nav_weights -> {
                if (globalState.equals("user")){
                    val restrictedFragment  = RestrictedFragment.newInstance()
                    openFragment(restrictedFragment)
                } else {
                    val weightsFragment = WeightsFragment.newInstance()
                    openFragment(weightsFragment)
                }
            }
//            R.id.nav_demo -> {
//                if (globalState.equals("user")){
//                    val restrictedFragment  = RestrictedFragment.newInstance()
//                    openFragment(restrictedFragment)
//                } else {
//                    val demoFragment = DemoFragment.newInstance()
//                    openFragment(demoFragment)
//                }
//            }

        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun openFragment(fragment: android.support.v4.app.Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.content_frame, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
