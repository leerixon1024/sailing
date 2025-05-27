package com.spinnaker.sailing
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "BoatPerformance")
data class BoatPerformanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "boat_name") val boatName: String = "new",
@ColumnInfo(name = "true_wind") val trueWind: Float = 25F,
@ColumnInfo(name = "tack_angle") val tackAngle: Float = 90F,
@ColumnInfo(name = "target_speed") val targetSpeed: Float = 10F,


)