package com.spinnaker.sailing

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BoatPerformanceDao {

    @Insert
    suspend fun insert(boatPerformance: BoatPerformanceEntity)

    @Update
    suspend fun update(boatPerformance: BoatPerformanceEntity)

    @Delete
    suspend fun delete(boatPerformance: BoatPerformanceEntity)


    @Query("select * from BoatPerformance order by boat_name asc")
    fun getAllBoatPerformance(): List<BoatPerformanceEntity>

}