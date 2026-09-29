package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/** Body POST /api/sos/trigger. */
data class SosTriggerRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("severity") val severity: String,
    @SerializedName("message") val message: String? = null
)
