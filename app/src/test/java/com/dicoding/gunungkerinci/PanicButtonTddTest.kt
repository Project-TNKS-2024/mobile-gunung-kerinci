package com.dicoding.gunungkerinci

import com.dicoding.gunungkerinci.data.local.sos.PendingChatDao
import com.dicoding.gunungkerinci.data.local.sos.PendingChatEntity
import com.dicoding.gunungkerinci.data.repository.SosChatRepository
import com.dicoding.gunungkerinci.data.sync.SosQueueRule
import com.dicoding.gunungkerinci.network.ApiService
import com.dicoding.gunungkerinci.ui.panic_button.DisasterReportValidator
import com.dicoding.gunungkerinci.ui.panic_button.SosTriggerValidator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import java.io.IOException

/** Enam skenario skripsi TDD-C01 sampai TDD-C06; antrean lokal bukan bukti penerimaan server. */
class PanicButtonTddTest {

    @Test
    fun `TDD-C01 - lokasi dan tingkat bahaya SOS harus valid`() {
        for (severity in listOf("low", "medium", "high")) {
            assertNull(SosTriggerValidator.error(-1.6975, 101.2635, severity, null))
        }
        for ((lat, lon) in listOf(null to 101.2635, -1.6975 to null, 999.0 to 101.2635, -1.6975 to 200.0, Double.NaN to 101.2635)) {
            assertEquals("Lokasi GPS tidak valid", SosTriggerValidator.error(lat, lon, "high", null))
        }
        assertEquals("Tingkat SOS tidak valid", SosTriggerValidator.error(-1.6975, 101.2635, "extreme", null))
    }

    @Test
    fun `TDD-C02 - pesan tambahan SOS opsional dengan batas seribu karakter`() {
        for (message in listOf(null, "", "Tolong, saya butuh bantuan", "x".repeat(1000))) {
            assertNull(SosTriggerValidator.error(-1.6975, 101.2635, "high", message))
        }
        assertEquals("Pesan darurat tidak valid", SosTriggerValidator.error(-1.6975, 101.2635, "high", "x".repeat(1001)))
    }

    @Test
    fun `TDD-C03 - laporan memerlukan isian wajib sedangkan lampiran opsional harus sesuai`() {
        assertNull(DisasterReportValidator.error("Banjir", "Deskripsi", "Pos 1"))
        assertEquals("Potensi bencana wajib diisi", DisasterReportValidator.error("", "Deskripsi", "Pos 1"))
        assertEquals("Deskripsi wajib diisi", DisasterReportValidator.error("Banjir", "", "Pos 1"))
        assertEquals("Lokasi wajib diisi", DisasterReportValidator.error("Banjir", "Deskripsi", ""))
        assertNull(DisasterReportValidator.error("Banjir", "Deskripsi", "Pos 1", "image/jpeg", 5L * 1024 * 1024))
        assertEquals("Lampiran tidak valid", DisasterReportValidator.error("Banjir", "Deskripsi", "Pos 1", "application/pdf", 1000L))
        assertEquals("Lampiran tidak valid", DisasterReportValidator.error("Banjir", "Deskripsi", "Pos 1", "image/jpeg", 6L * 1024 * 1024))
    }

    @Test
    fun `TDD-C04 - hanya kegagalan jaringan memutuskan penyimpanan SOS ke antrean`() {
        assertTrue(SosQueueRule.simpanOffline(IOException("no network")))
        assertFalse(SosQueueRule.simpanOffline(RuntimeException("server menolak")))
    }

    @Test
    fun `TDD-C05 - kegagalan sementara dicoba ulang dan penolakan permanen tidak dianggap terkirim`() {
        assertTrue(SosQueueRule.harusKirimUlang(code = null, error = IOException("timeout")))
        for (code in listOf(401, 408, 429, 500, 503)) {
            assertTrue("HTTP $code dapat dicoba kembali", SosQueueRule.harusKirimUlang(code, error = null))
        }
        for (code in listOf(403, 422)) {
            assertFalse("HTTP $code tidak dicoba ulang", SosQueueRule.harusKirimUlang(code, error = null))
        }
        assertFalse(SosQueueRule.harusKirimUlang(code = null, error = RuntimeException("ditolak")))
    }

    @Test
    fun `TDD-C06 - antrean chat terikat sesi SOS dan dapat dihapus tanpa menyatakan terkirim`() = runBlocking {
        val api = Retrofit.Builder().baseUrl("http://localhost/").build().create(ApiService::class.java)
        val repository = SosChatRepository(api, pendingChatDao = FakePendingChatDao())
        assertTrue(repository.savePendingChat(7, "text", "Tolong, butuh bantuan", null))
        assertTrue(repository.savePendingChat(8, "text", "Pesan sesi lain", null))
        val pesan = repository.observePendingChats(7).first().single()
        assertEquals(7, pesan.sosId)
        assertEquals("Tolong, butuh bantuan", pesan.content)
        assertEquals("PENDING", pesan.syncStatus)
        repository.deletePendingChat(pesan.localId)
        assertTrue(repository.observePendingChats(7).first().isEmpty())
        assertEquals("Pesan sesi lain", repository.observePendingChats(8).first().single().content)
    }

    private class FakePendingChatDao : PendingChatDao {
        private val chats = mutableListOf<PendingChatEntity>()

        fun firstLocalId(): String = chats.first().localId
        fun pendingCount(sosId: Int): Int = chats.count { it.sosId == sosId && it.syncStatus == "PENDING" }

        override suspend fun upsert(chat: PendingChatEntity) {
            chats.removeAll { it.localId == chat.localId }
            chats += chat
        }

        override suspend fun delete(localId: String) {
            chats.removeAll { it.localId == localId }
        }

        override suspend fun getPendingSync(pending: String, failed: String) = emptyList<PendingChatEntity>()
        override suspend fun getPendingForSos(sosId: Int) = chats.filter { it.sosId == sosId }
        override fun observePendingForSos(sosId: Int) = flowOf(chats.filter { it.sosId == sosId })
        override suspend fun markFailed(localId: String) = Unit
    }
}
