package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import com.dicoding.gunungkerinci.model.TrackingPost
import com.dicoding.gunungkerinci.model.TrackingProgressCheckpoint
import com.dicoding.gunungkerinci.model.TrackingProgressHiker
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal object TrackingProgressUiMapper {
    fun toCheckpointUi(
        checkpoints: List<TrackingProgressCheckpoint>,
        posts: List<TrackingPost> = emptyList(),
        hikers: List<TrackingProgressHiker> = emptyList()
    ): List<JejakCheckpointUi> {
        val postsMap = posts.associateBy { it.id }
        return checkpoints.sortedBy { it.urutan }.map { checkpoint ->
            val matchingPost = postsMap[checkpoint.postId]
            val anggotaCheckIn = hikers.filter { hiker ->
                hiker.checkpoints.any { it.postId == checkpoint.postId && it.completed }
            }
            // Waktu check-in paling awal di pos ini (ISO-8601 → urut secara leksikografis).
            val waktuCheckIn = anggotaCheckIn
                .mapNotNull { hiker ->
                    hiker.checkpoints.firstOrNull { it.postId == checkpoint.postId }?.checkedAt
                }
                .minOrNull()

            JejakCheckpointUi(
                times = listOf("Post ${checkpoint.urutan}"),
                name = checkpoint.nama,
                description = buildDescription(checkpoint),
                state = if (anggotaCheckIn.isNotEmpty() || checkpoint.completed) {
                    CheckpointState.Completed
                } else {
                    CheckpointState.Upcoming
                },
                postId = checkpoint.postId,
                order = checkpoint.urutan,
                altitude = matchingPost?.altitude,
                memberNames = anggotaCheckIn.map { it.nama },
                checkedAtLabel = labelWaktuCheckpoint(waktuCheckIn)
            )
        }
    }

    /**
     * Daftar anggota tim + status kehadiran mereka **di pos tertentu**
     * (section "Cek Anggota"). Sumber: `GET /tracking/progress/{booking_id}` → `hikers[]`.
     */
    fun toMembers(hikers: List<TrackingProgressHiker>, postId: Int?): List<JejakMemberUi> =
        hikers.map { hiker ->
            JejakMemberUi(
                pendakiId = hiker.pendakiId,
                name = hiker.nama,
                checked = postId != null &&
                    hiker.checkpoints.any { it.postId == postId && it.completed }
            )
        }

    private fun buildDescription(checkpoint: TrackingProgressCheckpoint): String {
        val methodText = when (checkpoint.method) {
            "qr" -> "QR"
            "gps" -> "GPS"
            "manual" -> "Manual"
            null -> "Belum check-in"
            else -> checkpoint.method
        }
        return if (checkpoint.completed) {
            "Selesai via $methodText"
        } else {
            methodText
        }
    }

    /**
     * Ubah `checked_at` (ISO-8601 UTC dari backend, mis. "2026-09-17T03:12:45.000000Z")
     * menjadi label "checkpoint pada 10:12 wib" (zona Asia/Jakarta).
     *
     * Gagal parse → null; kartu timeline otomatis memakai deskripsi lama sebagai gantinya.
     */
    private fun labelWaktuCheckpoint(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        val bersih = iso.trim().substringBefore('.').removeSuffix("Z")
        // Catatan: SimpleDateFormat.parse() MELEMPAR ParseException untuk input rusak
        // (tidak pernah mengembalikan null), jadi wajib dibungkus runCatching —
        // kalau tidak, `checked_at` cacat dari server akan membuat layar Jejak crash.
        val waktu = runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }.parse(bersih)
        }.getOrNull() ?: return null
        val jam = SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }.format(waktu)
        return "checkpoint pada $jam wib"
    }
}
