package com.spinnaker.sailing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.spinnaker.sailing.data.GpsData
import com.spinnaker.sailing.data.RaceData
import com.spinnaker.sailing.genclasses.GeoCalculator
import com.spinnaker.sailing.location.LocationManager
import com.spinnaker.sailing.services.GpsLoggingService
import com.spinnaker.sailing.ui.RaceTimerViewModel
import com.spinnaker.sailing.ui.boatperformance.BoatPerformanceDao
import com.spinnaker.sailing.ui.coursemark.CourseMarkDao
import com.spinnaker.sailing.ui.mark.MarkDao
import com.spinnaker.sailing.ui.race.RaceDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class StartRaceFragment : Fragment(), View.OnClickListener {

    private lateinit var navController: NavController
    private lateinit var countdownTimerDisplay: TextView
    private lateinit var turnaroundTimeDisplay: TextView
    private lateinit var burnTimeDisplay: TextView
    private lateinit var currentSpeedDisplay: TextView
    private lateinit var startTackComment: EditText
    private lateinit var startEndComment: EditText
    private lateinit var gestureDetector: GestureDetector

    private lateinit var boatPerformanceDao: BoatPerformanceDao
    private lateinit var courseMarkDao: CourseMarkDao
    private lateinit var markDao: MarkDao
    private lateinit var raceDao: RaceDao
    private lateinit var locationManager: LocationManager

    private val raceTimerViewModel: RaceTimerViewModel by navGraphViewModels(R.id.nav_graph)
    private val defaultCountdownMillis: Long = 5 * 60 * 1000 // 5 minutes

    sealed class TimeToLineResult {
        data class TimeToLine(val time: Double) : TimeToLineResult()
        object CourseToPin : TimeToLineResult()
        object CourseToBoat : TimeToLineResult()
        object CourseBelowPin : TimeToLineResult()
        object CourseBelowBoat : TimeToLineResult()
        object NoIntersection : TimeToLineResult()
        object PathParallel : TimeToLineResult()
        object BoatStationary : TimeToLineResult()
        data class DataError(val message: String) : TimeToLineResult()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startGpsLogging()
            } else {
                Toast.makeText(requireContext(), "Location permission denied. GPS logging will not be available.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_start_race, container, false)
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

                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        handleLeftNavigation()
                    } else {
                        handleRightNavigation()
                    }
                    return true
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
        navController = Navigation.findNavController(view)

        val appDatabase = AppDatabase.getDatabase(requireContext())
        boatPerformanceDao = appDatabase.BoatPerformanceDao()
        courseMarkDao = appDatabase.CourseMarkDao()
        markDao = appDatabase.MarkDao()
        raceDao = appDatabase.RaceDao()

        locationManager = LocationManager.getInstance(requireContext())
        locationManager.addLocationListener { 
            updateSpeedDisplay()
            updateDisplayTimeSensitiveValues(raceTimerViewModel.currentTime.value ?: defaultCountdownMillis)
        }

        countdownTimerDisplay = view.findViewById(R.id.textView_countdown_timer_display)
        turnaroundTimeDisplay = view.findViewById(R.id.textView_turnaround_time_display)
        burnTimeDisplay = view.findViewById(R.id.textView_burn_time_display)
        currentSpeedDisplay = view.findViewById(R.id.textView_current_speed_display)

        startTackComment = view.findViewById(R.id.editText_start_tack_comment)
        startEndComment = view.findViewById(R.id.editText_start_end_comment)

        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)

        view.findViewById<Button>(R.id.button_timer_start).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_timer_stop).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_timer_reset).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_timer_set5).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_timer_set4).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_timer_set1).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_calculate_turnaround).setOnClickListener(this)

        view.post {
            raceTimerViewModel.currentTime.observe(viewLifecycleOwner) { millis ->
                if(isAdded) {
                    val isCountingUp = raceTimerViewModel.isCountingUp.value ?: false
                    countdownTimerDisplay.text = formatDurationMillis(millis, isCountingUp)
                    if (isCountingUp) {
                        countdownTimerDisplay.setTextColor(Color.RED)
                    } else {
                        countdownTimerDisplay.setTextColor(Color.GREEN)
                    }
                }
                updateDisplayTimeSensitiveValues(millis)
                updateSpeedDisplay()
            }

            raceTimerViewModel.isTimerRunning.observe(viewLifecycleOwner) {
                // UI updates based on timer running state can go here
            }

            raceTimerViewModel.timerFinished.observe(viewLifecycleOwner) { finished ->
                if (finished) {
                    lifecycleScope.launch {
                        val raceId = RaceData.race_id
                        if (raceId != 0L) {
                            val race = raceDao.getRaceById(raceId.toString())
                            if (race != null) {
                                val startTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                                val updatedRace = race.copy(startTime = startTime)
                                raceDao.update(updatedRace)
                                Toast.makeText(context, "Race started!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        raceTimerViewModel.onTimerFinishHandled()
                        if (navController.currentDestination?.id == R.id.startRaceFragment) {
                            findNavController().navigate(R.id.action_startRaceFragment_to_inProgressRaceFragment1)
                        }
                    }
                }
            }

            if (raceTimerViewModel.currentTime.value == null || raceTimerViewModel.isTimerRunning.value == false) {
                if(isAdded) {
                    countdownTimerDisplay.text = formatDurationMillis(defaultCountdownMillis, false)
                    countdownTimerDisplay.setTextColor(Color.GREEN)
                }
                updateDisplayTimeSensitiveValues(defaultCountdownMillis)
            }

            if(isAdded) turnaroundTimeDisplay.text = getString(R.string._00_00)
            updateSpeedDisplay()
            setBoatLocationToFirstMark()
            updateStartingTack()
            calculateLineBias()
        }
    }

    private fun handleLeftNavigation() {
        if (navController.currentDestination?.id == R.id.startRaceFragment) {
            findNavController().navigate(R.id.action_startRaceFragment_to_setUpRaceFragment)
        }
    }

    private fun handleRightNavigation() {
        if (navController.currentDestination?.id == R.id.startRaceFragment) {
            findNavController().navigate(R.id.action_startRaceFragment_to_inProgressRaceFragment1)
        }
    }

    private fun handleHomeNavigation() {
         if (navController.currentDestination?.id == R.id.startRaceFragment) {
            navController.navigate(R.id.mainFragment)
        }
    }

    private fun setBoatLocationToFirstMark() {
        lifecycleScope.launch {
            val currentCourseId = RaceData.course_id
            if (currentCourseId != 0) {
                val courseMarks = courseMarkDao.getSingleCourseMarks(currentCourseId)
                if (courseMarks.isNotEmpty()) {
                    val firstCourseMark = courseMarks.minByOrNull { it.courseMarkSequence }
                    if (firstCourseMark != null) {
                        RaceData.course_mark_id = firstCourseMark.id
                        val markEntity = markDao.getMarkById(firstCourseMark.markId)
                        if (markEntity != null) {
                            GpsData.clearData()
                            val currentTime = System.currentTimeMillis()
                            GpsData.updateLocation(
                                newLatitude = markEntity.markLatitude - 0.00002,
                                newLongitude = markEntity.markLongitude - 0.00002,
                                newAccuracy = 1.0f,
                                newTimestamp = currentTime - 2000
                            )
                            GpsData.updateLocation(
                                newLatitude = markEntity.markLatitude - 0.00001,
                                newLongitude = markEntity.markLongitude - 0.00001,
                                newAccuracy = 1.0f,
                                newTimestamp = currentTime - 1000
                            )
                            GpsData.updateLocation(
                                newLatitude = markEntity.markLatitude,
                                newLongitude = markEntity.markLongitude,
                                newAccuracy = 1.0f,
                                newTimestamp = currentTime
                            )
                            if(isAdded) Toast.makeText(context, "GPS set to first mark: ${markEntity.markName}", Toast.LENGTH_LONG).show()
                            updateSpeedDisplay()
                        } else {
                            if(isAdded) Toast.makeText(context, "First mark details not found.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        if(isAdded) Toast.makeText(context, "Could not determine first mark.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if(isAdded) Toast.makeText(context, "Selected course has no marks.", Toast.LENGTH_SHORT).show()
                }
            } else {
                if(isAdded) Toast.makeText(context, "No course selected.", Toast.LENGTH_SHORT).show()
            }
            updateStartingTack()
            calculateLineBias()
            updateDisplayTimeSensitiveValues(raceTimerViewModel.currentTime.value ?: defaultCountdownMillis)
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestLocationPermission()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun checkAndRequestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startGpsLogging()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun startGpsLogging() {
        val raceId = RaceData.race_id
        if (raceId == 0L) {
            Toast.makeText(context, "Cannot start logging without a race.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(requireActivity(), GpsLoggingService::class.java).apply {
            putExtra(GpsLoggingService.EXTRA_RACE_ID, raceId)
        }
        requireActivity().startService(intent)
    }


    private fun updateSpeedDisplay() {
        val speedKnots = GpsData.calculateSpeedInKnots()
        if(isAdded) currentSpeedDisplay.text = String.format(Locale.US, "%.1f knots", speedKnots)
    }

    private fun updateStartingTack() {
        lifecycleScope.launch {
            val currentCourseId = RaceData.course_id
            if (currentCourseId == 0) {
                if(isAdded) {
                    startTackComment.setText(R.string.no_course_selected)
                }
                return@launch
            }

            val courseMarks = courseMarkDao.getSingleCourseMarks(currentCourseId)
            if (courseMarks.size < 2) {
                if(isAdded) {
                    startTackComment.setText(R.string.not_enough_marks)
                }
                return@launch
            }

            val firstMarkEntity = courseMarks.minByOrNull { it.courseMarkSequence }
            val secondMarkEntity = courseMarks.filter { firstMarkEntity != null && it.courseMarkSequence > firstMarkEntity.courseMarkSequence }.minByOrNull { it.courseMarkSequence }

            if (firstMarkEntity == null || secondMarkEntity == null) {
                if(isAdded) {
                    startTackComment.setText(R.string.marks_not_found)
                }
                return@launch
            }

            val firstMark = markDao.getMarkById(firstMarkEntity.markId)
            val secondMark = markDao.getMarkById(secondMarkEntity.markId)

            if (firstMark == null || secondMark == null) {
                if(isAdded) {
                    startTackComment.setText(R.string.mark_details_not_found)
                }
                return@launch
            }

            val bearing = GeoCalculator().calculate(
                GeoCalculator.Coordinates(firstMark.markLatitude, firstMark.markLongitude),
                GeoCalculator.Coordinates(secondMark.markLatitude, secondMark.markLongitude)
            ).bearing

            val windDirection = RaceData.wind_direction
            val angle = (bearing - windDirection + 540) % 360 - 180

            if (abs(angle) > 90) {
                if(isAdded) {
                    startTackComment.setText(R.string.downwind_start_is_unlikely)
                }
            } else {
                if (angle > 0) {
                    if(isAdded) {
                        startTackComment.setText(R.string.port_tack_favoured)
                    }
                } else {
                    if(isAdded) {
                        startTackComment.setText(R.string.starboard_tack_favoured)
                    }
                }
            }
        }
    }

    private fun calculateLineBias() {
        // TODO: This is a placeholder. Implement actual line bias calculation.
        if(isAdded) {
            startEndComment.setText(R.string.line_bias_placeholder)
        }
    }
    
    private fun formatDurationMillis(millis: Long, isCountingUp: Boolean): String {
        val totalSeconds = abs(millis) / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (isCountingUp) {
            String.format(Locale.US, "+%02d:%02d", minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    private fun updateDisplayTimeSensitiveValues(currentTime: Long) {
        // TODO: This is a placeholder. Implement actual time-to-line and burn time calculations.
        if(isAdded) {
            burnTimeDisplay.text = getString(R.string._00_00)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.button_timer_start -> raceTimerViewModel.startTimer(raceTimerViewModel.currentTime.value ?: defaultCountdownMillis)
            R.id.button_timer_stop -> raceTimerViewModel.stopTimer()
            R.id.button_timer_reset -> raceTimerViewModel.resetTimer(defaultCountdownMillis)
            R.id.button_timer_set5 -> raceTimerViewModel.resetTimer(5 * 60 * 1000)
            R.id.button_timer_set4 -> raceTimerViewModel.resetTimer(4 * 60 * 1000)
            R.id.button_timer_set1 -> raceTimerViewModel.resetTimer(1 * 60 * 1000)
            R.id.button_calculate_turnaround -> {
                // TODO: Implement turnaround calculation
                Toast.makeText(context, "Calculate Turnaround clicked", Toast.LENGTH_SHORT).show()
            }
            R.id.left_btn -> handleLeftNavigation()
            R.id.right_btn -> handleRightNavigation()
            R.id.home_btn -> handleHomeNavigation()
        }
    }
}
