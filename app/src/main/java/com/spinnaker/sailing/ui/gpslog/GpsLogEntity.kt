package com.spinnaker.sailing.ui.gpslog

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gps_log")
data class GpsLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val raceId: Long, // Changed from Int to Long
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val bearing: Double,
    val accuracy: Float
)
