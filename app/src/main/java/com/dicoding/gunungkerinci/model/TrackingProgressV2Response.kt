package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/** Respons `GET /api/tracking/v2/progress/{booking_id}`. */
data class TrackingProgressV2Response(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: TrackingProgressV2Data?,
    @SerializedName("errors") val errors: Any?
)

data class TrackingProgressV2Data(
    @SerializedName("booking_id") val bookingId: String,
    @SerializedName("gate") val gate: String?,
    @SerializedName("total_posts") val totalPosts: Int,
    @SerializedName("total_visits") val totalVisits: Int,
    @SerializedName("hikers") val hikers: List<TrackingProgressV2Hiker>,
    @SerializedName("current_pendaki_id") val currentPendakiId: String? = null,
    @SerializedName("member_notes") val memberNotes: List<CheckpointMemberNote>? = null,
    @Transient val fromCache: Boolean = false
)

data class TrackingProgressV2Hiker(
    @SerializedName("pendaki_id") val pendakiId: String,
    @SerializedName("nama") val nama: String,
    @SerializedName("is_leader") val isLeader: Boolean = false,
    @SerializedName("tracking_phase") val trackingPhase: String,
    @SerializedName("next_visit_key") val nextVisitKey: String? = null,
    @SerializedName("ascent_progress") val ascentProgress: PhaseProgress,
    @SerializedName("descent_progress") val descentProgress: PhaseProgress,
    @SerializedName("overall_progress") val overallProgress: PhaseProgress,
    @SerializedName("route_return_completed") val routeReturnCompleted: Boolean = false,
    @SerializedName("visits") val visits: List<TrackingVisitV2>
)

data class PhaseProgress(
    @SerializedName("completed") val completed: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("percentage") val percentage: Int
)

/** Satu kunjungan pada itinerary dua fase. */
data class TrackingVisitV2(
    @SerializedName("visit_key") val visitKey: String,
    @SerializedName("visit_order") val visitOrder: Int,
    @SerializedName("phase") val phase: String,
    @SerializedName("post_id") val postId: Int,
    @SerializedName("nama") val nama: String,
    @SerializedName("urutan") val urutan: Int,
    @SerializedName("is_summit") val isSummit: Boolean = false,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("checkpoint_log_id") val checkpointLogId: Int? = null,
    @SerializedName("checked_at") val checkedAt: String? = null,
    @SerializedName("method") val method: String? = null,
    @SerializedName("estimated_arrival_at") val estimatedArrivalAt: String? = null,
    @SerializedName("travel_minutes_from_previous") val travelMinutesFromPrevious: Int? = null,
    @SerializedName("next_visit_key") val nextVisitKey: String? = null,
    @SerializedName("distance_to_next_m") val distanceToNextM: Int? = null,
    @SerializedName("estimated_time_to_next_minutes") val estimatedTimeToNextMinutes: Int? = null,
    @SerializedName("estimate_is_dummy") val estimateIsDummy: Boolean = false
)


data class CheckpointMemberNote(
    @SerializedName("pendaki_id") val pendakiId: String,
    @SerializedName("post_id") val postId: Int,
    @SerializedName("phase") val phase: String,
    @SerializedName("note") val note: String,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    val key: String get() = "$phase:$postId:$pendakiId"
}

data class SaveCheckpointMemberNoteRequest(
    @SerializedName("pendaki_id") val pendakiId: String,
    @SerializedName("post_id") val postId: Int,
    @SerializedName("phase") val phase: String,
    @SerializedName("note") val note: String
)
