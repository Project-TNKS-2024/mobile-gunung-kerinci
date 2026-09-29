package com.dicoding.gunungkerinci

import com.dicoding.gunungkerinci.data.local.sos.PendingChatDao
import com.dicoding.gunungkerinci.data.local.sos.PendingChatEntity
import com.dicoding.gunungkerinci.data.repository.SosChatRepository
import com.dicoding.gunungkerinci.network.ApiService
import com.dicoding.gunungkerinci.ui.panic_button.DisasterReportValidator
import com.dicoding.gunungkerinci.ui.panic_button.SosQueueRule
import com.dicoding.gunungkerinci.ui.panic_button.SosTriggerValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import java.io.IOException

/**
 * Test Case TDD — fitur Panic Button (Tabel 22).
 *
 * Satu method = satu baris Tabel 22 (C01–C07). Murni JVM:
 * tanpa emulator, tanpa mock, tanpa Robolectric.
 */
class PanicButtonTddTest {

    // ============================================================
    // TDD-C01 — pemicu SOS valid (GPS + severity valid)
    // ============================================================
    @Test
    fun `TDD C01 - pemicu SOS dengan data valid`() {
        val hasil = SosTriggerValidator.error(
            latitude = -1.6975,
            longitude = 101.2635,
            severity = "high",
            message = "Tolong, saya butuh bantuan"
        )

        assertNull(hasil)
    }

    // ============================================================
    // TDD-C02 — koordinat lokasi tidak valid
    // ============================================================
    @Test
    fun `TDD C02 - koordinat lokasi tidak valid`() {
        val kosong = SosTriggerValidator.error(
            latitude = null, longitude = 101.2635, severity = "high", message = null
        )
        val diLuar = SosTriggerValidator.error(
            latitude = 999.0, longitude = 101.2635, severity = "high", message = null
        )

        assertEquals("Lokasi GPS tidak valid", kosong)
        assertEquals("Lokasi GPS tidak valid", diLuar)
    }

    // ============================================================
    // TDD-C03 — tingkat bahaya (severity) tidak valid
    // ============================================================
    @Test
    fun `TDD C03 - tingkat SOS tidak valid`() {
        val hasil = SosTriggerValidator.error(
            latitude = -1.6975, longitude = 101.2635, severity = "extreme", message = null
        )

        assertEquals("Tingkat SOS tidak valid", hasil)
    }

    // ============================================================
    // TDD-C04 — pesan darurat kosong atau melebihi batas karakter
    // ============================================================
    @Test
    fun `TDD C04 - pesan darurat kosong atau melebihi batas`() {
        val kosong = SosTriggerValidator.error(
            latitude = -1.6975, longitude = 101.2635, severity = "high",
            message = "", wajibPesan = true
        )
        val kepanjangan = SosTriggerValidator.error(
            latitude = -1.6975, longitude = 101.2635, severity = "high",
            message = "x".repeat(1001)
        )

        assertEquals("Pesan darurat tidak valid", kosong)
        assertEquals("Pesan darurat tidak valid", kepanjangan)
    }

    // ============================================================
    // TDD-C05 — laporan/lampiran bencana tidak valid
    // ============================================================
    @Test
    fun `TDD C05 - laporan bencana field atau lampiran tidak valid`() {
        val tanpaPotensi = DisasterReportValidator.error(
            potensiBencana = "", deskripsi = "Deskripsi", lokasi = "Lokasi"
        )
        val formatSalah = DisasterReportValidator.error(
            potensiBencana = "Banjir", deskripsi = "Deskripsi", lokasi = "Lokasi",
            lampiranMime = "application/pdf", lampiranBytes = 1_000L
        )
        val terlaluBesar = DisasterReportValidator.error(
            potensiBencana = "Banjir", deskripsi = "Deskripsi", lokasi = "Lokasi",
            lampiranMime = "image/jpeg", lampiranBytes = 6L * 1024 * 1024
        )

        assertEquals("Potensi bencana wajib diisi", tanpaPotensi)
        assertEquals("Lampiran tidak valid", formatSalah)
        assertEquals("Lampiran tidak valid", terlaluBesar)
    }

    // ============================================================
    // TDD-C06 — simpan data SOS secara lokal saat jaringan tidak ada
    // ============================================================
    @Test
    fun `TDD C06 - data SOS disimpan lokal saat jaringan tidak tersedia`() {
        assertTrue(SosQueueRule.simpanOffline(IOException("no network")))
        assertFalse(SosQueueRule.simpanOffline(RuntimeException("server menolak")))
    }

    // ============================================================
    // TDD-C07 — kirim ulang data SOS saat koneksi tersedia
    // ============================================================
    @Test
    fun `TDD C07 - data SOS dikirim ulang saat koneksi tersedia`() {
        assertTrue(SosQueueRule.harusKirimUlang(code = null, error = IOException("timeout")))
        assertTrue(SosQueueRule.harusKirimUlang(code = 500, error = null))
        assertFalse(SosQueueRule.harusKirimUlang(code = 403, error = null))
    }

    // ============================================================
    // TDD-C08 — pesan chat darurat di-queue saat offline (US-12)
    // ============================================================
    @Test
    fun `TDD C08 - pesan chat darurat disimpan ke antrean saat offline`() = runBlocking {
        val dao = FakePendingChatDao()
        // ApiService tidak pernah dipanggil — cukup dibuat lewat Retrofit tanpa server.
        val api = Retrofit.Builder().baseUrl("http://localhost/").build().create(ApiService::class.java)
        val repository = SosChatRepository(api, pendingChatDao = dao)

        val tersimpan = repository.savePendingChat(sosId = 7, type = "text", content = "Tolong, butuh bantuan", imagePath = null)

        assertTrue(tersimpan)
        assertEquals(1, dao.pendingCount(sosId = 7))

        // Setelah berhasil terkirim, baris antrean dibersihkan.
        repository.deletePendingChat(localId = dao.firstLocalId())
        assertEquals(0, dao.pendingCount(sosId = 7))
    }

    private class FakePendingChatDao : PendingChatDao {
        private val chats = mutableListOf<PendingChatEntity>()

        fun firstLocalId(): String = chats.first().localId
        fun pendingCount(sosId: Int): Int = chats.count { it.sosId == sosId && it.syncStatus == "PENDING" }

        override suspend fun upsert(chat: PendingChatEntity) {
            chats.removeAll { it.localId == chat.localId }
            chats += chat
        }

        override suspend fun getPendingSync(pending: String, failed: String) =
            chats.filter { it.syncStatus == pending || it.syncStatus == failed }

        override suspend fun getPendingForSos(sosId: Int) =
            chats.filter { it.sosId == sosId && it.syncStatus == "PENDING" }

        override suspend fun markFailed(localId: String) {
            val index = chats.indexOfFirst { it.localId == localId }
            if (index >= 0) chats[index] = chats[index].copy(retryCount = chats[index].retryCount + 1, syncStatus = "FAILED")
        }

        override suspend fun delete(localId: String) {
            chats.removeAll { it.localId == localId }
        }
    }
}
