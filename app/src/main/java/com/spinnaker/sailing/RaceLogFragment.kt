package com.spinnaker.sailing

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.gson.Gson
import com.spinnaker.sailing.ui.coursemark.CourseMarkDao
import com.spinnaker.sailing.ui.gpslog.GpsLogDao
import com.spinnaker.sailing.ui.gpslog.GpsLogEntity
import com.spinnaker.sailing.ui.mark.MarkDao
import com.spinnaker.sailing.ui.mark.MarkEntity
import com.spinnaker.sailing.ui.race.RaceDao
import com.spinnaker.sailing.ui.race.RaceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RaceLogFragment : Fragment(), View.OnClickListener {

    lateinit var navController: NavController
    private lateinit var raceDao: RaceDao
    private lateinit var gpsLogDao: GpsLogDao
    private lateinit var courseMarkDao: CourseMarkDao
    private lateinit var markDao: MarkDao
    private var selectedRaceIdForBackup: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_race_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        val appDatabase = AppDatabase.getDatabase(context ?: return)
        raceDao = appDatabase.RaceDao()
        gpsLogDao = appDatabase.GpsLogDao()
        courseMarkDao = appDatabase.CourseMarkDao()
        markDao = appDatabase.MarkDao()

        view.findViewById<Button>(R.id.button_backup_race).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_restore_race).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_export_gpx).setOnClickListener(this)
        view.findViewById<Button>(R.id.button_clear_gps_data).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.left_btn).setOnClickListener(this)
        view.findViewById<ImageButton>(R.id.home_btn).setOnClickListener(this)
    }

    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        val safeContext = context ?: return@registerForActivityResult
        if (uri != null) {
            selectedRaceIdForBackup?.let {
                lifecycleScope.launch {
                    writeRaceToUri(uri, it)
                }
            }
        } else {
            Toast.makeText(safeContext, "No file selected for backup.", Toast.LENGTH_SHORT).show()
        }
    }

    private val saveGpxLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/gpx+xml")) { uri: Uri? ->
        val safeContext = context ?: return@registerForActivityResult
        if (uri != null) {
            selectedRaceIdForBackup?.let {
                lifecycleScope.launch {
                    writeGpxToUri(uri, it)
                }
            }
        } else {
            Toast.makeText(safeContext, "No file selected for GPX export.", Toast.LENGTH_SHORT).show()
        }
    }

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val safeContext = context ?: return@registerForActivityResult
        if (uri != null) {
            lifecycleScope.launch {
                readRaceFromUri(uri)
            }
        } else {
            Toast.makeText(safeContext, "No file selected.", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun writeGpxToUri(uri: Uri, raceId: Long) {
        val safeContext = context ?: return
        val race = raceDao.getRaceById(raceId.toString()) ?: return
        val gpsLogs = gpsLogDao.getLogsForRace(raceId)

        val gpxBuilder = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

        gpxBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        gpxBuilder.append("<gpx version=\"1.1\" creator=\"SailingApp\">\n")
        gpxBuilder.append("  <trk>\n")
        gpxBuilder.append("    <name>${race.raceDescription}</name>\n")
        gpxBuilder.append("    <trkseg>\n")

        for (log in gpsLogs) {
            gpxBuilder.append("      <trkpt lat=\"${log.latitude}\" lon=\"${log.longitude}\">\n")
            gpxBuilder.append("        <time>${dateFormat.format(Date(log.timestamp))}</time>\n")
            gpxBuilder.append("      </trkpt>\n")
        }

        gpxBuilder.append("    </trkseg>\n")
        gpxBuilder.append("  </trk>\n")
        gpxBuilder.append("</gpx>")

        try {
            withContext(Dispatchers.IO) {
                safeContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).use {
                        it.write(gpxBuilder.toString().toByteArray())
                    }
                }
            }
            Toast.makeText(safeContext, "GPX file exported successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(safeContext, "Failed to export GPX: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun writeRaceToUri(uri: Uri, raceId: Long) {
        val safeContext = context ?: return
        val race = raceDao.getRaceById(raceId.toString())
        if (race == null) {
            Toast.makeText(safeContext, "Race not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val gpsLogs = gpsLogDao.getLogsForRace(raceId)
        val courseMarks = race.courseId?.let { courseMarkDao.getSingleCourseMarks(it) } ?: emptyList()

        val features = mutableListOf<Feature>()

        val coordinates = gpsLogs.map { listOf(it.longitude, it.latitude) }
        val timestamps = gpsLogs.map { it.timestamp }
        val accuracies = gpsLogs.map { it.accuracy }
        val geometry = Geometry("LineString", coordinates)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val properties = Properties(
            raceDescription = race.raceDescription,
            boatName = race.boatName,
            boatId = race.boatId,
            raceDate = race.raceDate?.let { dateFormat.format(it) },
            windDirection = race.windDirection,
            windStrength = race.windStrength,
            course = race.course,
            courseId = race.courseId,
            pinLatitude = race.pinLatitude,
            pinLongitude = race.pinLongitude,
            boatLatitude = race.boatLatitude,
            boatLongitude = race.boatLongitude,
            startTime = race.startTime,
            finishTime = race.finishTime,
            timestamps = timestamps,
            accuracies = accuracies
        )
        features.add(Feature(properties = properties, geometry = geometry))

        for (courseMark in courseMarks) {
            markDao.getMarkById(courseMark.markId)?.let { mark ->
                val markCoordinates = listOf(mark.markLongitude, mark.markLatitude)
                val markGeometry = Geometry("Point", markCoordinates)
                val markProperties = Properties(
                    markName = mark.markName,
                    markSequence = courseMark.courseMarkSequence ?: 0
                )
                features.add(Feature(properties = markProperties, geometry = markGeometry))
            }
        }

        val geoJson = GeoJson(features = features)
        val gson = Gson()
        val json = gson.toJson(geoJson)

        try {
            withContext(Dispatchers.IO) {
                safeContext.contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).use {
                        it.write(json.toByteArray())
                    }
                }
            }
            Toast.makeText(safeContext, "Race log backed up successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(safeContext, "Failed to backup race log: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun readRaceFromUri(uri: Uri) {
        val safeContext = context ?: return
        val gson = Gson()
        try {
            val jsonString = withContext(Dispatchers.IO) {
                safeContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
            if (jsonString.isNullOrEmpty()) {
                Toast.makeText(safeContext, "Backup file is empty or could not be read.", Toast.LENGTH_LONG).show()
                return
            }

            val geoJson: GeoJson = gson.fromJson(jsonString, GeoJson::class.java)
            var gpsPointsRestored = 0

            geoJson.features.find { it.geometry.type == "LineString" }?.let { raceFeature ->
                val raceProperties = raceFeature.properties
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val raceDate: Date? = raceProperties.raceDate?.let { dateFormat.parse(it) }

                val newRace = RaceEntity(
                    raceDescription = raceProperties.raceDescription ?: "Restored Race",
                    boatName = raceProperties.boatName ?: "Restored Boat",
                    boatId = raceProperties.boatId,
                    raceDate = raceDate,
                    windDirection = raceProperties.windDirection,
                    windStrength = raceProperties.windStrength,
                    course = raceProperties.course,
                    courseId = raceProperties.courseId,
                    pinLatitude = raceProperties.pinLatitude,
                    pinLongitude = raceProperties.pinLongitude,
                    boatLatitude = raceProperties.boatLatitude,
                    boatLongitude = raceProperties.boatLongitude,
                    startTime = raceProperties.startTime,
                    finishTime = raceProperties.finishTime
                )
                val newRaceId = raceDao.addRace(newRace)

                @Suppress("UNCHECKED_CAST")
                val coordinates = raceFeature.geometry.coordinates as? List<List<Double>>
                val timestamps = raceFeature.properties.timestamps
                val accuracies = raceFeature.properties.accuracies

                coordinates?.forEachIndexed { index, coords ->
                    val timestamp = timestamps?.getOrNull(index) ?: System.currentTimeMillis()
                    val accuracy = accuracies?.getOrNull(index) ?: 0.0f
                    val log = GpsLogEntity(
                        raceId = newRaceId,
                        timestamp = timestamp,
                        longitude = coords[0],
                        latitude = coords[1],
                        bearing = 0.0,
                        accuracy = accuracy
                    )
                    gpsLogDao.insert(log)
                    gpsPointsRestored++
                }
            }

            val markFeatures = geoJson.features.filter { it.geometry.type == "Point" }
            var marksAdded = 0
            var marksUpdated = 0

            for (feature in markFeatures) {
                val markName = feature.properties.markName
                @Suppress("UNCHECKED_CAST")
                val coordinates = feature.geometry.coordinates as? List<Double>

                if (markName != null && coordinates != null && coordinates.size == 2) {
                    val existingMark = markDao.getMarkByName(markName)
                    if (existingMark != null) {
                        val updatedMark = existingMark.copy(
                            markLatitude = coordinates[1],
                            markLongitude = coordinates[0]
                        )
                        markDao.updateMark(updatedMark)
                        marksUpdated++
                    } else {
                        val newMark = MarkEntity(
                            markName = markName,
                            markLatitude = coordinates[1],
                            markLongitude = coordinates[0]
                        )
                        markDao.addMark(newMark)
                        marksAdded++
                    }
                }
            }
            Toast.makeText(safeContext, "Restore complete. GPS Points: $gpsPointsRestored, Added Marks: $marksAdded, Updated Marks: $marksUpdated.", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(safeContext, "Failed to restore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun backupRace() {
        val safeContext = context ?: return
        lifecycleScope.launch {
            val races = raceDao.getAllRaces()
            val raceDescriptions = races.map { it.raceDescription }.toTypedArray()

            AlertDialog.Builder(safeContext)
                .setTitle("Select a Race to Backup")
                .setItems(raceDescriptions) { _, which ->
                    val selectedRace = races[which]
                    selectedRaceIdForBackup = selectedRace.id
                    val fileName = "race_${selectedRace.id}_backup.json"
                    saveFileLauncher.launch(fileName)
                }
                .show()
        }
    }

    private fun exportGpx() {
        val safeContext = context ?: return
        lifecycleScope.launch {
            val races = raceDao.getAllRaces()
            val raceDescriptions = races.map { it.raceDescription }.toTypedArray()

            AlertDialog.Builder(safeContext)
                .setTitle("Select a Race to Export as GPX")
                .setItems(raceDescriptions) { _, which ->
                    val selectedRace = races[which]
                    selectedRaceIdForBackup = selectedRace.id
                    val fileName = "race_${selectedRace.id}.gpx"
                    saveGpxLauncher.launch(fileName)
                }
                .show()
        }
    }

    private fun restoreRace() {
        openFileLauncher.launch(arrayOf("application/json"))
    }

    private fun clearGpsData() {
        val safeContext = context ?: return
        AlertDialog.Builder(safeContext)
            .setTitle("Clear All Race Data")
            .setMessage("Are you sure you want to delete all race and GPS data? This cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        gpsLogDao.deleteAll()
                        raceDao.deleteAll()
                    }
                    Toast.makeText(safeContext, "All race and GPS data has been cleared.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button_backup_race -> backupRace()
            R.id.button_restore_race -> restoreRace()
            R.id.button_export_gpx -> exportGpx()
            R.id.button_clear_gps_data -> clearGpsData()
            R.id.left_btn -> navController.navigateUp()
            R.id.home_btn -> navController.navigate(R.id.mainFragment)
        }
    }
}

data class GeoJson(val type: String = "FeatureCollection", val features: List<Feature>)
data class Feature(val type: String = "Feature", val properties: Properties, val geometry: Geometry)
data class Properties(
    val raceDescription: String? = null,
    val boatName: String? = null,
    val boatId: Int? = null,
    val raceDate: String? = null,
    val windDirection: String? = null,
    val windStrength: String? = null,
    val course: String? = null,
    val courseId: Int? = null,
    val pinLatitude: Double? = null,
    val pinLongitude: Double? = null,
    val boatLatitude: Double? = null,
    val boatLongitude: Double? = null,
    val startTime: String? = null,
    val finishTime: String? = null,
    val markName: String? = null,
    val markSequence: Int = 0,
    val timestamps: List<Long>? = null,
    val accuracies: List<Float>? = null
)
data class Geometry(val type: String, val coordinates: List<Any>)
