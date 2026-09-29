package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class CheckpointQrResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: CheckpointQrData?,
    @SerializedName("errors") val errors: Any?
)

data class CheckpointQrData(
    @SerializedName("checkpoint_log_id") val checkpointLogId: Int,
    /** UUID pendaki yang tercatat — pembeda antara scan sendiri dan perwakilan ketua. */
    @SerializedName("pendaki_id") val pendakiId: String? = null,
    @SerializedName("post") val post: CheckpointQrPost,
    @SerializedName("progress") val progress: CheckpointQrProgress
)

data class CheckpointQrPost(
    @SerializedName("id") val id: Int,
    @SerializedName("nama") val nama: String,
    @SerializedName("urutan") val urutan: Int
)

data class CheckpointQrProgress(
    @SerializedName("completed") val completed: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("percentage") val percentage: Int
)
