package com.spinnaker.sailing

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CourseEntity::class,
        MarkEntity::class,
        CourseMarkEntity::class,
        BoatEntity::class,
        BoatPerformanceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun MarkDao(): MarkDao
    abstract fun CourseDao(): CourseDao
    abstract fun CourseMarkDao(): CourseMarkDao
    abstract fun BoatDao(): BoatDao
    abstract fun BoatPerformanceDao(): BoatPerformanceDao
}
