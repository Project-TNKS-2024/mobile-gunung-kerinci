package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object JejakTimelineMapper {
    private val jakarta = TimeZone.getTimeZone("Asia/Jakarta")

    private fun parse(iso: String?): Date? {
        if (iso.isNullOrBlank()) return null
        val normalized = iso.trim().replace(Regex("\\.\\d+(?=Z|[+-]\\d{2}:?\\d{2}$)"), "")
        val position = ParsePosition(0)
        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            isLenient = false
        }.parse(normalized, position)
        return parsed?.takeIf { position.index == normalized.length }
    }

    private fun format(iso: String?, pattern: String): String? = parse(iso)?.let {
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = jakarta }.format(it)
    }

    fun time(iso: String?): String? = format(iso, "HH.mm")
    fun date(iso: String?): String? = format(iso, "yyyy-MM-dd")
    fun timeAt(millis: Long): String = SimpleDateFormat("HH.mm", Locale.US).apply {
        timeZone = jakarta
    }.format(Date(millis))

    fun dateAt(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = jakarta
    }.format(Date(millis))

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = jakarta
    }.format(Date())

    fun travelDescription(minutes: Int?): String = minutes?.takeIf { it >= 0 }
        ?.let { "$it menit dari titik sebelumnya" } ?: "Estimasi belum tersedia"

    fun markNext(checkpoints: List<JejakCheckpointUi>): List<JejakCheckpointUi> {
        val nextId = checkpoints.firstOrNull { it.state != CheckpointState.Completed }?.postId
        return checkpoints.map {
            when {
                it.state == CheckpointState.Completed -> it
                nextId != null && it.postId == nextId -> it.copy(
                    state = CheckpointState.Current,
                    description = "Checkpoint selanjutnya"
                )
                else -> it.copy(state = CheckpointState.Upcoming)
            }
        }
    }

    /** Undated posts stay on today's tab (or day 1 outside the ticket period). */
    fun forDay(checkpoints: List<JejakCheckpointUi>, days: List<JejakDayUi>): List<JejakCheckpointUi> {
        val selected = days.firstOrNull { it.selected } ?: days.firstOrNull() ?: return checkpoints
        val undatedDay = days.firstOrNull { it.isoDate == today() } ?: days.first()
        return checkpoints.filter {
            it.timelineDate == selected.isoDate ||
                (it.timelineDate == null && selected.isoDate == undatedDay.isoDate)
        }
    }
}
