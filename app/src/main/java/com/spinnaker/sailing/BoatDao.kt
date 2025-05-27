package com.spinnaker.sailing

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BoatDao {

    @Insert
    suspend fun insert(boat: BoatEntity)

    @Update
    suspend fun update(boat: BoatEntity)

    @Delete
    suspend fun delete(boat: BoatEntity)


    @Query("select * from Boat order by boat_name asc")
    fun getAllBoats(): LiveData<List<BoatEntity>>

    @Query ("select * FROM Boat WHERE boat_id = :boatId")
    fun getSingleBoat(boatId: Int): LiveData<BoatEntity>

}
