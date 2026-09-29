package com.dicoding.gunungkerinci.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogDao
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogEntity
import com.dicoding.gunungkerinci.data.local.checkpoint.TrackingPostDao
import com.dicoding.gunungkerinci.data.local.checkpoint.TrackingPostEntity
//import com.dicoding.gunungkerinci.data.local.disaster.PendingDisasterReportDao
//import com.dicoding.gunungkerinci.data.local.disaster.PendingDisasterReportEntity
//import com.dicoding.gunungkerinci.data.local.message.PendingEmergencyMessageDao
//import com.dicoding.gunungkerinci.data.local.message.PendingEmergencyMessageEntity
//import com.dicoding.gunungkerinci.data.local.sos.PendingSosDao
//import com.dicoding.gunungkerinci.data.local.sos.PendingSosEntity
//import com.dicoding.gunungkerinci.data.local.warning.EarlyWarningDao
//import com.dicoding.gunungkerinci.data.local.warning.EarlyWarningEntity

@Database(
    entities = [
        TrackingPostEntity::class,
        CheckpointLogEntity::class,
//        PendingSosEntity::class,
//        PendingEmergencyMessageEntity::class,
//        PendingDisasterReportEntity::class,
//        EarlyWarningEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class TnksDatabase : RoomDatabase() {
    abstract fun trackingPostDao(): TrackingPostDao
    abstract fun checkpointLogDao(): CheckpointLogDao
//    abstract fun pendingSosDao(): PendingSosDao
//    abstract fun pendingEmergencyMessageDao(): PendingEmergencyMessageDao
//    abstract fun pendingDisasterReportDao(): PendingDisasterReportDao
//    abstract fun earlyWarningDao(): EarlyWarningDao

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
                    // Skema v4: checkpoint_logs + batteryLevel (persen baterai saat check-in).
                    // Skema v3: checkpoint_logs + altitude/accuracy (titik posisi gk_tracking).
                    // Cache lokal → fallbackToDestructiveMigration aman.
                    // Skema v2: bookingId/pendakiId jadi String (UUID backend).
                    // Cache lokal belum berisi data penting saat upgrade → cukup bangun ulang.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
