package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class TrackingProgressResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: TrackingProgressData?,
    @SerializedName("errors") val errors: Any?
)

data class TrackingProgressData(
    @SerializedName("booking_id") val bookingId: String,
    @SerializedName("gate") val gate: String?,
    @SerializedName("total_posts") val totalPosts: Int,
    @SerializedName("hikers") val hikers: List<TrackingProgressHiker>
)

data class TrackingProgressHiker(
    @SerializedName("pendaki_id") val pendakiId: String,
    @SerializedName("nama") val nama: String,
    @SerializedName("completed") val completed: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("percentage") val percentage: Int,
    @SerializedName("checkpoints") val checkpoints: List<TrackingProgressCheckpoint>
)

data class TrackingProgressCheckpoint(
    @SerializedName("post_id") val postId: Int,
    @SerializedName("nama") val nama: String,
    @SerializedName("urutan") val urutan: Int,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("checked_at") val checkedAt: String?,
    @SerializedName("method") val method: String?
)
