package com.dicoding.gunungkerinci.data.local.sos

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dicoding.gunungkerinci.data.local.sync.SyncStatus

/** Antrean SOS lokal — dikirim ulang saat jaringan tersedia (US-12 / TDD C06-C07). */
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
