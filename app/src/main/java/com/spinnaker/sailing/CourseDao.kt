package com.spinnaker.sailing

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface CourseDao {
    @Insert
    suspend fun insert(course: CourseEntity)

    @Update
    suspend fun update(course: CourseEntity)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query ("SELECT * FROM Course WHERE id = :id")
    fun selectSingleCourse(id: Int): LiveData<CourseEntity>


    @Query("SELECT * FROM Course")
    fun getAllCourses(): List<CourseEntity>

}