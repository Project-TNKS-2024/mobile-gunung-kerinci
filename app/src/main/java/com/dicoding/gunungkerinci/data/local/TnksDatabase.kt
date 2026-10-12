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
    version = 7,
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
                    // Versi skema naik (6 -> 7) tanpa migrasi, jadi Room membangun ulang DB.
                    // Wajib ada, kalau tidak app crash. Data yang belum tersinkron ikut hilang.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
