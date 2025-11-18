package com.spinnaker.sailing.genclasses

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.asin


class GeoCalculator {

    companion object {
        private const val EARTH_RADIUS_NM = 3440.065 // Earth radius in nautical miles

        /**
         * Calculates the distance between two sets of coordinates using the Haversine formula.
         *
         * @param coord1 The starting coordinates.
         * @param coord2 The destination coordinates.
         * @return The distance in nautical miles.
         */
        fun calculateDistanceNm(coord1: Coordinates, coord2: Coordinates): Double {
            val lat1Rad = Math.toRadians(coord1.latitude)
            val lon1Rad = Math.toRadians(coord1.longitude)
            val lat2Rad = Math.toRadians(coord2.latitude)
            val lon2Rad = Math.toRadians(coord2.longitude)

            val deltaLat = lat2Rad - lat1Rad
            val deltaLon = lon2Rad - lon1Rad

            val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                    cos(lat1Rad) * cos(lat2Rad) *
                    sin(deltaLon / 2) * sin(deltaLon / 2)
            val c = 2 * asin(sqrt(a)) // Central angle in radians
            return EARTH_RADIUS_NM * c
        }

        enum class LineSide { LEFT, RIGHT, COLINEAR }

        fun getSideOfLine(p: Coordinates, lineP1: Coordinates, lineP2: Coordinates): LineSide {
            val crossProduct = (lineP2.longitude - lineP1.longitude) * (p.latitude - lineP1.latitude) -
                      (lineP2.latitude - lineP1.latitude) * (p.longitude - lineP1.longitude)

            return when {
                crossProduct > 1e-9 -> LineSide.LEFT // Point is on the left side
                crossProduct < -1e-9 -> LineSide.RIGHT // Point is on the right side
                else -> LineSide.COLINEAR // Point is on the line
            }
        }
    }

    data class Coordinates(val latitude: Double, val longitude: Double)

    data class CalculationResult(
        val bearing: Double,
        val reciprocalBearing: Double,
        val distanceNm: Double
    )

    /**
     * Calculates the bearing, reciprocal bearing, and distance between two sets of coordinates.
     *
     * @param coord1 The starting coordinates.
     * @param coord2 The destination coordinates.
     * @return CalculationResult containing bearing, reciprocal bearing (both in degrees),
     *         and distance (in nautical miles).
     */
    fun calculate(coord1: Coordinates, coord2: Coordinates): CalculationResult {
        val lat1Rad = Math.toRadians(coord1.latitude)
        val lon1Rad = Math.toRadians(coord1.longitude)
        val lat2Rad = Math.toRadians(coord2.latitude)
        val lon2Rad = Math.toRadians(coord2.longitude)

        val deltaLon = lon2Rad - lon1Rad

        // Calculate Bearing
        val y = sin(deltaLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLon)
        var initialBearing = Math.toDegrees(atan2(y, x))
        initialBearing = (initialBearing + 360) % 360 // Normalize to 0-360 degrees

        // Calculate Reciprocal Bearing
        val reciprocalBearing = (initialBearing + 180) % 360

        // Calculate Distance
        val distanceNm = calculateDistanceNm(coord1, coord2)

        return CalculationResult(initialBearing, reciprocalBearing, distanceNm)
    }
}
