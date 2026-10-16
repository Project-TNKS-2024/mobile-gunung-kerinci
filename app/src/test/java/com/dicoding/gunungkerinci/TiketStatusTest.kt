package com.dicoding.gunungkerinci

import com.dicoding.gunungkerinci.Ticket.TiketStatus
import com.dicoding.gunungkerinci.model.MyTiketItem
import com.dicoding.gunungkerinci.model.MyTiketBiodata
import com.dicoding.gunungkerinci.model.MyTiketPendaki
import com.dicoding.gunungkerinci.model.OverstayPendaki
import com.dicoding.gunungkerinci.model.OverstaySummary
import com.dicoding.gunungkerinci.model.OverstayTeam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TiketStatusTest {

    @Test
    fun `status 0 sampai 3 adalah draft`() {
        (0..3).forEach { assertTrue(TiketStatus.isDraft(it)) }
        assertFalse(TiketStatus.isDraft(null))
    }

    @Test
    fun `status 4 sampai 8 adalah tiket`() {
        (4..8).forEach { assertTrue(TiketStatus.isTicket(it)) }
        assertFalse(TiketStatus.isTicket(3))
        assertFalse(TiketStatus.isTicket(null))
    }

    @Test
    fun `overdue dan unpaid tampil bersamaan`() {
        val os = OverstaySummary(
            team = OverstayTeam(overdueActiveCount = 2, unpaidFinalCount = 1),
            pendakis = emptyList()
        )
        assertEquals(
            listOf("Lewat jadwal turun", "Biaya tambahan belum dibayar"),
            TiketStatus.overstayLabels(os)
        )
    }

    @Test
    fun `needs review menghasilkan label menunggu pemeriksaan`() {
        val os = OverstaySummary(
            team = OverstayTeam(),
            pendakis = listOf(OverstayPendaki(id = "p1", chargeStatus = "needs_review"))
        )
        assertEquals(listOf("Biaya menunggu pemeriksaan petugas"), TiketStatus.overstayLabels(os))
    }

    @Test
    fun `tanpa overstay tidak ada label`() {
        assertTrue(TiketStatus.overstayLabels(null).isEmpty())
        assertTrue(TiketStatus.overstayLabels(OverstaySummary(team = OverstayTeam(), pendakis = emptyList())).isEmpty())
    }

    @Test
    fun `nama ketua dari pendaki pertama`() {
        val item = MyTiketItem(
            id = "b1",
            pendakis = listOf(
                MyTiketPendaki(id = "p1", biodata = MyTiketBiodata(firstName = "Andi", lastName = "Pratama"))
            )
        )
        assertEquals("Andi Pratama", TiketStatus.ketuaName(item))
    }

    @Test
    fun `label jumlah pendaki campur wni dan wna`() {
        val item = MyTiketItem(id = "b1", totalPendakiWni = 2, totalPendakiWna = 1)
        assertEquals("2 WNI + 1 WNA", TiketStatus.pendakiLabel(item))
    }
}
