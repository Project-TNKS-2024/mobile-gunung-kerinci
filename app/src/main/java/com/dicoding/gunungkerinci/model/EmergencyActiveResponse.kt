package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/** Respons GET /api/emergency/active (peringatan dini bahaya untuk pendaki aktif). */
data class EmergencyActiveResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<EmergencyWarningDto>?,
    @SerializedName("errors") val errors: Any?
)

data class EmergencyWarningDto(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("severity") val severity: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("created_at") val createdAt: String?
)
