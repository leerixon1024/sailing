package com.spinnaker.sailing

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.navigation.NavController
import androidx.navigation.Navigation
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

class SetAnalysisParametersFragment : Fragment(), View.OnClickListener {

    lateinit var navController: NavController
    private lateinit var gestureDetector: GestureDetector

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_set_analysis_parameters, container, false)
        setupGestureDetector(view)
        return view
    }

    private fun setupGestureDetector(view: View) {
        val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Left-to-right swipe (backward)
                            handleLeftNavigation()
                        } else {
                            // No forward navigation
                        }
                        return true
                    }
                }
                return false
            }
        }

        gestureDetector = GestureDetector(requireContext(), gestureListener)

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)
    }

    private fun handleLeftNavigation() {
        navController.popBackStack()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.left_btn -> {
                handleLeftNavigation()
            }
            R.id.home_btn -> {
                navController.navigate(R.id.mainFragment)
            }
        }
    }
}