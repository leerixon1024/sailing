package com.spinnaker.sailing.ui.course
import androidx.room.ColumnInfo
data class CourseAndDescription(
    @ColumnInfo(name = "course_id") val id: Int,
    @ColumnInfo(name = "course_description") val courseDescription: String
)
