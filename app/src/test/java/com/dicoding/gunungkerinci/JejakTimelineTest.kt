package com.dicoding.gunungkerinci

import com.dicoding.gunungkerinci.model.TrackingProgressCheckpoint
import com.dicoding.gunungkerinci.model.TrackingProgressHiker
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.*
import org.junit.Assert.*
import org.junit.Test

class JejakTimelineTest {
    private fun checkpoint(id: Int, completed: Boolean = false, checkedAt: String? = null,
                           arrival: String? = null) = TrackingProgressCheckpoint(
        postId = id, nama = "Pos $id", urutan = id, completed = completed,
        checkedAt = checkedAt, method = if (completed) "qr" else null,
        estimatedArrivalAt = arrival, travelMinutesFromPrevious = 45
    )

    @Test fun `ticket days are inclusive across month and year boundaries`() {
        val days = TicketUiMapper.toDays("2026-12-31", "2027-01-02")
        assertEquals(listOf("2026-12-31", "2027-01-01", "2027-01-02"), days.map { it.isoDate })
        assertEquals(listOf("Hari 1", "Hari 2", "Hari 3"), days.map { it.title })
        assertEquals(1, TicketUiMapper.toDays("2026-10-15", "2026-10-15").size)
        assertTrue(TicketUiMapper.toDays("2026-02-30", "2026-03-01").isEmpty())
        assertTrue(TicketUiMapper.toDays("2026-10-16", "2026-10-15").isEmpty())
    }

    @Test fun `another member checkin supplies avatar but does not complete personal checkpoint`() {
        val mine = listOf(checkpoint(1))
        val member = TrackingProgressHiker("other", "Budi", 1, 1, 100,
            listOf(checkpoint(1, completed = true, checkedAt = "2026-10-15T02:00:00Z")))
        val result = TrackingProgressUiMapper.toCheckpointUi(mine, hikers = listOf(member))
        assertEquals(CheckpointState.Current, result.single().state)
        assertEquals(listOf("Budi"), result.single().memberNames)
        assertNull(result.single().checkedAtLabel)
    }

    @Test fun `arrival clock is separate from actual checkin and next card is unique`() {
        val result = TrackingProgressUiMapper.toCheckpointUi(listOf(
            checkpoint(1, completed = true, checkedAt = "2026-10-15T02:52:00Z", arrival = "2026-10-15T02:30:00Z"),
            checkpoint(2, arrival = "2026-10-15T03:37:00Z"),
            checkpoint(3, arrival = "2026-10-15T04:22:00Z")
        ))
        assertEquals(listOf("09.30"), result[0].times)
        assertEquals("Checkpoint pada 09.52 WIB", result[0].checkedAtLabel)
        assertEquals(listOf(CheckpointState.Completed, CheckpointState.Current, CheckpointState.Upcoming), result.map { it.state })
        assertEquals("45 menit dari titik sebelumnya", result[2].description)
        assertEquals("—", TrackingProgressUiMapper.toCheckpointUi(listOf(checkpoint(1))).single().times.single())
    }

    @Test fun `tab filtering uses actual day for completed and estimated day for future posts`() {
        val result = TrackingProgressUiMapper.toCheckpointUi(listOf(
            checkpoint(1, completed = true, checkedAt = "2026-10-15T16:45:00Z", arrival = "2026-10-15T16:00:00Z"),
            checkpoint(2, arrival = "2026-10-15T17:30:00.000000Z")
        ))
        val days = TicketUiMapper.toDays("2026-10-15", "2026-10-16")
        assertEquals(listOf(1), JejakTimelineMapper.forDay(result, days).map { it.postId })
        val secondDay = days.mapIndexed { index, day -> day.copy(selected = index == 1) }
        assertEquals(listOf(2), JejakTimelineMapper.forDay(result, secondDay).map { it.postId })
        assertEquals("00.30", result[1].times.single())
        assertEquals("00.30", JejakTimelineMapper.time("2026-10-16T00:30:00+07:00"))
        assertNull(JejakTimelineMapper.time("invalid"))
    }

    @Test fun `all completed means no next checkpoint`() {
        val result = TrackingProgressUiMapper.toCheckpointUi(listOf(checkpoint(1, true), checkpoint(2, true)))
        assertTrue(result.all { it.state == CheckpointState.Completed })
    }
}
