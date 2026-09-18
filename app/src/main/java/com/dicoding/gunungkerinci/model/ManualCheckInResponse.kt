package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class ManualCheckInResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: ManualCheckInData?,
    @SerializedName("errors") val errors: Any?
)

data class ManualCheckInData(
    @SerializedName("checkpoint_log_id") val checkpointLogId: Int,
    @SerializedName("post") val post: ManualCheckInPost,
    @SerializedName("is_manual_override") val isManualOverride: Boolean,
    @SerializedName("distance_from_post") val distanceFromPost: Double,
    @SerializedName("progress") val progress: ManualCheckInProgress
)

data class ManualCheckInPost(
    @SerializedName("id") val id: Int,
    @SerializedName("nama") val nama: String,
    @SerializedName("urutan") val urutan: Int
)

data class ManualCheckInProgress(
    @SerializedName("completed") val completed: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("percentage") val percentage: Int
)
