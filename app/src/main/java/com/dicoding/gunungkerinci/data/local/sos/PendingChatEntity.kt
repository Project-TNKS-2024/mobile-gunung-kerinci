package com.dicoding.gunungkerinci.data.local.sos

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dicoding.gunungkerinci.data.local.sync.SyncStatus

/**
 * Pesan chat darurat yang menunggu dikirim.
 * Hanya dibuat saat SOS sudah aktif di server, karena backend butuh id_sos.
 */
@Entity(tableName = "pending_sos_chats")
data class PendingChatEntity(
    @PrimaryKey val localId: String,
    val sosId: Int,
    val type: String,          // "text" | "image"
    val content: String?,      // teks, atau caption gambar
    val imagePath: String?,    // path file internal (wajib utk type = "image")
    val createdAt: Long,
    val syncStatus: String = SyncStatus.PENDING.name,
    val retryCount: Int = 0
)
