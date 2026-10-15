package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import com.dicoding.gunungkerinci.model.TrackingPost
import com.dicoding.gunungkerinci.model.TrackingProgressCheckpoint
import com.dicoding.gunungkerinci.model.TrackingProgressHiker

internal object TrackingProgressUiMapper {
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
     * Daftar anggota tim + status kehadiran mereka **di pos tertentu**
     * (section "Cek Anggota"). Sumber: `GET /tracking/progress/{booking_id}` → `hikers[]`.
     */
    fun toMembers(
        hikers: List<TrackingProgressHiker>,
        postId: Int?,
        localMarks: List<LocalMemberMark> = emptyList()
    ): List<JejakMemberUi> {
        val marksForPost = if (postId == null) emptyList()
        else localMarks.filter { it.postId == postId }
        return hikers.map { hiker ->
            val serverChecked = postId != null &&
                hiker.checkpoints.any { it.postId == postId && it.completed }
            val localMark = marksForPost.firstOrNull { it.pendakiId == hiker.pendakiId }
            JejakMemberUi(
                pendakiId = hiker.pendakiId,
                name = hiker.nama,
                checked = serverChecked || localMark != null,
                // Hanya "menunggu kirim" kalau catatan lokal ada TAPI server belum mencatatnya.
                pending = localMark?.pending == true && !serverChecked,
                isLeader = hiker.isLeader
            )
        }
    }

    /**
     * Syarat switch kehadiran anggota boleh dipakai: ketua sudah terbukti berada di pos itu,
     * yaitu sedang dalam radius pos tersebut atau sudah check-in di pos itu. Tanpa bukti itu,
     * kehadiran tidak boleh dicatat dari jauh.
     */
    fun ketuaAdaDiPosIni(
        postId: Int?,
        myPendakiId: String?,
        hikers: List<TrackingProgressHiker>,
        gpsGateState: GpsGateState
    ): Boolean {
        if (postId == null) return false
        // Sedang dalam radius pos yang sedang dibuka.
        if (gpsGateState is GpsGateState.DalamRadius && gpsGateState.postId == postId) return true
        // Atau sudah pernah check-in di pos itu.
        if (myPendakiId.isNullOrBlank()) return false
        val milikSaya = hikers.firstOrNull { it.pendakiId == myPendakiId } ?: return false
        return milikSaya.checkpoints.any { it.postId == postId && it.completed }
    }

}
