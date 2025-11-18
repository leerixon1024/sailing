package com.spinnaker.sailing

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
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
import com.spinnaker.sailing.AppDatabase
import com.spinnaker.sailing.location.LocationManager
import com.spinnaker.sailing.ui.mark.MarkDao
import com.spinnaker.sailing.ui.mark.MarkEntity
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

class MaintainIndividualMarkFragment : Fragment(), View.OnClickListener {

    private lateinit var navController: NavController
    private lateinit var markNameEditText: EditText
    private lateinit var latDegreesEditText: EditText
    private lateinit var latDecimalMinutesEditText: EditText
    private lateinit var latHemisphereEditText: EditText
    private lateinit var lonDegreesEditText: EditText
    private lateinit var lonDecimalMinutesEditText: EditText
    private lateinit var lonHemisphereEditText: EditText
    private lateinit var titleTextView: TextView
    private lateinit var markDao: MarkDao
    private var currentMark: MarkEntity? = null
    private lateinit var locationManager: LocationManager

    private val locationListener: (Location) -> Unit = { location ->
        if (isAdded) {
            val (latDegrees, latMinutes) = decimalToDdm(location.latitude)
            val (lonDegrees, lonMinutes) = decimalToDdm(location.longitude)

            latDegreesEditText.setText(latDegrees.toString())
            latDecimalMinutesEditText.setText(String.format(Locale.US, "%.3f", latMinutes))
            latHemisphereEditText.setText(if (location.latitude >= 0) "N" else "S")

            lonDegreesEditText.setText(lonDegrees.toString())
            lonDecimalMinutesEditText.setText(String.format(Locale.US, "%.3f", lonMinutes))
            lonHemisphereEditText.setText(if (location.longitude >= 0) "E" else "W")
        }
        locationManager.removeLocationListener(this.locationListener)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            locationManager.addLocationListener(locationListener)
        } else {
            Toast.makeText(requireContext(), "Location permission denied.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maintain_individual_mark, container, false)
        markDao = AppDatabase.getDatabase(requireContext()).MarkDao()
        locationManager = LocationManager.getInstance(requireContext())
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        markNameEditText = view.findViewById(R.id.textViewMarkName)
        latDegreesEditText = view.findViewById(R.id.editTextLatDegrees)
        latDecimalMinutesEditText = view.findViewById(R.id.editTextLatDecimalMinutes)
        latHemisphereEditText = view.findViewById(R.id.editTextLatHemisphere)
        lonDegreesEditText = view.findViewById(R.id.editTextLonDegrees)
        lonDecimalMinutesEditText = view.findViewById(R.id.editTextLonDecimalMinutes)
        lonHemisphereEditText = view.findViewById(R.id.editTextLonHemisphere)
        titleTextView = view.findViewById(R.id.textView_course_maintenance_title)

        view.findViewById<Button>(R.id.buttonUpdateMark).setOnClickListener(this)
        view.findViewById<Button>(R.id.buttonDeleteMark).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_add_new).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_ping_location).setOnClickListener(this)

        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.right_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)

        val markId = arguments?.getInt("markId", 0) ?: 0
        if (markId != 0) {
            loadMark(markId)
        } else {
            titleTextView.text = "New Mark"
            view.findViewById<Button>(R.id.buttonDeleteMark).isEnabled = false
        }
    }

    private fun loadMark(markId: Int) {
        lifecycleScope.launch {
            currentMark = markDao.getMarkById(markId)
            currentMark?.let {
                titleTextView.text = "Editing: ${it.markName}"
                markNameEditText.setText(it.markName)
                val (latDegrees, latMinutes) = decimalToDdm(it.markLatitude)
                latDegreesEditText.setText(latDegrees.toString())
                latDecimalMinutesEditText.setText(String.format(Locale.US, "%.3f", latMinutes))
                latHemisphereEditText.setText(if (it.markLatitude >= 0) "N" else "S")

                val (lonDegrees, lonMinutes) = decimalToDdm(it.markLongitude)
                lonDegreesEditText.setText(lonDegrees.toString())
                lonDecimalMinutesEditText.setText(String.format(Locale.US, "%.3f", lonMinutes))
                lonHemisphereEditText.setText(if (it.markLongitude >= 0) "E" else "W")
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.buttonUpdateMark -> saveMark()
            R.id.buttonDeleteMark -> deleteMark()
            R.id.button_add_new -> clearForm()
            R.id.button_ping_location -> getCurrentLocation()
            R.id.left_btn, R.id.right_btn -> navController.navigate(R.id.maintMarkFragment)
            R.id.home_btn -> navController.navigate(R.id.mainFragment)
        }
    }

    private fun saveMark() {
        val name = markNameEditText.text.toString()
        val lat = ddmToDecimal(
            latDegreesEditText.text.toString(),
            latDecimalMinutesEditText.text.toString(),
            latHemisphereEditText.text.toString()
        )
        val lon = ddmToDecimal(
            lonDegreesEditText.text.toString(),
            lonDecimalMinutesEditText.text.toString(),
            lonHemisphereEditText.text.toString()
        )

        if (name.isBlank() || lat == null || lon == null) {
            Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val markToSave = currentMark?.copy(markName = name, markLatitude = lat, markLongitude = lon) 
                ?: MarkEntity(markName = name, markLatitude = lat, markLongitude = lon)

            if (currentMark == null) {
                markDao.addMark(markToSave)
                Toast.makeText(context, "Mark saved successfully", Toast.LENGTH_SHORT).show()
            } else {
                markDao.updateMark(markToSave)
                Toast.makeText(context, "Mark updated successfully", Toast.LENGTH_SHORT).show()
            }
            navController.navigateUp()
        }
    }

    private fun deleteMark() {
        currentMark?.let {
            lifecycleScope.launch {
                markDao.deleteMark(it)
                Toast.makeText(context, "Mark deleted successfully", Toast.LENGTH_SHORT).show()
                navController.navigateUp()
            }
        }
    }

    private fun clearForm() {
        currentMark = null
        titleTextView.text = "New Mark"
        markNameEditText.text.clear()
        latDegreesEditText.text.clear()
        latDecimalMinutesEditText.text.clear()
        latHemisphereEditText.text.clear()
        lonDegreesEditText.text.clear()
        lonDecimalMinutesEditText.text.clear()
        lonHemisphereEditText.text.clear()
        view?.findViewById<Button>(R.id.buttonDeleteMark)?.isEnabled = false
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.addLocationListener(locationListener)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun decimalToDdm(decimal: Double): Pair<Int, Double> {
        val degrees = decimal.toInt()
        val minutes = (abs(decimal) - abs(degrees)) * 60
        return Pair(degrees, minutes)
    }

    private fun ddmToDecimal(degreesStr: String, minutesStr: String, hemisphere: String): Double? {
        val degrees = degreesStr.toDoubleOrNull() ?: return null
        val minutes = minutesStr.toDoubleOrNull() ?: return null
        var decimal = abs(degrees) + minutes / 60
        if (hemisphere.equals("S", ignoreCase = true) || hemisphere.equals("W", ignoreCase = true)) {
            decimal *= -1
        }
        return decimal
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeLocationListener(locationListener)
    }
}
