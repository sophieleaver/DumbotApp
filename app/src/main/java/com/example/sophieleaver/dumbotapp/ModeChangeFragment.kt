package com.example.sophieleaver.dumbotapp

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton


class ModeChangeFragment : Fragment(), View.OnClickListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_mode_change, container, false).apply {
            findViewById<Button>(R.id.button_user_mode).setOnClickListener(this@ModeChangeFragment)
            findViewById<Button>(R.id.button_manager_mode).setOnClickListener(this@ModeChangeFragment)
            findViewById<RadioButton>(R.id.radio_mode_user).isChecked = !isManagerMode
            findViewById<RadioButton>(R.id.radio_mode_manager).isChecked = isManagerMode
        }
    }

    override fun onClick(v: View?) {
//        (activity as MainActivity).changeMode()
    }


    companion object {
        @JvmStatic
        fun newInstance() = ModeChangeFragment()

    }
}
