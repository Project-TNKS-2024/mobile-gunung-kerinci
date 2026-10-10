package com.dicoding.gunungkerinci.model

data class UploadPaymentResponse(
    val success: Boolean,
    val message: String?,
    val data: BookingDetailData?,
    val errors: Any?
)