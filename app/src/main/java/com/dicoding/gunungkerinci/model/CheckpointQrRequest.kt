package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class CheckpointQrRequest(
    @SerializedName("qr_code_value") val qrCodeValue: String,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)
