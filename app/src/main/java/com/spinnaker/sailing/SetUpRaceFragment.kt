package com.spinnaker.sailing

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.spinnaker.sailing.AppDatabase
import com.spinnaker.sailing.data.RaceData
import com.spinnaker.sailing.location.LocationManager
import com.spinnaker.sailing.ui.boat.BoatDao
import com.spinnaker.sailing.ui.boat.BoatEntity
import com.spinnaker.sailing.ui.course.CourseDao
import com.spinnaker.sailing.ui.course.CourseEntity
import com.spinnaker.sailing.ui.race.RaceDao
import com.spinnaker.sailing.ui.race.RaceEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SetUpRaceFragment : Fragment(), View.OnClickListener {

    private lateinit var navController: NavController
    private lateinit var boatDao: BoatDao
    private lateinit var courseDao: CourseDao
    private lateinit var raceDao: RaceDao
    private lateinit var boatSpinner: Spinner
    private lateinit var courseSpinner: Spinner
    private lateinit var windDirectionEditText: EditText
    private lateinit var windStrengthEditText: EditText
    private lateinit var conditionsSpinner: Spinner
    private lateinit var raceDescriptionEditText: EditText
    private lateinit var raceDateEditText: EditText
    private lateinit var pinLatLongEditText: EditText
    private lateinit var boatLatLongEditText: EditText
    private lateinit var locationManager: LocationManager

    private val pinLocationListener: (Location) -> Unit = { location ->
        pinLatLongEditText.setText("${location.latitude}, ${location.longitude}")
        locationManager.removeLocationListener(pinLocationListener)
    }

    private val boatLocationListener: (Location) -> Unit = { location ->
        boatLatLongEditText.setText("${location.latitude}, ${location.longitude}")
        locationManager.removeLocationListener(boatLocationListener)
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Location permission is required for this feature.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val appDatabase = AppDatabase.getDatabase(requireContext())
        boatDao = appDatabase.BoatDao()
        courseDao = appDatabase.CourseDao()
        raceDao = appDatabase.RaceDao()
        locationManager = LocationManager.getInstance(requireContext())
        return inflater.inflate(R.layout.fragment_set_up_race, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()

        boatSpinner = view.findViewById(R.id.boat_name_spinner)
        courseSpinner = view.findViewById(R.id.course_name_spinner)
        windDirectionEditText = view.findViewById(R.id.editText_wind_direction)
        windStrengthEditText = view.findViewById(R.id.editText_wind_strength)
        conditionsSpinner = view.findViewById(R.id.conditions_spinner)
        raceDescriptionEditText = view.findViewById(R.id.editText_race_description)
        raceDateEditText = view.findViewById(R.id.editText_race_date)
        pinLatLongEditText = view.findViewById(R.id.editText_pin_lat_long)
        boatLatLongEditText = view.findViewById(R.id.editText_boat_lat_long)

        raceDescriptionEditText.filters = arrayOf(InputFilter.AllCaps())

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        raceDateEditText.setText(sdf.format(Date()))

        view.findViewById<Button>(R.id.button_accept_race).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_pin_ping).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_boat_ping).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)

        lifecycleScope.launch {
            boatDao.getAllBoats().collect { boats ->
                setupBoatSpinner(boats)
            }
        }
        lifecycleScope.launch {
            courseDao.getAllCourses().collect { courses ->
                setupCourseSpinner(courses)
            }
        }
        setupConditionsSpinner()
    }

    private fun setupBoatSpinner(boats: List<BoatEntity>) {
        val boatNames = boats.map { it.boatName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, boatNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        boatSpinner.adapter = adapter
        boatSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                RaceData.boat_id = boats[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCourseSpinner(courses: List<CourseEntity>) {
        val courseNames = courses.map { it.courseDesc }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, courseNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        courseSpinner.adapter = adapter
        courseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                RaceData.course_id = courses[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupConditionsSpinner() {
        val conditions = arrayOf("Smooth", "Waves", "Gusty")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, conditions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        conditionsSpinner.adapter = adapter
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.button_accept_race -> startRace()
            R.id.button_pin_ping -> pingLocation(pinLocationListener)
            R.id.button_boat_ping -> pingLocation(boatLocationListener)
            R.id.left_btn -> navController.navigate(R.id.mainFragment)
            R.id.right_btn -> navController.navigate(R.id.startRaceFragment)
            R.id.home_btn -> navController.navigate(R.id.mainFragment)
        }
    }

    private fun pingLocation(listener: (Location) -> Unit) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.addLocationListener(listener)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startRace() {
        val raceDescription = raceDescriptionEditText.text.toString()
        val raceDateStr = raceDateEditText.text.toString()
        val windDirection = windDirectionEditText.text.toString()
        val windStrength = windStrengthEditText.text.toString()
        val conditions = conditionsSpinner.selectedItem.toString()
        val pinLocation = pinLatLongEditText.text.toString().split(",").map { it.trim().toDoubleOrNull() }
        val boatLocation = boatLatLongEditText.text.toString().split(",").map { it.trim().toDoubleOrNull() }

        if (raceDescription.isBlank() || raceDateStr.isBlank() || windDirection.isBlank() || windStrength.isBlank() || pinLocation.size != 2 || pinLocation.any { it == null } || boatLocation.size != 2 || boatLocation.any { it == null }) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val raceDate = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raceDateStr)
        } catch (e: Exception) {
            Toast.makeText(context, "Invalid date format. Please use YYYY-MM-DD.", Toast.LENGTH_SHORT).show()
            return
        }

        val newRace = RaceEntity(
            raceDescription = raceDescription,
            boatName = boatSpinner.selectedItem.toString(),
            boatId = RaceData.boat_id,
            raceDate = raceDate,
            course = courseSpinner.selectedItem.toString(),
            courseId = RaceData.course_id,
            windDirection = windDirection,
            windStrength = windStrength,
            conditions = conditions,
            pinLatitude = pinLocation[0],
            pinLongitude = pinLocation[1],
            boatLatitude = boatLocation[0],
            boatLongitude = boatLocation[1],
            startTime = null,
            finishTime = null
        )

        lifecycleScope.launch {
            val raceId = raceDao.addRace(newRace)
            RaceData.race_id = raceId
            navController.navigate(R.id.startRaceFragment)
        }
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeLocationListener(pinLocationListener)
        locationManager.removeLocationListener(boatLocationListener)
    }
}
