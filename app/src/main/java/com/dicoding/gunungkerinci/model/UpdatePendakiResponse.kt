package com.dicoding.gunungkerinci.model

data class UpdatePendakiResponse(
    val success: Boolean,
    val message: String,
    val data: BookingUpdateData
)

data class BookingUpdateData(
    val id: String,
    val id_user: Int,
    val id_tiket: Int,
    val tanggal_masuk: String,
    val tanggal_keluar: String,
    val total_hari: Int,
    val total_pendaki_wni: Int,
    val total_pendaki_wna: Int
)