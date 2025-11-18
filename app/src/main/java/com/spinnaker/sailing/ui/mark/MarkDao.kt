package com.spinnaker.sailing.ui.mark

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkDao {

    @Insert
    suspend fun addMark(mark: MarkEntity): Long

    @Update
    suspend fun updateMark(mark: MarkEntity): Int

    @Delete
    suspend fun deleteMark(mark: MarkEntity): Int

    @Query("DELETE FROM Mark")
    suspend fun deleteAll()
    @Query("SELECT * FROM Mark WHERE id = :id")
    suspend fun getMarkById(id: Int): MarkEntity?

    @Query("SELECT * FROM Mark WHERE mark_name = :name LIMIT 1")
    suspend fun getMarkByName(name: String): MarkEntity?

    @Query("select * from Mark order by mark_name asc")
    fun  getAllMarks(): Flow<List<MarkEntity>>

    @Query("select * from Mark order by mark_name asc")
    suspend fun getAllMarksList(): List<MarkEntity>


}