package com.spinnaker.sailing.ui.coursemark // Or your relevant package

import androidx.room.ColumnInfo

data class CourseMarkWithName(
    // Fields from CourseMarkEntity
    // Make sure these match the names and types in your CourseMarkEntity
    @ColumnInfo(name = "id") // Assuming 'id' is the primary key of CourseMark
    val id: Int,

    @ColumnInfo(name = "course_mark_sequence")
    val courseMarkSequence: Int,

    @ColumnInfo(name = "course") // This is courseId
    val courseId: Int,

    @ColumnInfo(name = "mark") // This is markId (foreign key to Mark table) - Changed from markNumId
    val markId: Int, // Changed from markNumId

    @ColumnInfo(name = "Course_mark_type")
    val courseMarkType: String?,

    // Field from the Mark table, aliased as markName in the query
    @ColumnInfo(name = "markName")
    val markName: String?
)
