package com.spinnaker.sailing.ui.mark

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "Mark")
data class MarkEntity(

    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    @ColumnInfo(name = "mark_name") val markName: String,
    @ColumnInfo(name = "latitude") val markLatitude: Double = 0.00,
    @ColumnInfo(name = "longitude") val markLongitude: Double = 0.00,

)