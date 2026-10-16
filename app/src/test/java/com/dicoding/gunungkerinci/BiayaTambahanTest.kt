package com.dicoding.gunungkerinci

import com.dicoding.gunungkerinci.Ticket.Barcode.BiayaTambahan
import com.dicoding.gunungkerinci.model.OverstayPendaki
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BiayaTambahanTest {

    @Test
    fun `label jenis hari`() {
        assertEquals("Hari biasa", BiayaTambahan.dayTypeLabel("weekday"))
        assertEquals("Akhir pekan", BiayaTambahan.dayTypeLabel("weekend"))
        assertEquals("Hari libur", BiayaTambahan.dayTypeLabel("holiday"))
        assertEquals("-", BiayaTambahan.dayTypeLabel(null))
    }

    @Test
    fun `label kategori`() {
        assertEquals("WNI", BiayaTambahan.kategoriLabel("wni"))
        assertEquals("WNA", BiayaTambahan.kategoriLabel("wna"))
    }

    @Test
    fun `unpaid final hanya menjumlah item final dan unpaid`() {
        val pendakis = listOf(
            OverstayPendaki(id = "p1", chargeStatus = "final", paymentState = "unpaid", amount = 50_000),
            OverstayPendaki(id = "p2", chargeStatus = "final", paymentState = "paid", amount = 30_000),
            OverstayPendaki(id = "p3", chargeStatus = "estimate", paymentState = "none", amount = 20_000)
        )
        assertEquals(50_000, BiayaTambahan.unpaidFinalTotal(pendakis))
    }

    @Test
    fun `unpaid final null ketika ada needs review`() {
        val pendakis = listOf(
            OverstayPendaki(id = "p1", chargeStatus = "final", paymentState = "unpaid", amount = 50_000),
            OverstayPendaki(id = "p2", chargeStatus = "needs_review", amount = null)
        )
        assertNull(BiayaTambahan.unpaidFinalTotal(pendakis))
    }

    @Test
    fun `tidak ada pendaki berarti total nol`() {
        assertEquals(0, BiayaTambahan.unpaidFinalTotal(emptyList()))
        assertEquals(0, BiayaTambahan.unpaidFinalTotal(null))
    }
}
