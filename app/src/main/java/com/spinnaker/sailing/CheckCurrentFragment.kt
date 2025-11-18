package com.spinnaker.sailing

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
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
import androidx.navigation.Navigation
import com.spinnaker.sailing.data.RaceData
import com.spinnaker.sailing.location.LocationManager
import com.spinnaker.sailing.ui.boatperformance.BoatPerformanceDao
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import com.spinnaker.sailing.genclasses.GeoCalculator

data class CurrentResult(val speed: Double, val direction: Double)

class CheckCurrentFragment : Fragment(), View.OnClickListener {

    lateinit var navController: NavController
    private lateinit var timerValue: TextView
    private lateinit var northResult: TextView
    private lateinit var eastResult: TextView
    private lateinit var southResult: TextView
    private lateinit var westResult: TextView
    private lateinit var averageCurrentResult: TextView
    private lateinit var northButton: Button
    private lateinit var eastButton: Button
    private lateinit var southButton: Button
    private lateinit var westButton: Button
    private lateinit var boatPerformanceDao: BoatPerformanceDao

    private var countDownTimer: CountDownTimer? = null
    private var currentTestDirection: String? = null
    private val currentResults = mutableMapOf<String, CurrentResult>()
    private lateinit var locationManager: LocationManager

    private var startLocation: Location? = null
    private var endLocation: Location? = null

    private val locationListener: (Location) -> Unit = { location ->
        if (startLocation == null) {
            startLocation = location
        }
        endLocation = location
    }

    private val requestPermissionLauncher = 
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startTestWithPermissionCheck(currentTestDirection, 0.0)
            } else {
                Toast.makeText(requireContext(), "Location permission denied.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        locationManager = LocationManager.getInstance(requireContext())
        return inflater.inflate(R.layout.fragment_check_current, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        timerValue = view.findViewById(R.id.textView_timer_value)
        northResult = view.findViewById(R.id.textView_north_result)
        eastResult = view.findViewById(R.id.textView_east_result)
        southResult = view.findViewById(R.id.textView_south_result)
        westResult = view.findViewById(R.id.textView_west_result)
        averageCurrentResult = view.findViewById(R.id.textView_average_current_result)

        northButton = view.findViewById(R.id.button_start_north)
        eastButton = view.findViewById(R.id.button_start_east)
        southButton = view.findViewById(R.id.button_start_south)
        westButton = view.findViewById(R.id.button_start_west)

        boatPerformanceDao = AppDatabase.getDatabase(requireContext()).BoatPerformanceDao()

        northButton.setOnClickListener(this)
        eastButton.setOnClickListener(this)
        southButton.setOnClickListener(this)
        westButton.setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.button_start_north -> startTestWithPermissionCheck("North", 0.0)
            R.id.button_start_east -> startTestWithPermissionCheck("East", 90.0)
            R.id.button_start_south -> startTestWithPermissionCheck("South", 180.0)
            R.id.button_start_west -> startTestWithPermissionCheck("West", 270.0)
            R.id.left_btn -> navController.popBackStack()
            R.id.right_btn -> navController.navigate(R.id.action_checkCurrentFragment_to_startRaceFragment)
            R.id.home_btn -> navController.navigate(R.id.mainFragment)
        }
    }

    private fun setButtonsEnabled(isEnabled: Boolean) {
        northButton.isEnabled = isEnabled
        eastButton.isEnabled = isEnabled
        southButton.isEnabled = isEnabled
        westButton.isEnabled = isEnabled
    }

    private fun startTestWithPermissionCheck(direction: String?, heading: Double) {
        if (direction == null) return
        currentTestDirection = direction
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startTest(direction, heading)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startTest(direction: String, heading: Double) {
        startLocation = null
        endLocation = null
        locationManager.addLocationListener(locationListener)
        countDownTimer?.cancel()
        setButtonsEnabled(false)

        countDownTimer = object : CountDownTimer(20000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                timerValue.text = String.format(Locale.US, "00:%02d", seconds)
            }

            override fun onFinish() {
                timerValue.text = "00:00"
                locationManager.removeLocationListener(locationListener)
                lifecycleScope.launch{
                    calculateAndDisplayResult(heading)
                }
                setButtonsEnabled(true)
                updateButtonState(direction)
                if (currentResults.size == 4) {
                    calculateAndDisplayAverageCurrent()
                }
            }
        }.start()
    }

    private fun updateButtonState(direction: String) {
        val button = when (direction) {
            "North" -> northButton
            "East" -> eastButton
            "South" -> southButton
            "West" -> westButton
            else -> null
        }
        button?.setBackgroundColor(Color.GREEN)
    }

    private suspend fun calculateAndDisplayResult(heading: Double) {
        if (startLocation == null || endLocation == null || startLocation!!.time == endLocation!!.time) {
            displayGpsError()
            return
        }

        val elapsedTimeSeconds = (endLocation!!.time - startLocation!!.time) / 1000.0
        val startCoords = GeoCalculator.Coordinates(startLocation!!.latitude, startLocation!!.longitude)
        val endCoords = GeoCalculator.Coordinates(endLocation!!.latitude, endLocation!!.longitude)

        val distanceNm = GeoCalculator.calculateDistanceNm(startCoords, endCoords)
        val averageSog = if (elapsedTimeSeconds > 0) distanceNm / (elapsedTimeSeconds / 3600.0) else 0.0
        val averageCog = GeoCalculator().calculate(startCoords, endCoords).bearing

        val boatPerformanceDao = AppDatabase.getDatabase(requireContext()).BoatPerformanceDao()
        val performanceData = boatPerformanceDao.getSpecificBoatPerformance(RaceData.boat_id)
        val closestPerformance = performanceData.minByOrNull { abs(it.trueWind - RaceData.wind_strength) }
        val leeway = closestPerformance?.leeway ?: 5.0

        val angle_between_wind_and_boat = abs(RaceData.wind_direction - heading)
        val leeway_effect = leeway * sin(Math.toRadians(angle_between_wind_and_boat))

        val assumedSpeed = 5.0 // Knots
        val boatVelX = assumedSpeed * sin(Math.toRadians(heading + leeway_effect))
        val boatVelY = assumedSpeed * cos(Math.toRadians(heading + leeway_effect))

        val gpsVelX = averageSog * sin(Math.toRadians(averageCog))
        val gpsVelY = averageSog * cos(Math.toRadians(averageCog))

        val currentVelX = gpsVelX - boatVelX
        val currentVelY = gpsVelY - boatVelY

        val currentSpeed = sqrt(currentVelX * currentVelX + currentVelY * currentVelY)
        var currentDirection = Math.toDegrees(atan2(currentVelX, currentVelY))
        currentDirection = (currentDirection + 360) % 360

        currentResults[currentTestDirection!!] = CurrentResult(currentSpeed, currentDirection)

        val resultText = String.format(Locale.US, "%.1f kn @ %.0f°", currentSpeed, currentDirection)
        displayResult(resultText)
    }

    private fun displayGpsError() {
        val errorText = "GPS Error"
        when (currentTestDirection) {
            "North" -> northResult.text = "North Result: $errorText"
            "East" -> eastResult.text = "East Result: $errorText"
            "South" -> southResult.text = "South Result: $errorText"
            "West" -> westResult.text = "West Result: $errorText"
        }
    }

    private fun displayResult(resultText: String) {
        when (currentTestDirection) {
            "North" -> northResult.text = "North Result: $resultText"
            "East" -> eastResult.text = "East Result: $resultText"
            "South" -> southResult.text = "South Result: $resultText"
            "West" -> westResult.text = "West Result: $resultText"
        }
    }

    private fun calculateAndDisplayAverageCurrent() {
        var sumX = 0.0
        var sumY = 0.0

        currentResults.values.forEach {
            sumX += it.speed * sin(Math.toRadians(it.direction))
            sumY += it.speed * cos(Math.toRadians(it.direction))
        }

        val avgX = sumX / 4
        val avgY = sumY / 4

        val avgSpeed = sqrt(avgX * avgX + avgY * avgY)
        var avgDirection = Math.toDegrees(atan2(avgX, avgY))
        avgDirection = (avgDirection + 360) % 360

        RaceData.current_speed = avgSpeed.toFloat()
        RaceData.current_direction = avgDirection.toInt()

        averageCurrentResult.text = String.format(Locale.US, "Current: %.1f kn @ %.0f°", avgSpeed, avgDirection)
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeLocationListener(locationListener)
        countDownTimer?.cancel()
    }
}
