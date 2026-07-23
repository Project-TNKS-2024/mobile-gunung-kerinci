package com.dicoding.gunungkerinci.model

data class CreateBookingResponse(
    val success: Boolean,
    val message: String,
    val data: BookingData?,
    val errors: Any?
)

data class BookingData (
    val id: String,
    val tanggal_masuk: String,
    val tanggal_keluar: String,
    val total_hari: Int,

    val total_pendaki_wni: Int,
    val total_pendaki_wna: Int,

    val total_pembayaran: Int,

    val gate_masuk: BookingGate,
    val gate_keluar: BookingGate,

    val gktiket: BookingJenisTiket,
    val destinasi: BookingDestinasi
)

data class BookingDestinasi (
    val id: Int,
    val nama: String,
    val status: Int
)

data class BookingJenisTiket (
    val id: Int,
    val nama: String,
    val min_pendaki: Int
)

data class BookingGate (
    val id: Int,
    val nama: String,
    val max_pendaki_hari: Int
)
