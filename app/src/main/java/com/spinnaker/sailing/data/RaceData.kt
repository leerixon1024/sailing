package com.spinnaker.sailing.data

object RaceData {
    var boat_id: Int = 0
    var course_id: Int = 0
    var course_mark_id: Int = 0 // Stores the ID from CourseMarkEntity
    var race_id: Long = 0L // Stores the ID from RaceEntity
    var mark_id: Int = 0 // Stores the ID from MarkEntity

    var race_description: String? = null  //stores Race Description
    var pin_ping: String? = null // Stores Pin Latitude/Longitude as a String
    var boat_ping: String? = null // Stores Boat Latitude/Longitude as a String
    var race_date: Int? = null // Represents race date in yyyymmdd format
    var wind_direction: Int = 0 // Wind direction in degrees
    var wind_strength: Double = 0.00 // Wind strength, e.g., in knots
    var conditions: String? = "Smooth"
    var current_speed: Float = 0.0f
    var current_direction: Int = 0
}
