package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/**
 * Respons `DELETE /api/tracking/checkpoint/{checkpoint_log_id}`.
 * Dipakai saat kehadiran pada satu pos dibatalkan (toggle dimatikan).
 */
data class CancelCheckpointResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: CancelCheckpointData?,
    @SerializedName("errors") val errors: Any?
)

data class CancelCheckpointData(
    @SerializedName("checkpoint_log_id") val checkpointLogId: Int,
    /** UUID pendaki yang dibatalkan kehadirannya. */
    @SerializedName("pendaki_id") val pendakiId: String? = null,
    @SerializedName("post") val post: CheckpointQrPost,
    /** Progress pendaki tersebut setelah pembatalan. */
    @SerializedName("progress") val progress: CheckpointQrProgress
)
