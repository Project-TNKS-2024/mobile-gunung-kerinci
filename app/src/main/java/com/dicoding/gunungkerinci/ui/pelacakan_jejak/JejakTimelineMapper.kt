package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.dicoding.gunungkerinci.util.DateTimeFormat

internal object JejakTimelineMapper {
    private val jakarta = TimeZone.getTimeZone("Asia/Jakarta")

    fun time(iso: String?): String? = DateTimeFormat.apiWibHm(iso)
    fun date(iso: String?): String? = DateTimeFormat.apiWibDate(iso)
    fun millis(iso: String?): Long? = DateTimeFormat.isoToEpochMillis(iso)
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
