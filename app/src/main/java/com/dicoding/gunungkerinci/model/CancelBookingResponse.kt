package com.dicoding.gunungkerinci.model

data class CancelBookingResponse (
    val success: Boolean,
    val message: String?,
    val data: Any?,
    val errors: Any?
)