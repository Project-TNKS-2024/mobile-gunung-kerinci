package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/**
 * Respons GET /api/tracking/my-position — posisi terakhir pendaki aktif
 * dari gk_tracking (sumber kebenaran semua jalur kirim lokasi).
 * `data` null = belum pernah ada titik posisi tercatat.
 */
data class MyPositionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: MyPositionData?,
    @SerializedName("errors") val errors: Any?
)

data class MyPositionData(
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("altitude") val altitude: Double?,
    @SerializedName("accuracy") val accuracy: Double?,
    @SerializedName("battery_level") val batteryLevel: Int?,
    @SerializedName("recorded_at") val recordedAt: String?
)
