package com.spinnaker.sailing
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "CourseMark")
data class CourseMarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "course_mark_sequence") val courseMarkSequence: Int = 0,
    @ColumnInfo(name = "course") val courseId: String?,
    @ColumnInfo(name = "mark") val markId: String?,
    @ColumnInfo(name = "Course_mark_type") val courseMarkType: String?,





)