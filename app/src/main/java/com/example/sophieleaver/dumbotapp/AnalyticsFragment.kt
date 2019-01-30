package com.example.sophieleaver.dumbotapp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.sophieleaver.dumbotapp.R.styleable.Spinner

//TODO create a login
class AnalyticsFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_analytics, container, false)

        //create spinner to hold selection box for period of time for graph
        val dropdown : Spinner = view.findViewById(R.id.spinner1)
        val items : Array<String> = arrayOf("Last Week", "Last Month", "Last Year")
        val adapter : ArrayAdapter<String> = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items)
        dropdown.adapter = adapter
        return view
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    companion object {
        @JvmStatic
        fun newInstance() = AnalyticsFragment()
    }
}
