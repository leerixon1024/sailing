package com.spinnaker.sailing
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "Course")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "course_type") val courseType: String,
    @ColumnInfo(name = "course_description") val courseDesc: String


)
