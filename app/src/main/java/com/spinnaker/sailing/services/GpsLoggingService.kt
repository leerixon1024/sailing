package com.spinnaker.sailing.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Priority
import com.spinnaker.sailing.AppDatabase
import com.spinnaker.sailing.R
import com.spinnaker.sailing.location.LocationManager
import com.spinnaker.sailing.ui.gpslog.GpsLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GpsLoggingService : Service() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var locationManager: LocationManager
    private var localRaceId: Long = 0L

    private val locationListener: (Location) -> Unit = { location ->
        if (localRaceId != 0L) {
            logGpsLocation(location)
        }
    }

    companion object {
        const val EXTRA_RACE_ID = "com.spinnaker.sailing.services.extra.RACE_ID"
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = LocationManager.getInstance(applicationContext, Priority.PRIORITY_HIGH_ACCURACY)
        locationManager.addLocationListener(locationListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf() // Cannot run without an intent
            return START_NOT_STICKY
        }

        val intentRaceId = intent.getLongExtra(EXTRA_RACE_ID, 0L)
        if (intentRaceId != 0L) {
            this.localRaceId = intentRaceId
        } else if (this.localRaceId == 0L) {
            // If we don't get a valid race ID and don't already have one, we can't continue.
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = createNotification()
        startForeground(1, notification)

        locationManager.startLocationUpdates()

        return START_REDELIVER_INTENT
    }

    private fun logGpsLocation(location: Location) {
        coroutineScope.launch {
            val gpsLogDao = AppDatabase.getDatabase(applicationContext).GpsLogDao()
            val log = GpsLogEntity(
                raceId = localRaceId,
                timestamp = location.time,
                latitude = location.latitude,
                longitude = location.longitude,
                bearing = if (location.hasBearing()) location.bearing.toDouble() else 0.0,
                accuracy = location.accuracy
            )
            gpsLogDao.insert(log)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeLocationListener(locationListener)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "gps_logging_channel",
            "GPS Logging",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "gps_logging_channel")
            .setContentTitle("Sailing GPS Logging")
            .setContentText("Recording race track.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
}
