/*
package com.spinnaker.sailing

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.spinnaker.sailing.data.GpsData
import com.spinnaker.sailing.data.RaceData
import com.spinnaker.sailing.genclasses.GeoCalculator
import com.spinnaker.sailing.location.LocationManager
import com.spinnaker.sailing.ui.RaceTimerViewModel
import com.spinnaker.sailing.ui.boatperformance.BoatPerformanceDao
import com.spinnaker.sailing.ui.boatperformance.BoatPerformanceEntity
import com.spinnaker.sailing.ui.coursemark.CourseMarkDao
import com.spinnaker.sailing.ui.coursemark.CourseMarkEntity
import com.spinnaker.sailing.ui.mark.MarkDao
import com.spinnaker.sailing.ui.race.RaceDao

import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.abs
import java.lang.Math
import android.view.GestureDetector
import android.view.MotionEvent

class InProgressRaceFragmentBkup : Fragment(),  View.OnClickListener{

    private lateinit var navController: NavController
    private lateinit var textViewCurrentMarkValue: TextView
    private lateinit var textViewNextMarkValue: TextView
    private lateinit var textViewBoatSpeedValue: TextView
    private lateinit var textViewVmgToPinValue: TextView
    private lateinit var textViewVarianceToPinValue: TextView
    private lateinit var textViewVarianceToPinLabel: TextView
    private lateinit var textViewBearingToPinValue: TextView
    private lateinit var textViewTargetSpeedValue: TextView
    private lateinit var textViewTargetSpeedOffwindValue: TextView
    private lateinit var textViewCogValue: TextView
    private lateinit var textViewKnockLiftValue: TextView

    private lateinit var courseMarkDao: CourseMarkDao
    private lateinit var markDao: MarkDao
    private lateinit var boatPerformanceDao: BoatPerformanceDao
    private lateinit var raceDao: RaceDao

    private var displayUpdateTimer: CountDownTimer? = null
    private val displayUpdateInterval: Long = 1000 // 1 second
    private val autoAdvanceDistanceNm = 25.0 / 6076.12 // 25 feet in nautical miles

    private val SMOOTH_WINDOW = 5
    private val WAVES_WINDOW = 10
    private val GUSTY_WINDOW = 10

    private val BEARING_TURN_SMOOTH_WINDOW = 2
    private val BEARING_TURN_WAVES_WINDOW = 3
    private val BEARING_TURN_GUSTY_WINDOW = 3

    private val historicalBearings = mutableListOf<Double>()
    private val historicalTurnAngles = mutableListOf<Double>()

    private lateinit var locationManager: LocationManager
    private lateinit var gestureDetector: GestureDetector

    private val raceTimerViewModel: RaceTimerViewModel by navGraphViewModels(R.id.nav_graph)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startLocationUpdatesSafely()
            } else {
                Toast.makeText(requireContext(), "Location permission denied. GPS features will not be available.", Toast.LENGTH_LONG).show()
                println("Location permission denied by user in InProgressRaceFragment.")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_in_progress_race, container, false)
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
                            // Right-to-left swipe (forward)
                            handleRightNavigation()
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
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()

        val appDatabase = AppDatabase.getDatabase(requireContext())
        courseMarkDao = appDatabase.CourseMarkDao()
        markDao = appDatabase.MarkDao()
        boatPerformanceDao = appDatabase.BoatPerformanceDao()
        raceDao = appDatabase.RaceDao()
        locationManager = LocationManager.getInstance(requireContext())

        textViewCurrentMarkValue = view.findViewById(R.id.textView_current_mark_value)
        textViewNextMarkValue = view.findViewById(R.id.textView_next_mark_value)
        textViewBoatSpeedValue = view.findViewById(R.id.textView_boat_speed_value)
        textViewVmgToPinValue = view.findViewById(R.id.textView_vmg_to_pin_value)
        textViewVarianceToPinValue = view.findViewById(R.id.textView_variance_to_pin_value)
        textViewVarianceToPinLabel = view.findViewById(R.id.textView_variance_to_pin_label)
        textViewBearingToPinValue = view.findViewById(R.id.textView_bearing_to_pin_value)
        textViewTargetSpeedValue = view.findViewById(R.id.textView_target_speed_value)
        textViewTargetSpeedOffwindValue = view.findViewById(R.id.textView_target_speed_offwind_value)
        textViewCogValue = view.findViewById(R.id.textView_cog_value)
        textViewKnockLiftValue = view.findViewById(R.id.textView_knock_lift_value)

        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_next_mark).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_prev_mark).setOnClickListener(this)

        loadAndDisplayRaceData() // Initial load
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestLocationPermission()
        startDisplayUpdates()
    }

    override fun onPause() {
        super.onPause()
        locationManager.stopLocationUpdates()
        stopDisplayUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationManager.stopLocationUpdates() // Ensure location updates are stopped
        stopDisplayUpdates() // Ensure timer is stopped
    }

    private fun checkAndRequestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startLocationUpdatesSafely()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                println("Showing permission rationale, then requesting location permission in InProgressRaceFragment.")
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun startLocationUpdatesSafely() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.startLocationUpdates()
            println("Real GPS updates started in InProgressRaceFragment.")
        } else {
            println("Attempted to start location updates without permission in InProgressRaceFragment.")
        }
    }

    private fun startDisplayUpdates() {
        if (displayUpdateTimer == null) {
            displayUpdateTimer = object : CountDownTimer(Long.MAX_VALUE, displayUpdateInterval) {
                override fun onTick(millisUntilFinished: Long) {
                    loadAndDisplayRaceData()
                }

                override fun onFinish() {
                    // Not expected to finish with Long.MAX_VALUE
                }
            }.start()
        }
    }

    private fun stopDisplayUpdates() {
        displayUpdateTimer?.cancel()
        displayUpdateTimer = null
    }

    private fun updateCurrentMarkDisplay(currentMarkName: String) {
        if (::textViewCurrentMarkValue.isInitialized && isAdded) {
            textViewCurrentMarkValue.text = currentMarkName
        }
    }

    private fun updateNextMarkDisplay(nextMarkName: String) {
        if (::textViewNextMarkValue.isInitialized && isAdded) {
            textViewNextMarkValue.text = nextMarkName
        }
    }

    private fun updateBoatSpeedDisplay(boatSpeed: String) {
        if (::textViewBoatSpeedValue.isInitialized && isAdded) {
            textViewBoatSpeedValue.text = boatSpeed
        }
    }

    private fun updateVmgToPinDisplay(vmgToPin: String) {
        if (::textViewVmgToPinValue.isInitialized && isAdded) {
            textViewVmgToPinValue.text = vmgToPin
        }
    }

    private fun updateVarianceToPinDisplay(varianceToPin: String) {
        if (::textViewVarianceToPinValue.isInitialized && isAdded) {
            textViewVarianceToPinValue.text = varianceToPin
        }
    }

    private fun updateVarianceToPinLabel(label: String) {
        if (::textViewVarianceToPinLabel.isInitialized && isAdded) {
            textViewVarianceToPinLabel.text = label
        }
    }

    private fun updateBearingToPinDisplay(bearingToPin: String) {
        if (::textViewBearingToPinValue.isInitialized && isAdded) {
            textViewBearingToPinValue.text = bearingToPin
        }
    }

    private fun updateTargetBoatSpeedDisplay(targetSpeed: String) {
        if (::textViewTargetSpeedValue.isInitialized && isAdded) {
            textViewTargetSpeedValue.text = targetSpeed
        }
    }

    private fun updateTargetBoatSpeedOffwindDisplay(targetSpeed: String) {
        if (::textViewTargetSpeedOffwindValue.isInitialized && isAdded) {
            textViewTargetSpeedOffwindValue.text = targetSpeed
        }
    }

    private fun updateCogDisplay(cog: String) {
        if (::textViewCogValue.isInitialized && isAdded) {
            textViewCogValue.text = cog
        }
    }

    private fun updateKnockLiftDisplay(knockLift: String) {
        if (::textViewKnockLiftValue.isInitialized && isAdded) {
            textViewKnockLiftValue.text = knockLift
            when {
                knockLift.contains("Lift") -> textViewKnockLiftValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                knockLift.contains("Knock") -> textViewKnockLiftValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                else -> textViewKnockLiftValue.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            }
        }
    }

    private fun calculateAverageCog(points: List<Pair<Double, Double>>): Double? {
        if (points.size < 2) return null

        val bearingsRadianList = mutableListOf<Double>()
        val geoCalc = GeoCalculator()

        for (i in 0 until points.size - 1) {
            val p1 = GeoCalculator.Coordinates(points[i].first, points[i].second)
            val p2 = GeoCalculator.Coordinates(points[i+1].first, points[i+1].second)
            if (abs(p1.latitude - p2.latitude) > 1e-7 || abs(p1.longitude - p2.longitude) > 1e-7) {
                val bearing = geoCalc.calculate(p1, p2).bearing
                bearingsRadianList.add(Math.toRadians(bearing))
            }
        }

        if (bearingsRadianList.isEmpty()) return null

        var sumX = 0.0
        var sumY = 0.0
        for (bearingRad in bearingsRadianList) {
            sumX += cos(bearingRad)
            sumY += sin(bearingRad)
        }

        if (abs(sumX) < 1e-9 && abs(sumY) < 1e-9) return null

        var avgCogDeg = Math.toDegrees(atan2(sumY, sumX))
        avgCogDeg = (avgCogDeg + 360) % 360
        return avgCogDeg
    }

    private fun calculateAverageBearing(bearings: List<Double>): Double? {
        if (bearings.isEmpty()) return null

        var sumX = 0.0
        var sumY = 0.0
        for (bearing in bearings) {
            val bearingRad = Math.toRadians(bearing)
            sumX += cos(bearingRad)
            sumY += sin(bearingRad)
        }

        if (abs(sumX) < 1e-9 && abs(sumY) < 1e-9) return null

        var avgBearingDeg = Math.toDegrees(atan2(sumY, sumX))
        avgBearingDeg = (avgBearingDeg + 360) % 360
        return avgBearingDeg
    }

    private fun calculateCurrentCog(smoothingWindow: Int): Double? {
        val allPoints = (GpsData.historicalLatitudes zip GpsData.historicalLongitudes) +
                        listOf(Pair(GpsData.currentLatitude, GpsData.currentLongitude))

        if (allPoints.size < smoothingWindow) return null

        return calculateAverageCog(allPoints.takeLast(smoothingWindow))
    }

    private fun calculateKnockLift(smoothingWindow: Int): String {
        val allPoints = (GpsData.historicalLatitudes zip GpsData.historicalLongitudes) +
                        listOf(Pair(GpsData.currentLatitude, GpsData.currentLongitude))

        val shortTermWindow = smoothingWindow
        val longTermWindow = smoothingWindow * 2

        if (allPoints.size < longTermWindow) return "---" // Not enough data for a comparison

        val longTermPoints = allPoints.takeLast(longTermWindow)
        val shortTermPoints = allPoints.takeLast(shortTermWindow)

        val longTermCog = calculateAverageCog(longTermPoints)
        val shortTermCog = calculateAverageCog(shortTermPoints)

        if (longTermCog == null || shortTermCog == null) return "---" // Cannot calculate

        val diff = shortTermCog - longTermCog
        val normalizedDiff = (diff + 540) % 360 - 180

        if (abs(normalizedDiff) > 3) { // Keep the 3-degree threshold for now
            return if (normalizedDiff > 0) {
                String.format(Locale.US, "%.0f° Lift", abs(normalizedDiff))
            } else {
                String.format(Locale.US, "%.0f° Knock", abs(normalizedDiff))
            }
        }
        return "---"
    }

    private fun handleLeftNavigation() {
        if (findNavController().currentDestination?.id == R.id.inProgressRaceFragment) {
            findNavController().navigate(R.id.startRaceFragment)
        }
    }

    private fun handleRightNavigation() {
        if (findNavController().currentDestination?.id == R.id.inProgressRaceFragment) {
            findNavController().navigate(R.id.action_inProgressRaceFragment_to_inProgressRaceFragment2)
        }
    }

    private fun advanceToNextMark() {
        lifecycleScope.launch {
            val currentCourseMarkIdFromRaceData = RaceData.course_mark_id
            if (currentCourseMarkIdFromRaceData != 0) {
                val currentCourseMarkEntity = courseMarkDao.getCourseMarkById(currentCourseMarkIdFromRaceData)
                if (currentCourseMarkEntity != null) {
                    val allCourseMarksForThisCourse = courseMarkDao.getSingleCourseMarks(currentCourseMarkEntity.courseId)
                    val sortedCourseMarks = allCourseMarksForThisCourse.sortedBy { it.courseMarkSequence }
                    var nextMarkToAdvanceTo: CourseMarkEntity? = null
                    for (potentialNextMark in sortedCourseMarks) {
                        if (potentialNextMark.courseMarkSequence > currentCourseMarkEntity.courseMarkSequence) {
                            nextMarkToAdvanceTo = potentialNextMark
                            break
                        }
                    }
                    if (nextMarkToAdvanceTo != null) {
                        RaceData.course_mark_id = nextMarkToAdvanceTo.id
                        if(isAdded) {
                            val nextMarkName = markDao.getMarkById(nextMarkToAdvanceTo.markId)?.markName ?: "Unknown"
                            val dialog = AlertDialog.Builder(requireContext())
                                .setTitle("Changing to Next Mark")
                                .setMessage(nextMarkName)
                                .setCancelable(true)
                                .show()
                            Handler(Looper.getMainLooper()).postDelayed({ dialog.dismiss() }, 2000)
                            loadAndDisplayRaceData() // Refresh display immediately
                        }
                    } else {
                        // End of course, navigate to FinishFragment
                        if (isAdded) {
                            findNavController().navigate(R.id.finishFragment)
                        }
                    }
                } else {
                    if(isAdded) Toast.makeText(context, "Error finding current mark details.", Toast.LENGTH_SHORT).show()
                }
            } else {
                if(isAdded) Toast.makeText(context, "No current mark set in RaceData.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goBackToPreviousMark() {
        lifecycleScope.launch {
            val currentCourseMarkIdFromRaceData = RaceData.course_mark_id
            if (currentCourseMarkIdFromRaceData != 0) {
                val currentCourseMarkEntity = courseMarkDao.getCourseMarkById(currentCourseMarkIdFromRaceData)
                if (currentCourseMarkEntity != null) {
                    val allCourseMarksForThisCourse = courseMarkDao.getSingleCourseMarks(currentCourseMarkEntity.courseId)
                    val sortedCourseMarks = allCourseMarksForThisCourse.sortedByDescending { it.courseMarkSequence } // sort descending
                    var prevMarkToAdvanceTo: CourseMarkEntity? = null
                    for (potentialPrevMark in sortedCourseMarks) {
                        if (potentialPrevMark.courseMarkSequence < currentCourseMarkEntity.courseMarkSequence) { // find smaller sequence
                            prevMarkToAdvanceTo = potentialPrevMark
                            break
                        }
                    }
                    if (prevMarkToAdvanceTo != null) {
                        RaceData.course_mark_id = prevMarkToAdvanceTo.id
                        if(isAdded) {
                            val prevMarkName = markDao.getMarkById(prevMarkToAdvanceTo.markId)?.markName ?: "Unknown"
                            val dialog = AlertDialog.Builder(requireContext())
                                .setTitle("Changing to Previous Mark")
                                .setMessage(prevMarkName)
                                .setCancelable(true)
                                .show()
                            Handler(Looper.getMainLooper()).postDelayed({ dialog.dismiss() }, 2000)
                            loadAndDisplayRaceData() // Refresh display immediately
                        }
                    } else {
                        if(isAdded) Toast.makeText(context, "This is the first mark.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if(isAdded) Toast.makeText(context, "Error finding current mark details.", Toast.LENGTH_SHORT).show()
                }
            } else {
                if(isAdded) Toast.makeText(context, "No current mark set in RaceData.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.left_btn -> handleLeftNavigation()
            R.id.right_btn -> handleRightNavigation()
            R.id.home_btn -> {
                if (findNavController().currentDestination?.id == R.id.inProgressRaceFragment) {
                    findNavController().navigate(R.id.startRaceFragment)
                }
            }
            R.id.button_next_mark -> advanceToNextMark()
            R.id.button_prev_mark -> goBackToPreviousMark()
        }
    }
}
*/