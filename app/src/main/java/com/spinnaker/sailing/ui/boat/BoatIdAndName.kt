package com.spinnaker.sailing.ui.boat

import androidx.room.ColumnInfo

// You might define this in your Boat.kt file or a shared models file
data class BoatIdAndName(
    @ColumnInfo(name = "boat_id") val id: Int, // Or use this if you want to keep 'id'
    @ColumnInfo(name = "boat_name") val name: String // Or use this if you want to keep 'name'
)
