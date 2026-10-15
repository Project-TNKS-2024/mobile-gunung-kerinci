package com.dicoding.gunungkerinci.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogDao
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogEntity
import com.dicoding.gunungkerinci.data.local.checkpoint.TrackingPostDao
import com.dicoding.gunungkerinci.data.local.checkpoint.TrackingPostEntity
//import com.dicoding.gunungkerinci.data.local.disaster.PendingDisasterReportDao
//import com.dicoding.gunungkerinci.data.local.disaster.PendingDisasterReportEntity
//import com.dicoding.gunungkerinci.data.local.message.PendingEmergencyMessageDao
//import com.dicoding.gunungkerinci.data.local.message.PendingEmergencyMessageEntity
import com.dicoding.gunungkerinci.data.local.sos.PendingChatDao
import com.dicoding.gunungkerinci.data.local.sos.PendingChatEntity
import com.dicoding.gunungkerinci.data.local.sos.PendingSosDao
import com.dicoding.gunungkerinci.data.local.sos.PendingSosEntity
import com.dicoding.gunungkerinci.data.local.warning.PeringatanDiniDao
import com.dicoding.gunungkerinci.data.local.warning.PeringatanDiniEntity
import com.dicoding.gunungkerinci.data.local.warning.PeringatanDiniMetaEntity

@Database(
    entities = [
        TrackingPostEntity::class,
        CheckpointLogEntity::class,
        PendingSosEntity::class,
        PendingChatEntity::class,
//        PendingEmergencyMessageEntity::class,
//        PendingDisasterReportEntity::class,
        PeringatanDiniEntity::class,
        PeringatanDiniMetaEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class TnksDatabase : RoomDatabase() {
    abstract fun trackingPostDao(): TrackingPostDao
    abstract fun checkpointLogDao(): CheckpointLogDao
    abstract fun pendingSosDao(): PendingSosDao
    abstract fun pendingChatDao(): PendingChatDao
//    abstract fun pendingEmergencyMessageDao(): PendingEmergencyMessageDao
//    abstract fun pendingDisasterReportDao(): PendingDisasterReportDao
    abstract fun peringatanDiniDao(): PeringatanDiniDao

    companion object {
        @Volatile
        private var INSTANCE: TnksDatabase? = null

        fun getInstance(context: Context): TnksDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TnksDatabase::class.java,
                    "tnks_safety.db"
                )
                    .addMigrations(object : Migration(7, 8) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE tracking_posts ADD COLUMN estimatedTimeMinutes INTEGER")
                            db.execSQL("ALTER TABLE tracking_posts ADD COLUMN infoIsDummy INTEGER NOT NULL DEFAULT 0")
                        }
                    })
                    .addMigrations(object : Migration(8, 9) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            // Version 8 was used with two variants of tracking_posts.
                            val columns = mutableSetOf<String>()
                            db.query("PRAGMA table_info(tracking_posts)").use { cursor ->
                                val nameIndex = cursor.getColumnIndexOrThrow("name")
                                while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
                            }
                            if ("estimatedTimeMinutes" !in columns) {
                                db.execSQL("ALTER TABLE tracking_posts ADD COLUMN estimatedTimeMinutes INTEGER")
                            }
                            if ("infoIsDummy" !in columns) {
                                db.execSQL("ALTER TABLE tracking_posts ADD COLUMN infoIsDummy INTEGER NOT NULL DEFAULT 0")
                            }
                        }
                    })
                    .addMigrations(object : Migration(9, 10) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE tracking_posts ADD COLUMN temperatureC REAL")
                            db.execSQL("ALTER TABLE tracking_posts ADD COLUMN distanceToSummitM INTEGER")
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
