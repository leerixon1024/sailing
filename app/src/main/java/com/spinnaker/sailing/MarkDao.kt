package com.spinnaker.sailing

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MarkDao {

    @Insert
    suspend fun insert(mark: MarkEntity)

    @Update
    suspend fun update(mark: MarkEntity)

    @Delete
    suspend fun delete(mark: MarkEntity)

    @Query("SELECT * FROM Mark WHERE id = :id")
    fun getMarkById(id: Int): MarkEntity?

    @Query("select * from Mark order by mark_name asc")
    fun  getAllMarks(): List<MarkEntity>


}