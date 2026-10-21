package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/** Respons check-in v2 (QR & manual) `POST /api/tracking/v2/checkpoint/qr` dan `/manual`. */
data class CheckpointV2Response(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: CheckpointV2Data?,
    @SerializedName("errors") val errors: CheckpointV2Error?
)

data class CheckpointV2Data(
    @SerializedName("checkpoint_log_id") val checkpointLogId: Int,
    @SerializedName("client_event_id") val clientEventId: String? = null,
    @SerializedName("pendaki_id") val pendakiId: String? = null,
    @SerializedName("phase") val phase: String,
    @SerializedName("checked_at") val checkedAt: String? = null,
    @SerializedName("post") val post: CheckpointQrPost,
    @SerializedName("visit_key") val visitKey: String? = null,
    @SerializedName("tracking_phase") val trackingPhase: String? = null,
    @SerializedName("next_visit_key") val nextVisitKey: String? = null,
    @SerializedName("ascent_progress") val ascentProgress: PhaseProgress? = null,
    @SerializedName("descent_progress") val descentProgress: PhaseProgress? = null,
    @SerializedName("overall_progress") val overallProgress: PhaseProgress? = null,
    @SerializedName("idempotent") val idempotent: Boolean = false,
    @SerializedName("is_manual_override") val isManualOverride: Boolean = false,
    @SerializedName("distance_from_post") val distanceFromPost: Double? = null
)

/** Kode kesalahan mesin dari backend, mis. `DESCENT_REQUIRES_SUMMIT`. */
data class CheckpointV2Error(
    @SerializedName("code") val code: String? = null,
    @SerializedName("checkpoint_log_id") val checkpointLogId: Int? = null,
    @SerializedName("checked_at") val checkedAt: String? = null,
    @SerializedName("visit_key") val visitKey: String? = null,
    @SerializedName("summit_post_id") val summitPostId: Int? = null
)
