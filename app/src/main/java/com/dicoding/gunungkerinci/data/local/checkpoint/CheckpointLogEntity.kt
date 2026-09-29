package com.dicoding.gunungkerinci.data.local.checkpoint

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dicoding.gunungkerinci.data.local.sync.SyncStatus

@Entity(tableName = "checkpoint_logs")
data class CheckpointLogEntity(
    @PrimaryKey val localId: String,
    val serverId: Int?,
    val bookingId: String,
    val pendakiId: String,
    val postId: Int,
    val method: String,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double? = null,
    val accuracy: Double? = null,
    /** Persen baterai saat check-in (0..100) — dikirim sebagai battery_level. */
    val batteryLevel: Int? = null,
    val checkedAt: Long,
    val syncStatus: String = SyncStatus.PENDING.name,
    val retryCount: Int = 0
)
