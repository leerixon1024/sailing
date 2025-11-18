package com.spinnaker.sailing.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.spinnaker.sailing.data.GpsData
import java.util.concurrent.ConcurrentHashMap

class LocationManager private constructor(
    private val context: Context,
    private val priority: Int = Priority.PRIORITY_HIGH_ACCURACY
) {

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null
    private val listeners = ConcurrentHashMap<(Location) -> Unit, Unit>()

    private val locationRequest: LocationRequest = LocationRequest.Builder(priority, 1000)
        .setMinUpdateIntervalMillis(500)
        .build()

    fun addLocationListener(listener: (Location) -> Unit) {
        listeners[listener] = Unit
        startLocationUpdates() 
    }

    fun removeLocationListener(listener: (Location) -> Unit) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            stopLocationUpdates()
        }
    }

    fun startLocationUpdates() {
        if (locationCallback != null) {
            // Already running
            return
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("Location permission not granted")
            return
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { newLocation ->
                    if (newLocation.hasAltitude()) {
                        val geomagneticField = GeomagneticField(
                            newLocation.latitude.toFloat(),
                            newLocation.longitude.toFloat(),
                            newLocation.altitude.toFloat(),
                            newLocation.time
                        )
                        GpsData.magneticDeclination = geomagneticField.declination.toDouble()
                    }
                    GpsData.updateLocation(newLocation.latitude, newLocation.longitude, newLocation.accuracy, newLocation.time)

                    listeners.keys.forEach { it.invoke(newLocation) }
                }
            }
        }

        locationCallback?.let {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                it,
                Looper.getMainLooper()
            )
        }
        println("Location updates started")
    }

    fun stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
            locationCallback = null
            println("Location updates stopped")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LocationManager? = null

        fun getInstance(context: Context, priority: Int = Priority.PRIORITY_HIGH_ACCURACY): LocationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationManager(context.applicationContext, priority).also { INSTANCE = it }
            }
        }
        
        fun calculateMagneticDeclinationForPoint(latitude: Double, longitude: Double, altitude: Float, time: Long): Double {
            val geomagneticField = GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                altitude,
                time
            )
            return geomagneticField.declination.toDouble()
        }
    }
}
