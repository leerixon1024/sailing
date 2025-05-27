package com.spinnaker.sailing
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "Boat")
data class BoatEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "boat_id") val id: Int = 0,
    @ColumnInfo(name = "boat_name") val boatName: String = "new",
    @ColumnInfo(name = "LOA") val loa: Float = 25.0F,
    @ColumnInfo(name = "PHRF") val pHRF: Int = 150,
    @ColumnInfo(name = "boat_make") val boatMake: String = "make",
    @ColumnInfo(name = "boat_model_name") val boatModelName: String = "model",


)