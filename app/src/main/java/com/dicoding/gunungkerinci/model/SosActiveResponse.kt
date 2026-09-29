package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/** Respons GET /api/sos/active (data null bila tidak ada SOS aktif). */
data class SosActiveResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: SosActiveData?,
    @SerializedName("errors") val errors: Any?
)

data class SosActiveData(
    @SerializedName("id") val id: Int,
    @SerializedName("severity") val severity: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("created_at") val createdAt: String?
)
