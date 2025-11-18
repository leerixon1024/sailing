package com.spinnaker.sailing

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
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
import android.widget.LinearLayout
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
import kotlin.math.sqrt
import java.lang.Math
import android.view.GestureDetector
import android.view.MotionEvent

class InProgressRaceFragment : Fragment(),  View.OnClickListener{

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
    private lateinit var textViewCurrentSpeedValue: TextView
    private lateinit var textViewCurrentDirectionValue: TextView
    private lateinit var textViewLeewayValue: TextView
    private lateinit var varianceToPinRowLayout: LinearLayout
    private lateinit var knockLiftRowLayout: LinearLayout

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
    private val cogHistory = mutableListOf<Double>()

    private lateinit var locationManager: LocationManager
    private lateinit var gestureDetector: GestureDetector

    private val raceTimerViewModel: RaceTimerViewModel by navGraphViewModels(R.id.nav_graph)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permissions are now granted, start using location services
            } else {
                Toast.makeText(requireContext(), "Location permission denied. GPS features will not be available.", Toast.LENGTH_LONG).show()
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
                val diffY = e2.y - e1.y

                if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY < 0) {
                        // Swipe up
                        handleUpNavigation()
                    } else {
                        // Swipe down
                        handleDownNavigation()
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
        navController = findNavController()

        val appDatabase = AppDatabase.getDatabase(requireContext())
        courseMarkDao = appDatabase.CourseMarkDao()
        markDao = appDatabase.MarkDao()
        boatPerformanceDao = appDatabase.BoatPerformanceDao()
        raceDao = appDatabase.RaceDao()
        locationManager = LocationManager.getInstance(requireContext())
        locationManager.addLocationListener { 
            loadAndDisplayRaceData()
        }

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
        textViewCurrentSpeedValue = view.findViewById(R.id.textView_current_speed_value)
        textViewCurrentDirectionValue = view.findViewById(R.id.textView_current_direction_value)
        textViewLeewayValue = view.findViewById(R.id.textView_leeway_value)
        varianceToPinRowLayout = view.findViewById(R.id.variance_to_pin_row_layout)
        knockLiftRowLayout = view.findViewById(R.id.knock_lift_row_layout)

        loadAndDisplayRaceData() // Initial load
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestLocationPermission()
        startDisplayUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopDisplayUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopDisplayUpdates() // Ensure timer is stopped
    }

    private fun checkAndRequestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
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

    private fun updateTargetSpeedOffwindDisplay(targetSpeed: String) {
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
                knockLift.contains("Lift") -> knockLiftRowLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
                knockLift.contains("Knock") -> knockLiftRowLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                else -> knockLiftRowLayout.setBackgroundColor(Color.BLACK)
            }
        }
    }

    private fun updateCurrentSpeedDisplay(currentSpeed: String) {
        if (::textViewCurrentSpeedValue.isInitialized && isAdded) {
            textViewCurrentSpeedValue.text = currentSpeed
        }
    }

    private fun updateCurrentDirectionDisplay(currentDirection: String) {
        if (::textViewCurrentDirectionValue.isInitialized && isAdded) {
            textViewCurrentDirectionValue.text = currentDirection
        }
    }

    private fun updateLeewayDisplay(leeway: String) {
        if (::textViewLeewayValue.isInitialized && isAdded) {
            textViewLeewayValue.text = leeway
        }
    }

    private fun calculateAverageBearing(bearings: List<Double>): Double {
        if (bearings.isEmpty()) return 0.0

        var sumX = 0.0
        var sumY = 0.0
        for (bearing in bearings) {
            val bearingRad = Math.toRadians(bearing)
            sumX += cos(bearingRad)
            sumY += sin(bearingRad)
        }

        if (abs(sumX) < 1e-9 && abs(sumY) < 1e-9) return 0.0

        var avgBearingDeg = Math.toDegrees(atan2(sumY, sumX))
        avgBearingDeg = (avgBearingDeg + 360) % 360
        return avgBearingDeg
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

        return calculateAverageBearing(bearingsRadianList.map { Math.toDegrees(it) })
    }
    
    private fun calculateTideAdjustedCog(boatHeading: Double, boatSpeed: Double, tideSpeed: Double, tideDirection: Double): Double {
        val boatSpeedX = boatSpeed * sin(Math.toRadians(boatHeading))
        val boatSpeedY = boatSpeed * cos(Math.toRadians(boatHeading))

        val tideSpeedX = tideSpeed * sin(Math.toRadians(tideDirection))
        val tideSpeedY = tideSpeed * cos(Math.toRadians(tideDirection))

        val totalSpeedX = boatSpeedX + tideSpeedX
        val totalSpeedY = boatSpeedY + tideSpeedY

        val resultingCog = (Math.toDegrees(atan2(totalSpeedX, totalSpeedY)) + 360) % 360
        return resultingCog
    }

    private fun handleUpNavigation() {
        if (findNavController().currentDestination?.id == R.id.inProgressRaceFragment) {
            findNavController().navigate(R.id.action_inProgressRaceFragment_to_inProgressRaceFragment1)
        }
    }

    private fun handleDownNavigation() {
        if (findNavController().currentDestination?.id == R.id.inProgressRaceFragment) {
            findNavController().navigate(R.id.action_inProgressRaceFragment_to_inProgressRaceFragment1)
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

    private fun loadAndDisplayRaceData(){
        if (!isAdded) return

        lifecycleScope.launch {
            val race = raceDao.getRaceById(RaceData.race_id.toString())

            var currentMarkNameToDisplay = "N/A"
            var nextMarkNameToDisplay = "N/A"
            var vmgToDisplay = "N/A"
            var varianceToPinDisplay = "N/A"
            var bearingToPinDisplay = "N/A"
            var cogToDisplay = "N/A"
            var knockLiftToDisplay = "N/A"
            var targetBoatSpeedUpwindToDisplay = "N/A"
            var targetBoatSpeedOffwindToDisplay = "N/A"
            var leewayToDisplay = "N/A"

            val tidalSpeedToDisplay = String.format(Locale.US, "%.1f kn", RaceData.current_speed)
            val tidalDirectionToDisplay = String.format(Locale.US, "%d°", RaceData.current_direction)

            val currentBoatId = RaceData.boat_id
            val actualTrueWindSpeed = RaceData.wind_strength
            var closestProfile: BoatPerformanceEntity? = null

            if (currentBoatId != 0) {
                val performanceProfiles = boatPerformanceDao.getSpecificBoatPerformance(currentBoatId)
                if (performanceProfiles.isNotEmpty()) {
                    var minDifference = Double.MAX_VALUE
                    for (profile in performanceProfiles) {
                        val difference = abs(profile.trueWind - actualTrueWindSpeed)
                        if (difference < minDifference) {
                            minDifference = difference.toDouble()
                            closestProfile = profile
                        }
                    }
                    if (closestProfile != null) {
                        val targetSpeedUpwind = closestProfile!!.targetSpeedUpwind
                        targetBoatSpeedUpwindToDisplay = String.format(Locale.US, "%.1f kn", targetSpeedUpwind)

                        val targetSpeedOffwind = closestProfile!!.targetSpeedOffwind
                        targetBoatSpeedOffwindToDisplay = String.format(Locale.US, "%.1f kn", targetSpeedOffwind)

                        leewayToDisplay = String.format(Locale.US, "%.1f°", closestProfile!!.leeway)
                    } else {
                        targetBoatSpeedUpwindToDisplay = "N/A"
                        targetBoatSpeedOffwindToDisplay = "N/A"
                        leewayToDisplay = "N/A"
                    }
                } else {
                    targetBoatSpeedUpwindToDisplay = "No Perf Data"
                    targetBoatSpeedOffwindToDisplay = "No Perf Data"
                    leewayToDisplay = "No Perf Data"
                }
            } else {
                targetBoatSpeedUpwindToDisplay = "Boat ID N/A"
                targetBoatSpeedOffwindToDisplay = "Boat ID N/A"
                leewayToDisplay = "Boat ID N/A"
            }

            val currentCourseMarkId = RaceData.course_mark_id
            if (currentCourseMarkId != 0) {
                val currentCourseMarkEntity = courseMarkDao.getCourseMarkById(currentCourseMarkId)
                if (currentCourseMarkEntity != null) {
                    val targetMarkEntityForData = markDao.getMarkById(currentCourseMarkEntity.markId)
                    if (targetMarkEntityForData != null) {

                        currentMarkNameToDisplay = targetMarkEntityForData.markName

                        val allCourseMarksForThisCourse = courseMarkDao.getSingleCourseMarks(currentCourseMarkEntity.courseId)
                        val sortedCourseMarks = allCourseMarksForThisCourse.sortedBy { it.courseMarkSequence }
                        val currentMarkIndex = sortedCourseMarks.indexOfFirst { it.id == currentCourseMarkEntity.id }

                        if (currentMarkIndex != -1 && currentMarkIndex + 1 < sortedCourseMarks.size) {
                            val nextCourseMark = sortedCourseMarks[currentMarkIndex + 1]
                            val nextMarkDetailEntity = markDao.getMarkById(nextCourseMark.markId)
                            nextMarkNameToDisplay = nextMarkDetailEntity?.markName ?: "Error"
                        } else {
                            nextMarkNameToDisplay = "Finish"
                        }

                        val boatCurrentPos = GeoCalculator.Coordinates(GpsData.currentLatitude, GpsData.currentLongitude)
                        val bearingToTarget = GeoCalculator().calculate(boatCurrentPos, GeoCalculator.Coordinates(targetMarkEntityForData.markLatitude, targetMarkEntityForData.markLongitude)).bearing
                        val windDirection = RaceData.wind_direction
                        val angleToWind = (bearingToTarget - windDirection + 540) % 360 - 180

                        val isUpwind = abs(angleToWind) < 60
                        val currentCog = calculateCurrentCog()
                        val currentTack = if(currentCog != null) if ((currentCog - windDirection + 360) % 360 < 180) "Starboard" else "Port" else ""

                        if(isUpwind) {
                            if(currentCog != null) {
                                cogHistory.add(currentCog)
                                if (cogHistory.size > 10) {
                                    cogHistory.removeAt(0)
                                }

                                if (cogHistory.size == 10) {
                                    val oldCogAvg = calculateAverageBearing(cogHistory.subList(0, 5))
                                    val newCogAvg = calculateAverageBearing(cogHistory.subList(5, 10))
                                    var kl = newCogAvg - oldCogAvg
                                    if (kl > 180) kl -= 360
                                    if (kl < -180) kl += 360

                                    if (abs(kl) > 3) { 
                                        val isLift = (currentTack == "Starboard" && kl < 0) || (currentTack == "Port" && kl > 0)
                                        knockLiftToDisplay = if (isLift) {
                                            String.format(Locale.US, "%.0f° Lift", abs(kl))
                                        } else {
                                            String.format(Locale.US, "%.0f° Knock", abs(kl))
                                        }
                                    } else {
                                        knockLiftToDisplay = "---"
                                    }
                                } else {
                                    knockLiftToDisplay = "---"
                                }
                            } else {
                                knockLiftToDisplay = "---"
                            }

                            updateVarianceToPinLabel(getString(R.string.fetch_angle_label))
                            val tackAngle = closestProfile?.tackAngle?.toDouble() ?: 45.0
                            val boatSpeed = GpsData.calculateSpeedInKnots()
                            val tideSpeed = RaceData.current_speed
                            val tideDirection = RaceData.current_direction.toDouble()
                            
                            val starboardLayline = calculateTideAdjustedCog(windDirection - tackAngle, boatSpeed, tideSpeed.toDouble(), tideDirection)
                            val portLayline = calculateTideAdjustedCog(windDirection + tackAngle, boatSpeed, tideSpeed.toDouble(), tideDirection)

                            val fetchAngle = if (currentTack == "Starboard") {
                                (bearingToTarget - starboardLayline + 540) % 360 - 180
                            } else {
                                (portLayline - bearingToTarget + 540) % 360 - 180
                            }

                            varianceToPinDisplay = String.format(Locale.US, "%.0f°", fetchAngle)
                            bearingToPinDisplay = String.format(Locale.US, "%.0f°", bearingToTarget)

                            if (fetchAngle > 0) {
                                varianceToPinRowLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
                            } else {
                                varianceToPinRowLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                            }

                        } else {
                            knockLiftToDisplay = "Offwind"
                            varianceToPinRowLayout.setBackgroundColor(Color.BLACK)
                            updateVarianceToPinLabel(getString(R.string.variance_to_pin_label))

                            val sogKnots = GpsData.calculateSpeedInKnots()
                            val cogTrue = currentCog

                            if (cogTrue != null) {
                                cogToDisplay = String.format(Locale.US, "%.0f°", cogTrue)
                            } else {
                                cogToDisplay = "N/A"
                            }

                            if (boatCurrentPos.latitude != 0.0 || boatCurrentPos.longitude != 0.0) {
                                val targetCoords = GeoCalculator.Coordinates(targetMarkEntityForData.markLatitude, targetMarkEntityForData.markLongitude)
                                val distanceToTargetNm = GeoCalculator.calculateDistanceNm(boatCurrentPos, targetCoords)
                                val bearingToTargetSmoothed = GeoCalculator().calculate(boatCurrentPos, targetCoords).bearing
                                
                                historicalBearings.add(bearingToTargetSmoothed)
                                if (historicalBearings.size > 2) {
                                    historicalBearings.removeAt(0)
                                }
                                val smoothedBearing = calculateAverageBearing(historicalBearings)
                                bearingToPinDisplay = String.format(Locale.US, "%.0f°", smoothedBearing ?: bearingToTargetSmoothed)

                                if (distanceToTargetNm < autoAdvanceDistanceNm) {
                                    advanceToNextMark()
                                    return@launch
                                } else if (sogKnots > 0.05 && cogTrue != null) {
                                    val angleDiffCogToBearing = cogTrue - (smoothedBearing ?: bearingToTargetSmoothed)
                                    val normalizedAngleDiff = (angleDiffCogToBearing + 540.0) % 360.0 - 180.0

                                    historicalTurnAngles.add(normalizedAngleDiff)
                                    if (historicalTurnAngles.size > 2) {
                                        historicalTurnAngles.removeAt(0)
                                    }
                                    val smoothedTurnAngle = historicalTurnAngles.average()

                                    val vmgKnots = sogKnots * cos(Math.toRadians(smoothedTurnAngle))
                                    vmgToDisplay = String.format(Locale.US, "%.1f kn", vmgKnots)

                                    val turnMagnitude = abs(smoothedTurnAngle)
                                    val turnDirectionLabel = when {
                                        smoothedTurnAngle < -0.5 -> "L"
                                        smoothedTurnAngle > 0.5  -> "R"
                                        else -> ""
                                    }
                                    varianceToPinDisplay = if (turnDirectionLabel.isNotEmpty()) {
                                        String.format(Locale.US, "%.0f° %s", turnMagnitude, turnDirectionLabel)
                                    } else {
                                        String.format(Locale.US, "%.0f°", turnMagnitude)
                                    }
                                } else if (cogTrue == null && sogKnots > 0.05) {
                                    vmgToDisplay = "COG N/A"
                                    varianceToPinDisplay = "---"
                                } else if (sogKnots <= 0.05) {
                                    vmgToDisplay = "0.0 kn"
                                    varianceToPinDisplay = "---"
                                } else {
                                    vmgToDisplay = "VMG N/A"
                                    varianceToPinDisplay = "---"
                                }
                            } else { 
                                vmgToDisplay = "GPS N/A"
                                varianceToPinDisplay = "GPS N/A"
                            }
                        }
                    } else {
                        currentMarkNameToDisplay = "Mark Error"
                        println("InProgressRaceFragment: Target MarkEntity (for Data) not found for markId: ${currentCourseMarkEntity.markId}")
                    }
                } else {
                    currentMarkNameToDisplay = "Mark ID Error"
                    println("InProgressRaceFragment: Current CourseMarkEntity (main) not found for id: $currentCourseMarkId")
                }
            } else {
                currentMarkNameToDisplay = "No Target Mark"
                println("InProgressRaceFragment: RaceData.course_mark_id is 0. No target for Data.")
            }
            updateCurrentMarkDisplay(currentMarkNameToDisplay)
            updateNextMarkDisplay(nextMarkNameToDisplay)
            updateVmgToPinDisplay(vmgToDisplay)
            updateVarianceToPinDisplay(varianceToPinDisplay)
            updateBearingToPinDisplay(bearingToPinDisplay)
            updateCogDisplay(cogToDisplay)
            updateKnockLiftDisplay(knockLiftToDisplay)
            updateTargetBoatSpeedDisplay(targetBoatSpeedUpwindToDisplay)
            updateTargetSpeedOffwindDisplay(targetBoatSpeedOffwindToDisplay)
            updateCurrentSpeedDisplay(tidalSpeedToDisplay)
            updateCurrentDirectionDisplay(tidalDirectionToDisplay)
            updateLeewayDisplay(leewayToDisplay)
        }

        val currentSpeedKnots = GpsData.calculateSpeedInKnots()
        val boatSpeedToDisplay = String.format(Locale.US, "%.1f kn", currentSpeedKnots)
        updateBoatSpeedDisplay(boatSpeedToDisplay)
    }

    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.button_next_mark -> advanceToNextMark()
            R.id.button_prev_mark -> {
                lifecycleScope.launch {
                    val currentCourseMarkIdFromRaceData = RaceData.course_mark_id
                    if (currentCourseMarkIdFromRaceData != 0) {
                        val currentCourseMarkEntity = courseMarkDao.getCourseMarkById(currentCourseMarkIdFromRaceData)
                        if (currentCourseMarkEntity != null) {
                            val allCourseMarksForThisCourse = courseMarkDao.getSingleCourseMarks(currentCourseMarkEntity.courseId)
                            val sortedCourseMarksDescending = allCourseMarksForThisCourse.sortedByDescending { it.courseMarkSequence }
                            var prevMarkToMoveTo: CourseMarkEntity? = null
                            for (potentialPrevMark in sortedCourseMarksDescending) {
                                if (potentialPrevMark.courseMarkSequence < currentCourseMarkEntity.courseMarkSequence) {
                                    prevMarkToMoveTo = potentialPrevMark
                                    break
                                }
                            }
                            if (prevMarkToMoveTo != null) {
                                RaceData.course_mark_id = prevMarkToMoveTo.id
                                loadAndDisplayRaceData()
                                if(isAdded) Toast.makeText(context, "Moved to previous mark: ${markDao.getMarkById(prevMarkToMoveTo.markId)?.markName ?: "Unknown"}", Toast.LENGTH_SHORT).show()
                            } else {
                                if(isAdded) Toast.makeText(context, "Already at the first mark.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if(isAdded) Toast.makeText(context, "Error finding current mark details for Prev.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        if(isAdded) Toast.makeText(context, "No current mark set in RaceData for Prev.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
