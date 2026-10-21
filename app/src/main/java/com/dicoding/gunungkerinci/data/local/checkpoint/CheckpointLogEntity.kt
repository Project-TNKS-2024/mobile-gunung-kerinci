package com.dicoding.gunungkerinci.data.local.checkpoint

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dicoding.gunungkerinci.data.local.sync.SyncStatus
import com.dicoding.gunungkerinci.model.TrackingPhase

@Entity(tableName = "checkpoint_logs")
data class CheckpointLogEntity(
    @PrimaryKey val localId: String,
    val serverId: Int?,
    val bookingId: String,
    val pendakiId: String,
    val postId: Int,
    /** Fase kunjungan "naik"/"turun". Log lama migrasi ke "naik". */
    val phase: String = TrackingPhase.NAIK.serialized,
    /** Identitas kejadian untuk retry idempoten; dibuat sekali dan tidak diubah. */
    val clientEventId: String? = null,
    /** Penanda puncak jalur — dipakai worker untuk dependensi summit→turun. */
    @ColumnInfo(defaultValue = "0") val isSummit: Boolean = false,
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

/**
 * Kunci utama lokal satu kunjungan: booking + pendaki + pos + fase.
 * Fase di akhir agar migrasi lama cukup menambahkan "_naik" tanpa parsing UUID.
 */
internal fun checkpointLocalId(bookingId: String, pendakiId: String, postId: Int, phase: String): String =
    "${bookingId}_${pendakiId}_${postId}_$phase"
