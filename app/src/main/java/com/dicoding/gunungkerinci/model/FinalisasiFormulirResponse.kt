package com.dicoding.gunungkerinci.model

data class FinalisasiFormulirResponse(
    val success: Boolean,
    val message: String,
    val data: BookingFinalisasiData?,
    val errors: Any?
)

data class BookingFinalisasiData(
    val id: String,
    val id_user: Int,
    val id_tiket: Int,
    val total_hari: Int,
    val total_pendaki_wni: Int,
    val total_pendaki_wna: Int
)