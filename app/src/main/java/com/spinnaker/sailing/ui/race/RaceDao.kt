package com.spinnaker.sailing.ui.race

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRace(race: RaceEntity): Long

    @Update
    suspend fun update(race: RaceEntity)

    @Query("SELECT * FROM races WHERE boatId = :boatId")
    suspend fun getRacesForBoat(boatId: String): List<RaceEntity>

    @Query("SELECT * FROM races")
    suspend fun getAllRaces(): List<RaceEntity>

    @Query("SELECT * FROM races WHERE id = :raceId")
    suspend fun getRaceById(raceId: String): RaceEntity?

    @Query("DELETE FROM races")
    suspend fun deleteAll()
}
