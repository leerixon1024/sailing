package com.spinnaker.sailing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import android.view.GestureDetector
import android.view.MotionEvent
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.spinnaker.sailing.ui.boatperformance.BoatPerformanceDao
import com.spinnaker.sailing.ui.coursemark.CourseMarkDao
import com.spinnaker.sailing.ui.coursemark.CourseMarkWithMark
import com.spinnaker.sailing.ui.gpslog.GpsLogDao
import com.spinnaker.sailing.ui.gpslog.GpsLogEntity
import com.spinnaker.sailing.ui.race.RaceDao
import com.spinnaker.sailing.ui.race.RaceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

class AnalysisFragment : Fragment(), View.OnClickListener, OnMapReadyCallback, AdapterView.OnItemSelectedListener {

    private lateinit var navController: NavController
    private lateinit var gestureDetector: GestureDetector
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var timelineSeekBar: SeekBar
    private lateinit var dashboardSpeedTextView: TextView
    private lateinit var dashboardDirectionTextView: TextView
    private lateinit var dashboardWindTextView: TextView
    private lateinit var dashboardTimerTextView: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var speedSpinner: Spinner
    private lateinit var windArrowImageView: ImageView
    private lateinit var northArrowImageView: ImageView

    private lateinit var raceDao: RaceDao
    private lateinit var courseMarkDao: CourseMarkDao
    private lateinit var gpsLogDao: GpsLogDao
    private lateinit var boatPerformanceDao: BoatPerformanceDao

    private var raceGpsLog: List<GpsLogEntity> = emptyList()
    private var boatMarker: Marker? = null
    private val tackMarkers = mutableListOf<Marker>()

    private var isPlaying = false
    private var playbackSpeed = 1.0f
    private val playbackHandler = Handler(Looper.getMainLooper())
    private var playbackRunnable: Runnable? = null
    private val TACK_DETECTION_DISTANCE_METERS = 15 // meters

    private data class Tack(val location: LatLng, val time: Long, val angle: Double, val isUpwind: Boolean)
    private data class WindAnalysisResult(val windDirection: Double, val message: String)

    enum class WindSide { PORT, STARBOARD, UNKNOWN }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_analysis, container, false)

        mapView = view.findViewById(R.id.mapView)
        timelineSeekBar = view.findViewById(R.id.timeline_seekbar)
        dashboardSpeedTextView = view.findViewById(R.id.dashboard_speed_textview)
        dashboardDirectionTextView = view.findViewById(R.id.dashboard_direction_textview)
        dashboardWindTextView = view.findViewById(R.id.dashboard_wind_direction_textview)
        dashboardTimerTextView = view.findViewById(R.id.dashboard_timer_textview)
        playPauseButton = view.findViewById(R.id.play_pause_button)
        speedSpinner = view.findViewById(R.id.speed_spinner)
        windArrowImageView = view.findViewById(R.id.wind_arrow_imageview)
        northArrowImageView = view.findViewById(R.id.north_arrow_imageview)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        setupGestureDetector(view, mapView)

        return view
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        stopPlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        stopPlayback()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun setupGestureDetector(view: View, mapView: MapView) {
        val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
            private val swipeThreshold = 100
            private val swipeVelocityThreshold = 100

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
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

        view.setOnTouchListener { v, event ->
            val rect = Rect()
            mapView.getHitRect(rect)
            if (rect.contains(event.x.toInt(), event.y.toInt())) {
                false
            } else {
                val consumed = gestureDetector.onTouchEvent(event)
                if(consumed) v.performClick()
                consumed
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()

        val appDatabase = AppDatabase.getDatabase(requireContext())
        raceDao = appDatabase.RaceDao()
        courseMarkDao = appDatabase.CourseMarkDao()
        gpsLogDao = appDatabase.GpsLogDao()
        boatPerformanceDao = appDatabase.BoatPerformanceDao()

        dashboardSpeedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
        dashboardDirectionTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
        dashboardWindTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
        dashboardTimerTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
        northArrowImageView.rotation = 0f

        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)
        playPauseButton.setOnClickListener(this)

        val speedAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.playback_speeds,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            speedSpinner.adapter = adapter
        }
        speedSpinner.onItemSelectedListener = this


        timelineSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (raceGpsLog.isNotEmpty()) {
                    updateBoatStateForProgress(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                stopPlayback()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun handleLeftNavigation() {
        navController.popBackStack()
    }

    private fun handleRightNavigation() {
        navController.navigate(R.id.action_analysisFragment_to_setAnalysisParametersFragment)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.left_btn -> handleLeftNavigation()
            R.id.home_btn -> navController.navigate(R.id.mainFragment)
            R.id.play_pause_button -> togglePlayback()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.setOnMarkerClickListener { marker ->
            val tack = marker.tag as? Tack
            if (tack != null) {
                val title = if (tack.isUpwind) "Tack Details" else "Gybe Details"
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val time = timeFormat.format(Date(tack.time))
                AlertDialog.Builder(requireContext())
                    .setTitle(title)
                    .setMessage("""Time: $time
Angle: ${String.format(Locale.getDefault(), "%.1f", tack.angle)}°""")
                    .setPositiveButton("OK", null)
                    .show()
                true // Consume the event
            } else {
                false // Let the default behavior happen
            }
        }
        selectRace()
    }

    private fun selectRace() {
        lifecycleScope.launch {
            val races = raceDao.getAllRaces()
            if (races.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "No races found to analyze.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
                return@launch
            }
            val raceDescriptions = races.map { it.raceDescription }.toTypedArray()

            AlertDialog.Builder(requireContext())
                .setTitle("Select a Race to Analyze")
                .setItems(raceDescriptions) { _, which ->
                    lifecycleScope.launch {
                        drawRaceOnMap(races[which])
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    navController.popBackStack()
                }
                .show()
        }
    }

    private suspend fun drawRaceOnMap(race: RaceEntity) {
        // --- DATA FETCHING (BACKGROUND THREAD) ---
        raceGpsLog = gpsLogDao.getLogsForRace(race.id)
        if (raceGpsLog.size < 2) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Not enough GPS data for analysis.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val initialWindDirection = race.windDirection?.toDoubleOrNull()
        if (initialWindDirection == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Wind direction not available for this race.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val tacks = detectTacks(raceGpsLog, initialWindDirection)
        val windAnalysisResult = analyzeWindShifts(raceGpsLog, initialWindDirection, tacks)

        var courseMarks: List<CourseMarkWithMark> = emptyList()
        race.courseId?.let { courseId ->
            courseMarks = courseMarkDao.getCourseMarksWithMark(courseId)
        }

        // --- UI UPDATE (MAIN THREAD) ---
        withContext(Dispatchers.Main) {
            googleMap.clear()
            tackMarkers.clear()

            val boundsBuilder = LatLngBounds.builder()
            val trackPoints = raceGpsLog.map { LatLng(it.latitude, it.longitude) }
            trackPoints.forEach { boundsBuilder.include(it) }

            googleMap.addPolyline(PolylineOptions().addAll(trackPoints).color(Color.GRAY).width(5f))

            // Add Start/Finish Line Markers with valid coordinates
            race.pinLatitude?.let { lat ->
                race.pinLongitude?.let { lon ->
                    if (lat != 0.0 && lon != 0.0) {
                        val pinLatLng = LatLng(lat, lon)
                        googleMap.addMarker(MarkerOptions().position(pinLatLng).title("Start Line Pin").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
                        boundsBuilder.include(pinLatLng)
                    }
                }
            }
            race.boatLatitude?.let { lat ->
                race.boatLongitude?.let { lon ->
                    if (lat != 0.0 && lon != 0.0) {
                        val boatLatLng = LatLng(lat, lon)
                        googleMap.addMarker(MarkerOptions().position(boatLatLng).title("Start Line Boat").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
                        boundsBuilder.include(boatLatLng)
                    }
                }
            }

            // Add Course Mark Markers and include them in the bounds
            if (courseMarks.isNotEmpty()) {
                for (mark in courseMarks) {
                     if (mark.markLatitude != 0.0 && mark.markLongitude != 0.0) {
                        val markLatLng = LatLng(mark.markLatitude, mark.markLongitude)
                        googleMap.addMarker(MarkerOptions().position(markLatLng).title(mark.markName).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
                        boundsBuilder.include(markLatLng)
                    }
                }
            } else {
                Toast.makeText(requireContext(), "No course marks found for this race.", Toast.LENGTH_SHORT).show()
            }

            // Add Tack/Gybe Markers
            tacks.forEach { tack ->
                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .position(tack.location)
                        .title(if (tack.isUpwind) "Tack" else "Gybe")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                )
                marker?.tag = tack
                tackMarkers.add(marker!!)
            }

            // Add Boat Marker
            val startPosition = trackPoints.first()
            boatMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(startPosition)
                    .title("Boat")
                    .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.yellow_triangle))
                    .anchor(0.5f, 0.5f)
                    .flat(true)
            )

            // Update Dashboard
            windArrowImageView.rotation = (windAnalysisResult.windDirection.toFloat() + 180f) % 360f
            dashboardWindTextView.text = "Wind: ${windAnalysisResult.windDirection.roundToInt()}°"
            AlertDialog.Builder(requireContext())
                .setTitle("Wind Analysis")
                .setMessage(windAnalysisResult.message)
                .setPositiveButton("OK", null)
                .show()

            // Initialize Timeline and Playback
            timelineSeekBar.max = raceGpsLog.size - 1
            timelineSeekBar.progress = 0
            updateBoatStateForProgress(0)

            // Defer camera movement until the layout pass is complete.
            mapView.post {
                race.boatLatitude?.let { lat ->
                    race.boatLongitude?.let { lon ->
                        if (lat != 0.0 && lon != 0.0) {
                            val boatLatLng = LatLng(lat, lon)
                            // Calculate the zoom level for a 50 yard view
                            val zoomLevel = 19f // Approximate zoom level for a 50 yard view
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(boatLatLng, zoomLevel))
                        }
                    }
                }
            }
        }
    }

    private fun updateBoatStateForProgress(progress: Int) {
        if (raceGpsLog.isEmpty() || progress < 0 || progress >= raceGpsLog.size) return

        val currentPoint = raceGpsLog[progress]
        val newPosition = LatLng(currentPoint.latitude, currentPoint.longitude)
        boatMarker?.position = newPosition

        val elapsedTime = (currentPoint.timestamp - raceGpsLog.first().timestamp) / 1000
        val minutes = elapsedTime / 60
        val seconds = elapsedTime % 60
        dashboardTimerTextView.text = String.format(Locale.getDefault(), "Time: %02d:%02d", minutes, seconds)

        if (progress > 0) {
            val previousPoint = raceGpsLog[progress - 1]

            val distance = FloatArray(1)
            Location.distanceBetween(
                previousPoint.latitude,
                previousPoint.longitude,
                currentPoint.latitude,
                currentPoint.longitude,
                distance
            )

            val timeDifferenceSeconds = (currentPoint.timestamp - previousPoint.timestamp) / 1000.0
            val speedKnots = if (timeDifferenceSeconds > 0) {
                val speedMetersPerSecond = distance[0] / timeDifferenceSeconds
                speedMetersPerSecond * 1.94384
            } else {
                0.0
            }

            val bearing = calculateBearing(previousPoint, currentPoint)
            boatMarker?.rotation = bearing.toFloat()

            dashboardSpeedTextView.text = "Speed: ${String.format(Locale.getDefault(), "%.1f", speedKnots)} knots"
            dashboardDirectionTextView.text = "Direction: ${String.format(Locale.getDefault(), "%.0f", bearing)}°"
        } else {
            dashboardSpeedTextView.text = "Speed: --"
            dashboardDirectionTextView.text = "Direction: --"
        }
    }

    private fun togglePlayback() {
        if (isPlaying) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        if (raceGpsLog.size < 2) return
        isPlaying = true
        playPauseButton.setImageResource(android.R.drawable.ic_media_pause)

        playbackRunnable = object : Runnable {
            override fun run() {
                var currentProgress = timelineSeekBar.progress
                if (currentProgress < timelineSeekBar.max) {
                    currentProgress++
                    timelineSeekBar.progress = currentProgress

                    if (currentProgress > 0) {
                        val timeDiff = (raceGpsLog[currentProgress].timestamp - raceGpsLog[currentProgress - 1].timestamp).toLong()
                        playbackHandler.postDelayed(this, (timeDiff / playbackSpeed).toLong())
                    }
                } else {
                    stopPlayback()
                }
            }
        }
        playbackHandler.post(playbackRunnable!!)
    }

    private fun stopPlayback() {
        isPlaying = false
        playPauseButton.setImageResource(android.R.drawable.ic_media_play)
        playbackRunnable?.let { playbackHandler.removeCallbacks(it) }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val selectedSpeed = parent?.getItemAtPosition(position).toString()
        val newSpeed = when (selectedSpeed) {
            "2x" -> 2.0f
            "4x" -> 4.0f
            "8x" -> 8.0f
            else -> 1.0f
        }
        if (newSpeed != playbackSpeed) {
            playbackSpeed = newSpeed
            if (isPlaying) {
                stopPlayback()
                startPlayback()
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    private fun getWindSide(boatHeading: Double, windDirection: Double): WindSide {
        val diff = (boatHeading - windDirection + 360) % 360
        return when {
            diff > 1.0 && diff < 180 -> WindSide.STARBOARD
            diff > 180 && diff < 359 -> WindSide.PORT
            else -> WindSide.UNKNOWN
        }
    }

    private suspend fun detectTacks(gpsLog: List<GpsLogEntity>, windDirection: Double): List<Tack> {
        val candidates = mutableListOf<Tack>()
        if (gpsLog.size < 2) return candidates

        val race = raceDao.getRaceById(gpsLog.first().raceId.toString())
        val boatId = race?.boatId
        val windStrength = race?.windStrength?.toDoubleOrNull() ?: 0.0

        val tackAngle = if (boatId != null) {
            (boatPerformanceDao.getSpecificBoatPerformance(boatId)
                .minByOrNull { abs(it.trueWind.toDouble() - windStrength) }?.tackAngle?.toDouble() ?: 45.0) * 2.0
        } else {
            90.0
        }

        var i = 1
        while (i < gpsLog.size -1) {
            val prevPoint = gpsLog[i-1]
            val currentPoint = gpsLog[i]

            val avgBearingBefore = calculateAverageBearing(findSegment(gpsLog, i, -TACK_DETECTION_DISTANCE_METERS))
            val avgBearingAfter = calculateAverageBearing(findSegment(gpsLog, i, TACK_DETECTION_DISTANCE_METERS))

            if (avgBearingBefore != null && avgBearingAfter != null) {
                val windSideBefore = getWindSide(avgBearingBefore, windDirection)
                val windSideAfter = getWindSide(avgBearingAfter, windDirection)

                if (windSideBefore != WindSide.UNKNOWN && windSideAfter != WindSide.UNKNOWN && windSideBefore != windSideAfter) {
                    val angle = abs(avgBearingAfter - avgBearingBefore)
                    val normalizedAngle = if (angle > 180) 360 - angle else angle

                    val pointOfSail = abs((avgBearingBefore - windDirection + 360) % 360)
                    val isUpwind = pointOfSail < 90 || pointOfSail > 270

                    val tackAngleThreshold = if (isUpwind) tackAngle * 0.75 else 15.0

                    if (normalizedAngle > tackAngleThreshold) {
                        candidates.add(Tack(LatLng(currentPoint.latitude, currentPoint.longitude), currentPoint.timestamp, normalizedAngle, isUpwind))
                        // Skip points to avoid multiple detections for the same tack
                        var skipIndex = i + 1
                        while(skipIndex < gpsLog.size) {
                            val dist = FloatArray(1)
                            Location.distanceBetween(currentPoint.latitude, currentPoint.longitude, gpsLog[skipIndex].latitude, gpsLog[skipIndex].longitude, dist)
                            if(dist[0] < TACK_DETECTION_DISTANCE_METERS) {
                                skipIndex++
                            } else {
                                break
                            }
                        }
                        i = skipIndex
                        continue
                    }
                }
            }
            i++
        }

        return candidates
    }

    private fun findSegment(log: List<GpsLogEntity>, startIndex: Int, distance: Int): List<GpsLogEntity> {
        val segment = mutableListOf<GpsLogEntity>()
        if (startIndex !in log.indices) return segment

        var accumulatedDistance = 0f
        val step = if (distance > 0) 1 else -1
        var currentIndex = startIndex

        // Add the starting point.
        segment.add(log[currentIndex])

        while (accumulatedDistance < abs(distance)) {
            val nextIndex = currentIndex + step
            if (nextIndex !in log.indices) break

            val dist = FloatArray(1)
            Location.distanceBetween(
                log[currentIndex].latitude, log[currentIndex].longitude,
                log[nextIndex].latitude, log[nextIndex].longitude, dist
            )

            accumulatedDistance += dist[0]
            currentIndex = nextIndex
            segment.add(log[currentIndex])
        }
        if (step == -1) {
            segment.reverse()
        }
        return segment
    }


    private suspend fun analyzeWindShifts(gpsLog: List<GpsLogEntity>, initialWindDirection: Double, tacks: List<Tack>): WindAnalysisResult {
        if (tacks.isEmpty()) {
            return WindAnalysisResult(initialWindDirection, "No tacks detected to infer wind shift.")
        }

        val segments = mutableListOf<List<GpsLogEntity>>()
        var lastTackIndex = 0

        for (tack in tacks) {
            val tackIndex = gpsLog.indexOfFirst { it.timestamp == tack.time }
            if (tackIndex != -1) {
                segments.add(gpsLog.subList(lastTackIndex, tackIndex))
                lastTackIndex = tackIndex
            }
        }
        segments.add(gpsLog.subList(lastTackIndex, gpsLog.size))

        val portHeadings = mutableListOf<Double>()
        val starboardHeadings = mutableListOf<Double>()

        for (segment in segments) {
            if (segment.size < 2) continue

            val avgBearing = calculateAverageBearing(segment)
            if (avgBearing != null) {
                val pointOfSail = abs((avgBearing - initialWindDirection + 360) % 360)
                val isUpwind = pointOfSail < 90 || pointOfSail > 270

                if (isUpwind) {
                    when (getWindSide(avgBearing, initialWindDirection)) {
                        WindSide.PORT -> portHeadings.add(avgBearing)
                        WindSide.STARBOARD -> starboardHeadings.add(avgBearing)
                        else -> {}
                    }
                }
            }
        }

        val avgPortHeading = if (portHeadings.isNotEmpty()) calculateCircularAverage(portHeadings) else null
        val avgStarboardHeading = if (starboardHeadings.isNotEmpty()) calculateCircularAverage(starboardHeadings) else null

        if (avgPortHeading != null && avgStarboardHeading != null) {
            // The wind direction is the average of the two upwind headings.
            val inferredWindDirection = calculateCircularAverage(listOf(avgPortHeading, avgStarboardHeading))
            val windShift = inferredWindDirection - initialWindDirection
            val normalizedWindShift = (windShift + 540) % 360 - 180

            val shiftDirection = if (normalizedWindShift > 0) "Lift" else "Knock"

            val message = """Initial Wind: ${initialWindDirection.toInt()}°
Inferred Wind: ${inferredWindDirection.roundToInt()}°
Shift: ${abs(normalizedWindShift).roundToInt()}° $shiftDirection"""
            return WindAnalysisResult(inferredWindDirection, message)
        } else {
            val message = "No wind shift detected. Not enough tacks on both sides to infer a shift."
            return WindAnalysisResult(initialWindDirection, message)
        }
    }

    private fun calculateCircularAverage(bearings: List<Double>): Double {
        var sumX = 0.0
        var sumY = 0.0
        for (bearing in bearings) {
            val bearingRad = Math.toRadians(bearing)
            sumX += cos(bearingRad)
            sumY += sin(bearingRad)
        }
        val avgBearingRad = atan2(sumY, sumX)
        var avgBearing = Math.toDegrees(avgBearingRad)
        avgBearing = (avgBearing + 360) % 360
        return avgBearing
    }

    private fun calculateAverageBearing(points: List<GpsLogEntity>): Double? {
        if (points.size < 2) return null

        val bearings = mutableListOf<Double>()
        for (i in 0 until points.size - 1) {
            bearings.add(calculateBearing(points[i], points[i+1]))
        }

        return if(bearings.isNotEmpty()) calculateCircularAverage(bearings) else null

    }


    private fun calculateBearing(p1: GpsLogEntity, p2: GpsLogEntity): Double {
        val lat1 = Math.toRadians(p1.latitude)
        val lon1 = Math.toRadians(p1.longitude)
        val lat2 = Math.toRadians(p2.latitude)
        val lon2 = Math.toRadians(p2.longitude)

        val dLon = lon2 - lon1

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360
        return bearing
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
