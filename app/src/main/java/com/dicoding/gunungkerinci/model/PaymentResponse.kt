package com.dicoding.gunungkerinci.model

data class PaymentResponse(
    val success: Boolean,
    val message: String?,
    val data: PaymentData?,
    val errors: Any?
)

data class PaymentData(
    val booking: BookingDetailData,
    val bank: PaymentBank?,
    val qris: String?
)

data class PaymentBank(
    val id: String?,
    val nama: String?,
    val text1: String?,
    val text2: String?
)