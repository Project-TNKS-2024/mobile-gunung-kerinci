package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import com.dicoding.gunungkerinci.model.TrackingPost
import com.dicoding.gunungkerinci.model.TrackingProgressCheckpoint
import com.dicoding.gunungkerinci.model.TrackingProgressHiker
import com.dicoding.gunungkerinci.model.TrackingProgressV2Hiker

internal object TrackingProgressUiMapper {

    /** Proyeksi naik (legacy v1) — dipertahankan untuk konsumen lama. */
    fun toCheckpointUi(
        checkpoints: List<TrackingProgressCheckpoint>,
        posts: List<TrackingPost> = emptyList(),
        hikers: List<TrackingProgressHiker> = emptyList()
    ): List<JejakCheckpointUi> {
        val sortedPosts = posts.sortedBy { it.urutan }
        val postsMap = sortedPosts.associateBy { it.id }
        val previousPosts = sortedPosts.zipWithNext().associate { (previous, current) -> current.id to previous }
        val items = checkpoints.sortedBy { it.urutan }.map { checkpoint ->
            val matchingPost = postsMap[checkpoint.postId]
            val previousPost = previousPosts[checkpoint.postId]
            val members = hikers.filter { hiker ->
                hiker.checkpoints.any { it.postId == checkpoint.postId && it.completed }
            }
            val checkedTime = JejakTimelineMapper.time(checkpoint.checkedAt)
            JejakCheckpointUi(
                times = listOf(JejakTimelineMapper.time(checkpoint.estimatedArrivalAt) ?: "—"),
                name = checkpoint.nama,
                travelMinutesFromPrevious = checkpoint.travelMinutesFromPrevious ?: previousPost?.estimatedTimeMinutes,
                description = JejakTimelineMapper.travelDescription(
                    checkpoint.travelMinutesFromPrevious ?: previousPost?.estimatedTimeMinutes
                ),
                state = if (checkpoint.completed) CheckpointState.Completed else CheckpointState.Upcoming,
                postId = checkpoint.postId,
                order = checkpoint.urutan,
                altitude = matchingPost?.altitude,
                memberNames = members.map { it.nama },
                checkedAtLabel = if (checkpoint.completed) {
                    checkedTime?.let { "Checkpoint pada $it WIB" } ?: "Sudah check-in"
                } else null,
                timelineDate = JejakTimelineMapper.date(
                    if (checkpoint.completed) checkpoint.checkedAt else checkpoint.estimatedArrivalAt
                ),
                estimateIsDummy = checkpoint.estimateIsDummy || previousPost?.infoIsDummy == true
            )
        }
        return JejakTimelineMapper.markNext(items)
    }

    /**
     * Daftar anggota tim + status kehadiran mereka di pos + fase tertentu.
     * Sumber: `GET /api/tracking/v2/progress/{booking_id}` → `hikers[]`.
     */
    fun toMembers(
        hikers: List<TrackingProgressV2Hiker>,
        postId: Int?,
        phase: String?,
        localMarks: List<LocalMemberMark> = emptyList(),
        notes: List<com.dicoding.gunungkerinci.model.CheckpointMemberNote> = emptyList()
    ): List<JejakMemberUi> {
        val marksForPost = if (postId == null) emptyList()
        else localMarks.filter { it.postId == postId && it.phase == phase }
        return hikers.map { hiker ->
            val serverChecked = postId != null && phase != null &&
                hiker.visits.any { it.postId == postId && it.phase == phase && it.completed }
            val localMark = marksForPost.firstOrNull { it.pendakiId == hiker.pendakiId }
            JejakMemberUi(
                pendakiId = hiker.pendakiId,
                name = hiker.nama,
                checked = serverChecked || localMark != null,
                pending = localMark?.pending == true,
                isLeader = hiker.isLeader,
                trackingPhase = hiker.trackingPhase,
                note = notes.firstOrNull { it.pendakiId == hiker.pendakiId && it.postId == postId && it.phase == phase }?.note.orEmpty()
            )
        }
    }

    /**
     * Bukti ketua berada di pos + fase yang diminta: sedang dalam radius pos,
     * atau sudah check-in di pos itu pada fase yang sama.
     */
    fun ketuaAdaDiPosIni(
        postId: Int?,
        phase: String?,
        myPendakiId: String?,
        hikers: List<TrackingProgressV2Hiker>,
        gpsGateState: GpsGateState
    ): Boolean {
        if (postId == null || phase == null) return false
        if (myPendakiId.isNullOrBlank()) return false
        val milikSaya = hikers.firstOrNull { it.pendakiId == myPendakiId } ?: return false
        return milikSaya.visits.any { it.postId == postId && it.phase == phase && it.completed }
    }
}
