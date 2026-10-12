package com.dicoding.gunungkerinci.data.local.sos

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dicoding.gunungkerinci.data.local.sync.SyncStatus

/** SOS yang menunggu dikirim ke server. */
@Entity(tableName = "pending_sos")
data class PendingSosEntity(
    @PrimaryKey val localId: String,
    val latitude: Double,
    val longitude: Double,
    val severity: String,
    val message: String?,
    val createdAt: Long,
    val syncStatus: String = SyncStatus.PENDING.name,
    val retryCount: Int = 0
)
