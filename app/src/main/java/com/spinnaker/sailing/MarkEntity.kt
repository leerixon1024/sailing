package com.spinnaker.sailing

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "Mark")
data class MarkEntity(

    @PrimaryKey(autoGenerate = true) val id: Int,

    @ColumnInfo(name = "mark_number") val markNumId: String,
    @ColumnInfo(name = "mark_name") val markName: String,
    @ColumnInfo(name = "latitude") val markLatitude: Double = 0.00,
    @ColumnInfo(name = "longitude") val markLongitude: Double = 0.00,
    @ColumnInfo(name = "degrees_from_zero") val degreesTo: Double = 0.00
)