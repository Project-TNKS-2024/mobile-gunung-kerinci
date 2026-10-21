package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class ManualCheckInRequest(
    @SerializedName("post_id") val postId: Int,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("phase") val phase: String? = null,
    @SerializedName("client_event_id") val clientEventId: String? = null,
    @SerializedName("recorded_at") val recordedAt: String? = null
)
