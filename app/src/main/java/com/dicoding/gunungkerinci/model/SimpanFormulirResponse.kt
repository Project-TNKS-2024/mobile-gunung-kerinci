package com.dicoding.gunungkerinci.model

data class SimpanFormulirResponse(
    val success: Boolean,
    val message: String,
    val data: BookingFormulirData?,
    val errors: Any?
)

data class BookingFormulirData(
    val id: String,
    val id_user: Int,
    val id_tiket: Int,
    val total_hari: Int,
    val total_pendaki_wni: Int,
    val total_pendaki_wna: Int
)