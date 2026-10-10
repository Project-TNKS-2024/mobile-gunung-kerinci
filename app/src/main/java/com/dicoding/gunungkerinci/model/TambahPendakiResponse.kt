package com.dicoding.gunungkerinci.model

data class TambahPendakiResponse (
    val success: Boolean,
    val message: String,
    val data: PendakiTambahData?
)

data class PendakiTambahData(
    val id: String,
    val booking_id: String,
    val tagihan: Int,
    val id_bio: String,
    val usia: Int,
    val lampiran_surat_izin_ortu: String?
)