package com.spinnaker.sailing.genclasses
import androidx.room.TypeConverter
import java.util.Date

class DateConverter {
    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return date?.time // Convert Date to Long (timestamp in milliseconds)
    }

    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) } // Convert Long back to Date
    }
}
