package com.spinnaker.sailing.data

import com.spinnaker.sailing.genclasses.GeoCalculator
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GpsData {
    var currentLatitude: Double = 0.0
    var currentLongitude: Double = 0.0
    var currentAccuracy: Float = 0.0f
    var currentTimestamp: Long = 0L
    var magneticDeclination: Double = 0.0

    val historicalLatitudes = mutableListOf<Double>()
    val historicalLongitudes = mutableListOf<Double>()
    val historicalTimestamps = mutableListOf<Long>()

    private const val MAX_HISTORY_SIZE = 100

    fun updateLocation(newLatitude: Double, newLongitude: Double, newAccuracy: Float, newTimestamp: Long) {
        if (currentTimestamp != 0L) { // Avoid adding the initial 0.0, 0.0 location
            historicalLatitudes.add(currentLatitude)
            historicalLongitudes.add(currentLongitude)
            historicalTimestamps.add(currentTimestamp)
        }

        // Trim lists to maintain a manageable size
        if (historicalLatitudes.size > MAX_HISTORY_SIZE) {
            historicalLatitudes.removeAt(0)
            historicalLongitudes.removeAt(0)
            historicalTimestamps.removeAt(0)
        }

        currentLatitude = newLatitude
        currentLongitude = newLongitude
        currentAccuracy = newAccuracy
        currentTimestamp = newTimestamp
    }

    fun clearData() {
        currentLatitude = 0.0
        currentLongitude = 0.0
        currentAccuracy = 0.0f
        currentTimestamp = 0L
        magneticDeclination = 0.0
        historicalLatitudes.clear()
        historicalLongitudes.clear()
        historicalTimestamps.clear()
    }

    fun calculateSpeedInKnots(): Double {
        if (historicalLatitudes.isEmpty()) {
            return 0.0
        }

        val lastLat = historicalLatitudes.last()
        val lastLon = historicalLongitudes.last()
        val lastTimestamp = historicalTimestamps.last()

        val distanceNm = GeoCalculator.calculateDistanceNm(
            GeoCalculator.Coordinates(lastLat, lastLon),
            GeoCalculator.Coordinates(currentLatitude, currentLongitude)
        )

        val timeDiffSeconds = (currentTimestamp - lastTimestamp) / 1000.0
        if (timeDiffSeconds <= 0) {
            return 0.0
        }

        return distanceNm / (timeDiffSeconds / 3600.0) // Speed in knots
    }
}
