package com.dicoding.gunungkerinci

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.dicoding.gunungkerinci.data.repository.PeringatanDiniApiException
import com.dicoding.gunungkerinci.pref.UserPreference
import com.dicoding.gunungkerinci.ui.peringatan_dini.*
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Enam skenario skripsi TDD-B01 sampai TDD-B06, termasuk layanan alarm Android. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@LooperMode(LooperMode.Mode.PAUSED)
class PeringatanDiniTddTest {
    private lateinit var context: Context
    private val token = "tdd-warning-session"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        UserPreference(context).saveToken(token)
        WarningReceiptStore(context).clear()
        WarningMonitoring.enable(context)
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(true)
        WarningNotifications.createChannels(context)
    }

    @After
    fun cleanup() {
        WarningReceiptStore(context).clear()
        WarningMonitoring.disable(context)
        WarningMonitoring.setRinging(false)
        WarningMonitoring.setRunning(false)
        WarningMonitoring.setAlert(null)
        UserPreference(context).clearToken()
    }

    @Test
    fun `TDD-B01 - daftar kosong dan pencabutan membersihkan peringatan serta popup`() {
        val kosong = PeringatanDiniReducer.fromWarnings(emptyList())
        assertNull(kosong.activeWarning)
        assertFalse(kosong.popupVisible)
        val sebelumnya = PeringatanDiniReducer.fromWarnings(listOf(warning(5, "high")))
        val (hasil, shownId) = PeringatanDiniReducer.onNewWarnings(sebelumnya, emptyList(), 5)
        assertNull(hasil.activeWarning)
        assertFalse(hasil.popupVisible)
        assertNull(hasil.errorMessage)
        assertNull(shownId)
    }

    @Test
    fun `TDD-B02 - prioritas tingkat bahaya didahulukan lalu waktu terbaru`() {
        val lama = warning(1, "high", "2026-09-18T03:00:00.000000Z")
        val baru = warning(2, "high", "2026-09-18T05:00:00.000000Z")
        assertEquals(3, PeringatanDiniSelector.select(listOf(lama, baru, warning(3, "critical")))?.id)
        assertEquals(2, PeringatanDiniSelector.select(listOf(lama, baru))?.id)
    }

    @Test
    fun `TDD-B03 - popup yang ditutup tidak berulang dan dapat dibuka kembali`() {
        val warning = warning(5, "high")
        val awal = PeringatanDiniReducer.fromWarnings(listOf(warning))
        assertTrue(awal.popupVisible)
        val ditutup = PeringatanDiniReducer.dismissPopup(awal)
        assertFalse(ditutup.popupVisible)
        assertEquals(warning, ditutup.activeWarning)
        val (polling, _) = PeringatanDiniReducer.onNewWarnings(ditutup, listOf(warning), 5)
        assertFalse(polling.popupVisible)
        assertEquals(warning, polling.activeWarning)
        val dibuka = PeringatanDiniReducer.showPopup(polling)
        assertTrue(dibuka.popupVisible)
        assertEquals(warning, dibuka.activeWarning)
    }

    @Test
    fun `TDD-B04 - cache dipertahankan dan ditandai usang saat pembaruan gagal sementara`() {
        val cache = PeringatanDiniReducer.fromCache(listOf(warning(5, "high")), "14.30")
        val gagal = PeringatanDiniReducer.fromError(cache, "timeout", isTerminal = false)
        assertEquals(5, gagal.activeWarning?.id)
        assertEquals("14.30", gagal.lastUpdatedAt)
        assertTrue(gagal.isStale)
        assertNull(com.dicoding.gunungkerinci.util.DateTimeFormat.apiWibHm("2026-10-20T13:20:00.000000Z"))
        assertEquals("20.20", com.dicoding.gunungkerinci.util.DateTimeFormat.apiWibHm("2026-10-20T20:20:00+07:00"))
        assertEquals("timeout", gagal.errorMessage)
        val (kosong, _) = PeringatanDiniReducer.onNewWarnings(gagal, emptyList(), 5)
        assertNull(kosong.activeWarning)
        assertNull(kosong.errorMessage)
    }

    @Test
    fun `TDD-B05 - sesi atau pendakian tidak berlaku membersihkan peringatan dan menghentikan pemantauan`() {
        for (code in listOf(401, 403)) {
            WarningReceiptStore(context).clear()
            val awal = PeringatanDiniReducer.fromCache(listOf(warning(5, "high")), "14.30")
            val ditolak = PeringatanDiniReducer.fromError(awal, "Tidak dapat diakses ($code)", isTerminal = true)
            assertNull(ditolak.activeWarning)
            assertFalse(ditolak.isStale)
            val controller = Robolectric.buildService(WarningMonitoringService::class.java).create()
            try {
                val service = controller.get()
                service.acceptWarnings(token, listOf(warning(5, "high")))
                assertTrue(WarningMonitoring.ringing.value)
                service.warningSource = { Result.failure(PeringatanDiniApiException(code, "Sesi tidak berlaku")) }
                service.onStartCommand(Intent(context, WarningMonitoringService::class.java), 0, 1)
                assertTrue(shadowOf(service).isStoppedBySelf)
            } finally {
                controller.destroy()
            }
            assertFalse(WarningMonitoring.running.value)
            assertFalse(WarningMonitoring.ringing.value)
            assertNull(WarningMonitoring.alert.value)
        }
    }

    @Test
    fun `TDD-B06 - alarm baru dapat dihentikan tidak berulang dan dicabut bersama notifikasi`() {
        val controller = Robolectric.buildService(WarningMonitoringService::class.java).create()
        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            val service = controller.get()
            service.warningSource = { awaitCancellation() }
            service.acceptWarnings(token, listOf(warning(5, "high")))
            assertTrue(WarningMonitoring.ringing.value)
            assertNotNull(shadowOf(manager).getNotification(WarningNotifications.ALERT_ID))
            service.onStartCommand(Intent(context, WarningMonitoringService::class.java).setAction(WarningMonitoringService.STOP_ALARM), 0, 1)
            assertFalse(WarningMonitoring.ringing.value)
            assertEquals(5, WarningMonitoring.alert.value?.id)
            assertNotNull(shadowOf(manager).getNotification(WarningNotifications.ALERT_ID))
            service.acceptWarnings(token, listOf(warning(5, "high")))
            assertFalse(WarningMonitoring.ringing.value)
            service.acceptWarnings(token, listOf(warning(5, "high"), warning(6, "critical")))
            assertTrue(WarningMonitoring.ringing.value)
            assertEquals(6, WarningMonitoring.alert.value?.id)
            service.acceptWarnings(token, emptyList())
            assertFalse(WarningMonitoring.ringing.value)
            assertNull(WarningMonitoring.alert.value)
            assertNull(shadowOf(manager).getNotification(WarningNotifications.ALERT_ID))
        } finally {
            controller.destroy()
        }
    }

    private fun warning(id: Int, severity: String, createdAt: String = "2026-09-18T03:00:00.000000Z") =
        PeringatanDini(id = id, type = "admin_broadcast", title = "Peringatan $id",
            description = "Deskripsi peringatan $id", severity = severity, createdAt = createdAt)
}
