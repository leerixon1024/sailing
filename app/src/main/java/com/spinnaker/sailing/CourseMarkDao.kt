package com.spinnaker.sailing

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CourseMarkDao {
    @Insert
    suspend fun insert(courseMark: CourseMarkEntity)

    @Update
    suspend fun update(courseMark: CourseMarkEntity)

    @Delete
    suspend fun delete(courseMark: CourseMarkEntity)

    @Delete
    suspend fun deleteAllCourseMarksForCourse(courseMark: CourseMarkEntity)

    @Query ("DELETE FROM CourseMark WHERE course = :courseSelect")
    suspend fun deleteSingleCourse(courseSelect: String)

    @Query ("DELETE FROM CourseMark WHERE mark = :markSelect")
    suspend fun deleteSingleMark(markSelect: String)

    @Query("SELECT * FROM CourseMark")
    suspend fun getAllCourseMarks(): List<CourseMarkEntity>

    @Query("SELECT * FROM CourseMark WHERE course = :courseSelect  ")
    suspend fun getSingleCourseMarks(courseSelect: String): List<CourseMarkEntity>

}