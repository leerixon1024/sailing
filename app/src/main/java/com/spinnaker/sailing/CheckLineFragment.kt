package com.spinnaker.sailing

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.navigation.NavController
import androidx.navigation.Navigation




class CheckLineFragment : Fragment(),  View.OnClickListener{

    lateinit var navController: NavController



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_check_line, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)

    }

    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.left_btn -> navController!!.popBackStack()
            R.id.right_btn -> navController!!.navigate(R.id.action_checkLineFragment_to_stopwatchFragment)

        }
    }
}