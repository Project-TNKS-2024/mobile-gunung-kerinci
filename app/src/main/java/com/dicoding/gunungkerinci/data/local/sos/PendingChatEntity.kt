package com.dicoding.gunungkerinci.data.local.sos

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dicoding.gunungkerinci.data.local.sync.SyncStatus

/**
 * Antrean pesan chat darurat lokal — dikirim ulang saat jaringan tersedia
 * (US-12: "pesan darurat tersimpan secara lokal").
 *
 * Hanya diisi saat SOS sudah aktif di server (sosId valid) — chat tanpa induk
 * SOS tidak bisa dibuat (kontrak backend: gk_sos_chats.id_sos FK).
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
