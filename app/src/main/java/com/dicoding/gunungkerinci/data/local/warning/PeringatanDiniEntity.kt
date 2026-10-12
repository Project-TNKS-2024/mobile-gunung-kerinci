package com.dicoding.gunungkerinci.data.local.warning

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peringatan_dini_cache")
data class PeringatanDiniEntity(
    @PrimaryKey val id: Int,
    val type: String?,
    val title: String?,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val severity: String?,
    val createdAt: String?
)

// Penanda sinkronisasi cache (1 baris, id selalu 0): waktu sync terakhir + pemiliknya.
// Dipakai supaya label "terakhir diperbarui" tetap ada walau daftar peringatan kosong,
// dan cache tidak tertukar antar pengguna.
@Entity(tableName = "peringatan_dini_meta")
data class PeringatanDiniMetaEntity(
    @PrimaryKey val id: Int = 0,
    val ownerKey: String,
    val lastSyncEpochMillis: Long
)
