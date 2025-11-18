package com.spinnaker.sailing.ui.race

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "races")
data class RaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val raceDescription: String?,
    val boatName: String?,
    val boatId: Int?,
    val raceDate: Date?,
    val windDirection: String?,
    val windStrength: String?,
    val course: String?,
    val courseId: Int?,
    val pinLatitude: Double?,
    val pinLongitude: Double?,
    val boatLatitude: Double?,
    val boatLongitude: Double?,
    val startTime: String?, // Format: HH:mm:ss
    val finishTime: String?, // Format: HH:mm:ss
    val conditions: String? = "Smooth"
)
