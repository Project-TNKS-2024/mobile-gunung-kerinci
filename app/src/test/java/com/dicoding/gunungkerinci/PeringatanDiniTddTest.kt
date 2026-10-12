package com.dicoding.gunungkerinci

import com.dicoding.gunungkerinci.ui.peringatan_dini.PeringatanDiniReducer
import com.dicoding.gunungkerinci.ui.peringatan_dini.PeringatanDiniSelector
import com.dicoding.gunungkerinci.ui.peringatan_dini.PeringatanDiniUiState
import com.dicoding.gunungkerinci.ui.peringatan_dini.PeringatanDini
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeringatanDiniTddTest {

    // TDD-B01 — daftar peringatan aktif kosong
    @Test
    fun `TDD-B01 - daftar peringatan aktif kosong`() {
        val state = PeringatanDiniReducer.fromWarnings(emptyList())

        assertNull(state.activeWarning)
        assertFalse(state.popupVisible)
    }

    // TDD-B02 — pilih peringatan severity tertinggi
    @Test
    fun `TDD-B02 - pilih peringatan severity tertinggi`() {
        val hasil = PeringatanDiniSelector.select(
            listOf(
                warning(id = 1, severity = "low"),
                warning(id = 2, severity = "critical"),
                warning(id = 3, severity = "medium")
            )
        )

        assertEquals(2, hasil?.id)
    }

    // TDD-B03 — severity sama, pilih yang terbaru
    @Test
    fun `TDD-B03 - severity sama pilih yang terbaru`() {
        val hasil = PeringatanDiniSelector.select(
            listOf(
                warning(id = 1, severity = "high", createdAt = "2026-09-18T03:00:00.000000Z"),
                warning(id = 2, severity = "high", createdAt = "2026-09-18T05:00:00.000000Z")
            )
        )

        assertEquals(2, hasil?.id)
    }

    // TDD-B04 — tutup popup, data peringatan tetap ada
    @Test
    fun `TDD-B04 - tutup popup, data peringatan tetap ada`() {
        val state = PeringatanDiniReducer.fromWarnings(listOf(warning(id = 9, severity = "high")))
        assertTrue(state.popupVisible)

        val ditutup = PeringatanDiniReducer.dismissPopup(state)

        assertFalse(ditutup.popupVisible)
        assertNotNull(ditutup.activeWarning)
    }

    // TDD-B05 — gangguan jaringan tidak menghapus peringatan aktif
    @Test
    fun `TDD-B05 - gangguan jaringan tidak menghapus peringatan aktif`() {
        val sebelumnya = PeringatanDiniReducer.fromWarnings(listOf(warning(id = 5, severity = "high")))
        val state = PeringatanDiniReducer.fromError(sebelumnya, "timeout", isTerminal = false)

        // Blip jaringan tidak boleh menyembunyikan peringatan yang masih aktif.
        assertNotNull(state.activeWarning)
        assertTrue(state.popupVisible)
        assertEquals("timeout", state.errorMessage)
    }

    // TDD-B06 — polling tidak membuka ulang popup yang sudah ditutup
    @Test
    fun `TDD-B06 - polling tidak membuka ulang popup yang sudah ditutup`() {
        val daftar = listOf(warning(id = 5, severity = "high"))
        val ditutup = PeringatanDiniReducer.dismissPopup(PeringatanDiniReducer.fromWarnings(daftar))

        val (hasil, _) = PeringatanDiniReducer.onNewWarnings(
            current = ditutup,
            warnings = daftar,
            lastAutoShownId = 5
        )

        assertFalse(hasil.popupVisible)
        assertNotNull(hasil.activeWarning)
    }

    // TDD-B07 — peringatan hilang menutup bubble & popup
    @Test
    fun `TDD-B07 - peringatan hilang menutup bubble dan popup`() {
        val (hasil, _) = PeringatanDiniReducer.onNewWarnings(
            current = PeringatanDiniUiState(
                activeWarning = warning(id = 5, severity = "high"),
                popupVisible = true
            ),
            warnings = emptyList(),
            lastAutoShownId = 5
        )

        assertNull(hasil.activeWarning)
        assertFalse(hasil.popupVisible)
    }

    // TDD-B08 — peringatan dari cache tetap tampil & ditandai data usang
    @Test
    fun `TDD-B08 - peringatan dari cache tetap tampil dan ditandai usang`() {
        val state = PeringatanDiniReducer.fromCache(
            items = listOf(warning(id = 5, severity = "high")),
            lastUpdatedAt = "14.30"
        )

        // Offline: peringatan terakhir tetap ditampilkan (fitur tidak "senyap") …
        assertEquals(5, state.activeWarning?.id)
        assertEquals("14.30", state.lastUpdatedAt)
        // … dan ditandai sebagai data usang (belum tersinkron).
        assertTrue(state.isStale)
    }

    // helper

    private fun warning(
        id: Int,
        severity: String,
        createdAt: String = "2026-09-18T03:00:00.000000Z"
    ) = PeringatanDini(
        id = id,
        type = "admin_broadcast",
        title = "Peringatan $id",
        description = "Deskripsi peringatan $id",
        severity = severity,
        createdAt = createdAt
    )
}
