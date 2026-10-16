package com.dicoding.gunungkerinci

import com.dicoding.gunungkerinci.model.MyTiketItem
import com.dicoding.gunungkerinci.model.OverstayPendaki
import com.dicoding.gunungkerinci.model.OverstaySummary
import com.dicoding.gunungkerinci.model.OverstayTeam
import com.dicoding.gunungkerinci.notif.TicketEventType
import com.dicoding.gunungkerinci.notif.TicketNotificationSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketNotificationSelectionTest {

    private fun booking(
        id: String,
        idUser: Int? = 1,
        status: Int? = 6,
        exit: String? = "2026-10-19",
        hikeState: String = "mendaki",
        chargeStatus: String? = null,
        amount: Int? = null
    ) = MyTiketItem(
        id = id,
        idUser = idUser,
        statusBooking = status,
        tanggalKeluar = exit,
        overstay = OverstaySummary(
            serverNow = "2026-10-19T14:00:00+07:00",
            pendakis = listOf(
                OverstayPendaki(id = "p1", hikeState = hikeState, chargeStatus = chargeStatus, amount = amount)
            ),
            team = OverstayTeam()
        )
    )

    @Test
    fun `pengingat saat tanggal keluar hari ini dan sudah lewat jam pengingat`() {
        val events = TicketNotificationSelection.selectEvents(
            listOf(booking("b1", exit = "2026-10-19")),
            1, "2026-10-19T14:00:00+07:00", "07:00", "u1", emptySet()
        )
        assertEquals(1, events.size)
        assertEquals(TicketEventType.REMINDER, events[0].type)
        assertEquals("b1", events[0].bookingId)
    }

    @Test
    fun `tidak ada pengingat sebelum jam pengingat`() {
        val events = TicketNotificationSelection.selectEvents(
            listOf(booking("b1", exit = "2026-10-19")),
            1, "2026-10-19T06:59:00+07:00", "07:00", "u1", emptySet()
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `overdue saat tanggal keluar sebelum hari ini`() {
        val events = TicketNotificationSelection.selectEvents(
            listOf(booking("b1", exit = "2026-10-18")),
            1, "2026-10-19T14:00:00+07:00", "07:00", "u1", emptySet()
        )
        assertEquals(1, events.size)
        assertEquals(TicketEventType.OVERDUE, events[0].type)
    }

    @Test
    fun `dedup melewati kejadian yang sudah ditampilkan`() {
        val events = TicketNotificationSelection.selectEvents(
            listOf(booking("b1", exit = "2026-10-19")),
            1, "2026-10-19T14:00:00+07:00", "07:00", "u1",
            setOf("u1:reminder:b1:2026-10-19")
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `hanya pemilik yang menerima notifikasi`() {
        val events = TicketNotificationSelection.selectEvents(
            listOf(booking("b1", idUser = 2, exit = "2026-10-19")),
            1, "2026-10-19T14:00:00+07:00", "07:00", "u1", emptySet()
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `tidak ada pendaki aktif berarti tidak ada notifikasi`() {
        val events = TicketNotificationSelection.selectEvents(
            listOf(booking("b1", exit = "2026-10-18", hikeState = "turun")),
            1, "2026-10-19T14:00:00+07:00", "07:00", "u1", emptySet()
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `overdue tetap dikirim walau tarif belum diketahui`() {
        val events = TicketNotificationSelection.selectEvents(
            listOf(booking("b1", exit = "2026-10-18", hikeState = "mendaki", chargeStatus = "needs_review", amount = null)),
            1, "2026-10-19T14:00:00+07:00", "07:00", "u1", emptySet()
        )
        assertEquals(1, events.size)
        assertEquals(TicketEventType.OVERDUE, events[0].type)
    }

    @Test
    fun `server now null berarti tidak ada notifikasi`() {
        val events = TicketNotificationSelection.selectEvents(
            listOf(booking("b1")), 1, null, "07:00", "u1", emptySet()
        )
        assertTrue(events.isEmpty())
    }
}
