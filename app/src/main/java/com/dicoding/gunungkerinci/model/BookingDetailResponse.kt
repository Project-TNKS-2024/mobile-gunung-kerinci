package com.dicoding.gunungkerinci.model

data class BookingDetailResponse(
    val success: Boolean,
    val message: String,
    val data: BookingDetailData,
    val errors: Any?
)

data class BookingDetailData(
    val id: String,
    val destinasi: BookingDestination
)

data class BookingDestination(
    val id: Int,
    val nama: String,
    val sop: String
)