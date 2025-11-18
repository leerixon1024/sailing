package com.spinnaker.sailing

import android.content.Context

import androidx.room.Database
import androidx.room.TypeConverters
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spinnaker.sailing.genclasses.DateConverter
import com.spinnaker.sailing.ui.boat.BoatEntity
import com.spinnaker.sailing.ui.boat.BoatDao
import com.spinnaker.sailing.ui.boatperformance.BoatPerformanceDao
import com.spinnaker.sailing.ui.boatperformance.BoatPerformanceEntity
import com.spinnaker.sailing.ui.course.CourseDao
import com.spinnaker.sailing.ui.course.CourseEntity
import com.spinnaker.sailing.ui.coursemark.CourseMarkDao
import com.spinnaker.sailing.ui.coursemark.CourseMarkEntity
import com.spinnaker.sailing.ui.gpslog.GpsLogDao
import com.spinnaker.sailing.ui.gpslog.GpsLogEntity
import com.spinnaker.sailing.ui.mark.MarkDao
import com.spinnaker.sailing.ui.mark.MarkEntity
import com.spinnaker.sailing.ui.race.RaceDao
import com.spinnaker.sailing.ui.race.RaceEntity

@Database(
    entities = [
        CourseEntity::class,
        MarkEntity::class,
        CourseMarkEntity::class,
        BoatEntity::class,
        BoatPerformanceEntity::class,
        RaceEntity::class,
        GpsLogEntity::class
    ],
    version = 20,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun MarkDao(): MarkDao
    abstract fun CourseDao(): CourseDao
    abstract fun CourseMarkDao(): CourseMarkDao
    abstract fun BoatDao(): BoatDao
    abstract fun BoatPerformanceDao(): BoatPerformanceDao
    abstract fun RaceDao(): RaceDao
    abstract fun GpsLogDao(): GpsLogDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_17_18: Migration = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE BoatPerformance ADD COLUMN leeway REAL NOT NULL DEFAULT 5.0")
            }
        }

        val MIGRATION_18_19: Migration = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE races ADD COLUMN conditions TEXT DEFAULT 'Smooth'")
            }
        }

        fun getDatabase(context: Context?): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context?.applicationContext ?: throw IllegalStateException("Application context cannot be null"),
                    AppDatabase::class.java,
                    "sailing_database" // Your database name
                )
                    .addMigrations(MIGRATION_17_18, MIGRATION_18_19)
                    .fallbackToDestructiveMigration()
                    .build()
                // Assign the instance to the INSTANCE variable
                INSTANCE = instance
                instance
            }
        }
    }
}
