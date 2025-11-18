package com.spinnaker.sailing

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.navGraphViewModels
import com.spinnaker.sailing.data.RaceData
import com.spinnaker.sailing.services.GpsLoggingService
import com.spinnaker.sailing.ui.RaceTimerViewModel
import com.spinnaker.sailing.ui.boat.BoatDao
import com.spinnaker.sailing.ui.race.RaceDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

class FinishFragment : Fragment(), View.OnClickListener {

    lateinit var navController: NavController
    private lateinit var textViewFinishTimer: TextView
    private lateinit var textViewAdjustedTime: TextView
    private lateinit var buttonFinishRace: Button
    private lateinit var boatDao: BoatDao
    private lateinit var raceDao: RaceDao
    private lateinit var gestureDetector: GestureDetector

    private val raceTimerViewModel: RaceTimerViewModel by navGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_finish, container, false)
        setupGestureDetector(view)
        return view
    }

    @SuppressLint("ClickableViewAccessibility")
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
                            // No forward navigation from finish
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

        val appDatabase = AppDatabase.getDatabase(requireContext())
        boatDao = appDatabase.BoatDao()
        raceDao = appDatabase.RaceDao()

        textViewFinishTimer = view.findViewById(R.id.textView_finish_timer)
        textViewAdjustedTime = view.findViewById(R.id.textView_adjusted_time)
        buttonFinishRace = view.findViewById(R.id.button_finish_race)

        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)
        buttonFinishRace.setOnClickListener(this)

        raceTimerViewModel.currentTime.observe(viewLifecycleOwner) { millis ->
            val totalSeconds = millis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            textViewFinishTimer.text = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }
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
            R.id.button_finish_race -> {
                raceTimerViewModel.stopTimer()

                // Stop the logging service immediately
                val intent = Intent(requireActivity(), GpsLoggingService::class.java)
                requireActivity().stopService(intent)

                lifecycleScope.launch {
                    val raceId = RaceData.race_id
                    if (raceId != 0L) {
                        val race = raceDao.getRaceById(raceId.toString())
                        if (race != null) {
                            val elapsedTimeInMillis = raceTimerViewModel.currentTime.value ?: 0L
                            val elapsedTimeInSeconds = elapsedTimeInMillis / 1000
                            val finishTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                            val updatedRace = race.copy(finishTime = finishTime)
                            raceDao.update(updatedRace)

                            val boat = boatDao.getBoatById(RaceData.boat_id)
                            if (boat != null) {
                                // Using a common Time-on-Time formula for PHRF
                                val tcf = 650.0 / (550.0 + boat.pHRF)
                                val correctedTimeInSeconds = (elapsedTimeInSeconds * tcf).toLong()

                                val hours = correctedTimeInSeconds / 3600
                                val minutes = (correctedTimeInSeconds % 3600) / 60
                                val seconds = correctedTimeInSeconds % 60
                                textViewAdjustedTime.text = String.format(Locale.US, "Adjusted: %02d:%02d:%02d", hours, minutes, seconds)
                            } else {
                                textViewAdjustedTime.text = "Adjusted: Boat not found"
                            }
                            Toast.makeText(context, "Race Finished!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
