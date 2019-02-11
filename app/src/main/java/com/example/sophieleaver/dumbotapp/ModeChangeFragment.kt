package com.example.sophieleaver.dumbotapp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast


class ModeChangeFragment : Fragment(), View.OnClickListener{

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_mode_change, container, false)

        var button1 : Button = view.findViewById(R.id.button_user_mode)
        button1.setOnClickListener (this)
        var button2 : Button = view.findViewById(R.id.button_manager_mode)
        button2.setOnClickListener(this)
        return view
    }

    override fun onClick(v: View?) {
        when (v?.id){
            R.id.button_manager_mode -> {
                globalState = "manager"
//                (activity as MainActivity).setNavAsManager()
                Toast.makeText(context, "Mode is MANAGER", Toast.LENGTH_SHORT).show()
            }
            R.id.button_user_mode -> {
                globalState = "user"
//                (activity as MainActivity).setNavAsUser()
                Toast.makeText(context, "Mode is USER", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ModeChangeFragment()

    }
}
