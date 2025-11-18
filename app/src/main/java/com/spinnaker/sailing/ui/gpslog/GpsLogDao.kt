package com.spinnaker.sailing.ui.gpslog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spinnaker.sailing.ui.gpslog.GpsLogEntity

@Dao
interface GpsLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: GpsLogEntity)

    @Query("SELECT * FROM gps_log WHERE raceId = :raceId ORDER BY timestamp ASC")
    suspend fun getLogsForRace(raceId: Long): List<GpsLogEntity>

    @Query("DELETE FROM gps_log")
    suspend fun deleteAll()
}
