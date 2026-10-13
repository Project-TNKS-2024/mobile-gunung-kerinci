package com.dicoding.gunungkerinci.model

data class MyTiketResponse(
    val success: Boolean,
    val message: String?,
    val data: List<BookingDetailData>?,
    val errors: Any?
)