package com.dicoding.gunungkerinci

import com.dicoding.gunungkerinci.ui.peringatan_dini.EarlyWarningReducer
import com.dicoding.gunungkerinci.ui.peringatan_dini.EarlyWarningSelector
import com.dicoding.gunungkerinci.ui.peringatan_dini.EarlyWarningUiState
import com.dicoding.gunungkerinci.ui.peringatan_dini.EmergencyWarning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test Case TDD — fitur Peringatan Dini Bahaya (Tabel 19).
 *
 * Satu method = satu baris Tabel 19 (B01–B06). Murni JVM:
 * tanpa emulator, tanpa mock, tanpa Robolectric.
 */
class PeringatanDiniTddTest {

    // ============================================================
    // TDD-B01 — daftar peringatan aktif kosong
    // ============================================================
    @Test
    fun `TDD B01 - daftar peringatan aktif kosong`() {
        val state = EarlyWarningReducer.fromWarnings(emptyList())

        assertNull(state.activeWarning)
        assertFalse(state.popupVisible)
    }

    // ============================================================
    // TDD-B02 — pilih peringatan severity tertinggi
    // ============================================================
    @Test
    fun `TDD B02 - pilih peringatan severity tertinggi`() {
        val hasil = EarlyWarningSelector.select(
            listOf(
                warning(id = 1, severity = "low"),
                warning(id = 2, severity = "critical"),
                warning(id = 3, severity = "medium")
            )
        )

        assertEquals(2, hasil?.id)
    }

    // ============================================================
    // TDD-B03 — severity sama, pilih yang terbaru
    // ============================================================
    @Test
    fun `TDD B03 - severity sama pilih yang terbaru`() {
        val hasil = EarlyWarningSelector.select(
            listOf(
                warning(id = 1, severity = "high", createdAt = "2026-09-18T03:00:00.000000Z"),
                warning(id = 2, severity = "high", createdAt = "2026-09-18T05:00:00.000000Z")
            )
        )

        assertEquals(2, hasil?.id)
    }

    // ============================================================
    // TDD-B04 — popup tidak tampil saat belum ada peringatan
    // ============================================================
    @Test
    fun `TDD B04 - popup tidak tampil saat belum ada peringatan`() {
        val state = EarlyWarningReducer.fromWarnings(emptyList())

        assertFalse(state.popupVisible)
    }

    // ============================================================
    // TDD-B05 — tutup popup, data peringatan tetap ada
    // ============================================================
    @Test
    fun `TDD B05 - tutup popup, data peringatan tetap ada`() {
        val state = EarlyWarningReducer.fromWarnings(listOf(warning(id = 9, severity = "high")))
        assertTrue(state.popupVisible)

        val ditutup = EarlyWarningReducer.dismissPopup(state)

        assertFalse(ditutup.popupVisible)
        assertNotNull(ditutup.activeWarning)
    }

    // ============================================================
    // TDD-B06 — gangguan jaringan tidak membuat aplikasi error
    // ============================================================
    @Test
    fun `TDD B06 - gangguan jaringan tidak membuat aplikasi error`() {
        val state = EarlyWarningReducer.fromError("timeout")

        assertNull(state.activeWarning)
        assertFalse(state.popupVisible)
        assertEquals("timeout", state.errorMessage)
    }

    // ============================================================
    // TDD-B07 — polling tidak membuka ulang popup yang sudah ditutup
    // ============================================================
    @Test
    fun `TDD B07 - polling tidak membuka ulang popup yang sudah ditutup`() {
        val daftar = listOf(warning(id = 5, severity = "high"))
        val ditutup = EarlyWarningReducer.dismissPopup(EarlyWarningReducer.fromWarnings(daftar))

        val (hasil, _) = EarlyWarningReducer.onNewWarnings(
            current = ditutup,
            warnings = daftar,
            lastAutoShownId = 5
        )

        assertFalse(hasil.popupVisible)
        assertNotNull(hasil.activeWarning)
    }

    // ============================================================
    // TDD-B08 — peringatan hilang menutup bubble & popup
    // ============================================================
    @Test
    fun `TDD B08 - peringatan hilang menutup bubble dan popup`() {
        val (hasil, _) = EarlyWarningReducer.onNewWarnings(
            current = EarlyWarningUiState(
                activeWarning = warning(id = 5, severity = "high"),
                popupVisible = true
            ),
            warnings = emptyList(),
            lastAutoShownId = 5
        )

        assertNull(hasil.activeWarning)
        assertFalse(hasil.popupVisible)
    }

    // ==================== helper ====================

    private fun warning(
        id: Int,
        severity: String,
        createdAt: String = "2026-09-18T03:00:00.000000Z"
    ) = EmergencyWarning(
        id = id,
        type = "admin_broadcast",
        title = "Peringatan $id",
        description = "Deskripsi peringatan $id",
        severity = severity,
        createdAt = createdAt
    )
}
