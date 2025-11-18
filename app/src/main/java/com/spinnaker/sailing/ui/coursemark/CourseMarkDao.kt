package com.spinnaker.sailing.ui.coursemark

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

data class CourseMarkWithMark(
    @Embedded val courseMark: CourseMarkEntity,
    val markName: String,
    val markLatitude: Double,
    val markLongitude: Double
)

@Dao
interface CourseMarkDao {
    @Insert
    suspend fun insert(courseMark: CourseMarkEntity)

    @Update
    suspend fun update(courseMark: CourseMarkEntity)

    @Delete
    suspend fun delete(courseMark: CourseMarkEntity)

    @Query ("DELETE FROM CourseMark WHERE course = :courseSelect")
    suspend fun deleteSingleCourse(courseSelect: Int)

    @Query ("DELETE FROM CourseMark WHERE id = :idSelect")
    suspend fun deleteSingleMark(idSelect: Int)

    @Query("SELECT * FROM CourseMark")
    suspend fun getAllCourseMarks(): List<CourseMarkEntity>

    @Query("SELECT * FROM CourseMark WHERE course = :courseSelect  ")
    suspend fun getSingleCourseMarks(courseSelect: Int): List<CourseMarkEntity>

    @Query("SELECT * FROM CourseMark WHERE id = :courseMarkId")
    suspend fun getCourseMarkById(courseMarkId: Int): CourseMarkEntity?

    // Added method to get a CourseMarkEntity by its course ID and sequence
    @Query("SELECT * FROM CourseMark WHERE course = :courseId AND course_Mark_Sequence = :sequence")
    suspend fun getCourseMarkByCourseIdAndSequence(courseId: Int, sequence: Int): CourseMarkEntity?

    @Query("SELECT cm.*, m.mark_name AS markName, m.latitude AS markLatitude, m.longitude AS markLongitude FROM CourseMark cm INNER JOIN Mark m ON cm.mark = m.Id WHERE cm.course = :courseSelect")
    suspend fun getCourseMarksWithMark(courseSelect: Int): List<CourseMarkWithMark>

    @Query("SELECT MAX(course_Mark_Sequence) FROM CourseMark WHERE course = :courseSelect")
    suspend fun getHighestCourseMarkSequence(courseSelect: Int): Int?

}
