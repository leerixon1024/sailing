package com.spinnaker.sailing.ui.coursemark
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "CourseMark")
data class CourseMarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "course_mark_sequence") val courseMarkSequence: Int = 0,
    @ColumnInfo(name = "course") val courseId: Int = 0,
    @ColumnInfo(name = "mark") val markId: Int = 0, // Changed from markNumId to markId

)
