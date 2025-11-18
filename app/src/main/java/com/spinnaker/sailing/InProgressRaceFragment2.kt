package com.spinnaker.sailing

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
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
import androidx.navigation.findNavController
import androidx.navigation.navGraphViewModels
import com.spinnaker.sailing.data.GpsData
import com.spinnaker.sailing.data.RaceData
import com.spinnaker.sailing.genclasses.GeoCalculator
import com.spinnaker.sailing.location.LocationManager
import com.spinnaker.sailing.ui.RaceTimerViewModel
import com.spinnaker.sailing.ui.boatperformance.BoatPerformanceDao
import com.spinnaker.sailing.ui.coursemark.CourseMarkDao
import com.spinnaker.sailing.ui.mark.MarkDao
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.abs
import android.view.GestureDetector
import android.view.MotionEvent

class InProgressRaceFragment2 : Fragment(),  View.OnClickListener{

    private lateinit var navController: NavController
    private lateinit var textViewNextMarkValue: TextView
    private lateinit var textViewVarianceToMarkValue: TextView
    private lateinit var textViewBearingToMarkValue: TextView

    private lateinit var courseMarkDao: CourseMarkDao
    private lateinit var markDao: MarkDao
    private lateinit var boatPerformanceDao: BoatPerformanceDao

    private lateinit var locationManager: LocationManager
    private lateinit var gestureDetector: GestureDetector

    private val raceTimerViewModel: RaceTimerViewModel by navGraphViewModels(R.id.nav_graph)

    private val locationListener: (Location) -> Unit = {
        loadAndDisplayRaceData()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, LocationManager will start updates when a listener is added.
            } else {
                Toast.makeText(requireContext(), "Location permission denied. GPS features will not be available.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_in_progress_race2, container, false)
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
                            handleLeftNavigation()
                        } else {
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
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = view.findNavController()

        val appDatabase = AppDatabase.getDatabase(requireContext())
        courseMarkDao = appDatabase.CourseMarkDao()
        markDao = appDatabase.MarkDao()
        boatPerformanceDao = appDatabase.BoatPerformanceDao()
        locationManager = LocationManager.getInstance(requireContext())
        locationManager.addLocationListener(locationListener)

        textViewNextMarkValue = view.findViewById(R.id.textView_next_mark_value)
        textViewVarianceToMarkValue = view.findViewById(R.id.textView_variance_to_mark_value)
        textViewBearingToMarkValue = view.findViewById(R.id.textView_bearing_to_mark_value)

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
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationManager.removeLocationListener(locationListener)
    }

    private fun checkAndRequestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun updateNextMarkDisplay(nextMarkName: String) {
        if (::textViewNextMarkValue.isInitialized && isAdded) {
            textViewNextMarkValue.text = nextMarkName
        }
    }

    private fun updateVarianceToMarkDisplay(varianceToMark: String) {
        if (::textViewVarianceToMarkValue.isInitialized && isAdded) {
            textViewVarianceToMarkValue.text = varianceToMark
        }
    }

    private fun updateBearingToMarkDisplay(bearingToMark: String) {
        if (::textViewBearingToMarkValue.isInitialized && isAdded) {
            textViewBearingToMarkValue.text = bearingToMark
        }
    }
    
    private fun calculateCurrentCog(): Double? {
        val allPoints = (GpsData.historicalLatitudes zip GpsData.historicalLongitudes) +
                        listOf(Pair(GpsData.currentLatitude, GpsData.currentLongitude))

        if (allPoints.size < 2) return null

        val bearingsRadianList = mutableListOf<Double>()
        val geoCalc = GeoCalculator()

        for (i in 0 until allPoints.size - 1) {
            val p1 = GeoCalculator.Coordinates(allPoints[i].first, allPoints[i].second)
            val p2 = GeoCalculator.Coordinates(allPoints[i+1].first, allPoints[i+1].second)
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

    private fun handleLeftNavigation() {
        if (navController.currentDestination?.id == R.id.inProgressRaceFragment2) {
            navController.popBackStack()
        }
    }

    private fun handleRightNavigation() {
        if (navController.currentDestination?.id == R.id.inProgressRaceFragment2) {
            navController.navigate(R.id.action_inProgressRaceFragment2_to_finishFragment)
        }
    }

    private fun loadAndDisplayRaceData(){
        if (!isAdded) return

        lifecycleScope.launch {
            var nextMarkNameToDisplay = "N/A"
            var varianceToMarkDisplay = "N/A"
            var bearingToMarkDisplay = "N/A"

            val currentCourseMarkId = RaceData.course_mark_id
            if (currentCourseMarkId != 0) {
                val currentCourseMarkEntity = courseMarkDao.getCourseMarkById(currentCourseMarkId)
                if (currentCourseMarkEntity != null) {
                    val allCourseMarksForThisCourse = courseMarkDao.getSingleCourseMarks(currentCourseMarkEntity.courseId)
                    val sortedCourseMarks = allCourseMarksForThisCourse.sortedBy { it.courseMarkSequence }
                    val currentMarkIndex = sortedCourseMarks.indexOfFirst { it.id == currentCourseMarkEntity.id }

                    if (currentMarkIndex != -1 && currentMarkIndex + 1 < sortedCourseMarks.size) {
                        val nextCourseMark = sortedCourseMarks[currentMarkIndex + 1]
                        val nextMarkEntity = markDao.getMarkById(nextCourseMark.markId)
                        if(nextMarkEntity != null) {
                            nextMarkNameToDisplay = "Next: ${nextMarkEntity.markName}"
                            val currentCog = calculateCurrentCog()
                            if(currentCog != null) {
                                val bearingToNextMark = GeoCalculator().calculate(
                                    GeoCalculator.Coordinates(GpsData.currentLatitude, GpsData.currentLongitude),
                                    GeoCalculator.Coordinates(nextMarkEntity.markLatitude, nextMarkEntity.markLongitude)
                                ).bearing
                                
                                bearingToMarkDisplay = String.format(Locale.US, "%.0f°", bearingToNextMark)
                                
                                val variance = (currentCog - bearingToNextMark + 540) % 360 - 180
                                varianceToMarkDisplay = String.format(Locale.US, "%.0f°", variance)
                            } else {
                                varianceToMarkDisplay = "---"
                                bearingToMarkDisplay = "---"
                            }
                        } else {
                           nextMarkNameToDisplay = "Next: Error"
                        }
                    } else {
                        nextMarkNameToDisplay = "Next: Finish"
                    }
                }
            } else {
                nextMarkNameToDisplay = "No Course"
            }
            updateNextMarkDisplay(nextMarkNameToDisplay)
            updateVarianceToMarkDisplay(varianceToMarkDisplay)
            updateBearingToMarkDisplay(bearingToMarkDisplay)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.left_btn -> handleLeftNavigation()
            R.id.right_btn -> handleRightNavigation()
            R.id.home_btn -> navController.navigate(R.id.mainFragment)
            R.id.button_next_mark -> {
                // Placeholder for advancing to the next mark
                Toast.makeText(context, "Next Mark Clicked", Toast.LENGTH_SHORT).show()
            }
            R.id.button_prev_mark -> {
                 // Placeholder for going to the previous mark
                Toast.makeText(context, "Previous Mark Clicked", Toast.LENGTH_SHORT).show()
            }
        }
    }
}