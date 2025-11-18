package com.spinnaker.sailing

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.navigation.NavController
import androidx.navigation.Navigation





/**
 * A simple [Fragment] subclass.
 * Use the [MaintSubmenuFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MaintSubmenuFragment : Fragment() , View.OnClickListener {
    // TODO: Rename and change types of parameters

    lateinit var navController: NavController


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_maint_submenu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById< ImageButton>(R.id.home_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)
        view.findViewById<Button>(R.id.maintBoatFragment).setOnClickListener(this)
        view.findViewById<Button>(R.id.maintMarkFragment).setOnClickListener(this)
        view.findViewById<Button>(R.id.maintCourseFragment).setOnClickListener(this)
        view.findViewById<Button>(R.id.manageRaceLogsFragment).setOnClickListener(this)

    }

    override fun onClick(v: View?) {
        when(v!!.id){

            R.id.maintBoatFragment -> navController.navigate(R.id.action_maintSubmenuFragment_to_maintBoatFragment )
            R.id.maintCourseFragment  -> navController.navigate(R.id.action_maintSubmenuFragment_to_maintCourseFragment )
            R.id.maintMarkFragment -> navController.navigate(R.id.action_maintSubmenuFragment_to_maintMarkFragment  )
            R.id.manageRaceLogsFragment -> navController.navigate(R.id.action_maintSubmenuFragment_to_raceLogFragment)
            R.id.left_btn -> navController.popBackStack()
            R.id.right_btn -> navController.navigate(R.id.action_maintSubmenuFragment_to_maintBoatFragment )
            R.id.home_btn -> navController.navigate(R.id.mainFragment )



        }
    }
}