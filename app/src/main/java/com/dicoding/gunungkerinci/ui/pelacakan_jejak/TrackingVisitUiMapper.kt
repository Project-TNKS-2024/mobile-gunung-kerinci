package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import com.dicoding.gunungkerinci.model.TrackingProgressV2Hiker
import com.dicoding.gunungkerinci.model.TrackingVisitV2
import com.dicoding.gunungkerinci.model.TrackingPost
import com.dicoding.gunungkerinci.model.PhaseProgress
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogEntity
import com.dicoding.gunungkerinci.data.local.sync.SyncStatus
import com.dicoding.gunungkerinci.data.repository.isoWib
import kotlin.math.roundToInt

/**
 * Memetakan itinerary v2 (naik + turun) menjadi daftar kartu timeline.
 * Fase aktif menentukan kartu "selanjutnya" (Current); kunjungan turun sebelum
 * puncak tetap terlihat sebagai belum aktif (Upcoming).
 */
internal object TrackingVisitUiMapper {

    fun toCheckpointUi(
        visits: List<TrackingVisitV2>,
        nextVisitKey: String?,
        hikers: List<TrackingProgressV2Hiker>,
        pendingVisitKeys: Set<String> = emptySet()
    ): List<JejakCheckpointUi> {
        val items = visits.map { visit ->
            val members = hikers.filter { hiker ->
                hiker.visits.any { it.visitKey == visit.visitKey && it.completed }
            }.map { it.nama }
            val checkedTime = JejakTimelineMapper.time(visit.checkedAt)

            JejakCheckpointUi(
                times = listOf(JejakTimelineMapper.time(visit.estimatedArrivalAt) ?: "—"),
                name = visit.nama,
                description = JejakTimelineMapper.travelDescription(visit.travelMinutesFromPrevious),
                state = if (visit.completed) CheckpointState.Completed else CheckpointState.Upcoming,
                postId = visit.postId,
                order = visit.urutan,
                memberNames = members,
                checkedAtLabel = if (visit.visitKey in pendingVisitKeys) {
                    "Menunggu sinkronisasi" + (checkedTime?.let { " · $it WIB" } ?: "")
                } else if (visit.completed) {
                    checkedTime?.let { "Checkpoint pada $it WIB" } ?: "Sudah check-in"
                } else null,
                timelineDate = JejakTimelineMapper.date(
                    if (visit.completed) visit.checkedAt else visit.estimatedArrivalAt
                ),
                estimateIsDummy = visit.estimateIsDummy,
                visitKey = visit.visitKey,
                phase = visit.phase,
                isSummit = visit.isSummit,
                distanceToNextM = visit.distanceToNextM,
                estimatedTimeToNextMinutes = visit.estimatedTimeToNextMinutes,
                travelMinutesFromPrevious = visit.travelMinutesFromPrevious
            )
        }

        return markNext(items, nextVisitKey)
    }

    /** Bentuk yang sama dengan itinerary server, hanya dari konfigurasi backend yang dicache. */
    fun fromPosts(posts: List<TrackingPost>): List<TrackingVisitV2> {
        val sorted = posts.sortedBy { it.urutan }
        if (sorted.isEmpty() || sorted.count { it.isSummit } != 1 || !sorted.last().isSummit ||
            sorted.map { it.urutan }.distinct().size != sorted.size) return emptyList()
        val path = sorted.map { "naik" to it } + sorted.dropLast(1).reversed().map { "turun" to it }
        return path.mapIndexed { index, (phase, post) ->
            val postIndex = sorted.indexOfFirst { it.id == post.id }
            val outgoingDown = phase == "turun" || post.isSummit
            val next = path.getOrNull(index + 1)
            val distance = if (next == null) null else if (outgoingDown) {
                sorted.getOrNull(postIndex - 1)?.distanceToNextM
            } else post.distanceToNextM
            val minutes = if (next == null) null else if (outgoingDown) {
                post.estimatedDescentTimeMinutes
            } else post.estimatedTimeMinutes
            TrackingVisitV2(
                visitKey = "$phase:${post.id}", visitOrder = index + 1, phase = phase,
                postId = post.id, nama = post.nama, urutan = post.urutan,
                isSummit = post.isSummit, completed = false,
                nextVisitKey = next?.let { "${it.first}:${it.second.id}" },
                distanceToNextM = distance, estimatedTimeToNextMinutes = minutes,
                estimateIsDummy = post.infoIsDummy
            )
        }
    }

    /** Overlay data lokal yang belum ditolak; identitas selalu pos + fase. */
    fun project(hiker: TrackingProgressV2Hiker, logs: List<CheckpointLogEntity>): TrackingProgressV2Hiker {
        val local = logs.filter {
            it.pendakiId == hiker.pendakiId && it.syncStatus != SyncStatus.REJECTED.name && it.retryCount < 99
        }.associateBy { "${it.phase}:${it.postId}" }
        var arrival: Long? = null
        var previous: TrackingVisitV2? = null
        val visits = hiker.visits.sortedBy { it.visitOrder }.map { visit ->
            val log = local[visit.visitKey]
            val current = if (!visit.completed && log != null) visit.copy(
                completed = true, checkedAt = isoWib(log.checkedAt), method = log.method,
                checkpointLogId = log.serverId
            ) else visit
            val incoming = previous?.estimatedTimeToNextMinutes?.takeIf { it >= 0 }
            arrival = if (arrival != null && incoming != null) arrival + incoming * 60_000L else null
            val result = current.copy(
                estimatedArrivalAt = arrival?.let(::isoWib), travelMinutesFromPrevious = incoming
            )
            arrival = JejakTimelineMapper.millis(current.checkedAt) ?: arrival
            previous = current
            result
        }
        val phase = if (visits.any { it.isSummit && it.completed }) "turun" else "naik"
        fun progress(items: List<TrackingVisitV2>): PhaseProgress {
            val done = items.count { it.completed }
            return PhaseProgress(done, items.size, if (items.isEmpty()) 0 else (done * 100.0 / items.size).roundToInt())
        }
        return hiker.copy(
            visits = visits, trackingPhase = phase,
            nextVisitKey = visits.firstOrNull { it.phase == phase && !it.completed }?.visitKey,
            ascentProgress = progress(visits.filter { it.phase == "naik" }),
            descentProgress = progress(visits.filter { it.phase == "turun" }),
            overallProgress = progress(visits),
            routeReturnCompleted = visits.lastOrNull()?.let { it.phase == "turun" && it.completed } == true
        )
    }

    /**
     * Tandai tepat satu kunjungan "selanjutnya" pada fase aktif.
     * Null [nextVisitKey] = tidak ada kunjungan berikutnya (fase aktif selesai).
     */
    private fun markNext(items: List<JejakCheckpointUi>, nextVisitKey: String?): List<JejakCheckpointUi> =
        items.map {
            when {
                it.state == CheckpointState.Completed -> it
                nextVisitKey != null && it.visitKey == nextVisitKey -> it.copy(
                    state = CheckpointState.Current,
                    description = "Checkpoint selanjutnya"
                )
                else -> it.copy(state = CheckpointState.Upcoming)
            }
        }
}
