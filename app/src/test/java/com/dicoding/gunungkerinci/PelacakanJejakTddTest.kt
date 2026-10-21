package com.dicoding.gunungkerinci

import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogDao
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogEntity
import com.dicoding.gunungkerinci.data.local.checkpoint.checkpointLocalId
import com.dicoding.gunungkerinci.data.repository.TrackingRepository
import com.dicoding.gunungkerinci.data.sync.CheckpointSyncResult
import com.dicoding.gunungkerinci.data.sync.classifyCheckpointResponse
import com.dicoding.gunungkerinci.model.*
import com.dicoding.gunungkerinci.network.ApiService
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.*
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit

/** Tujuh skenario skripsi TDD-A01 sampai TDD-A07; satu metode per skenario. */
class PelacakanJejakTddTest {

    @Test
    fun `TDD-A01 - QR valid terbaca sedangkan format salah dan kosong ditolak`() {
        assertEquals(4, CheckpointQrParser.parse("TNKS_CHECKPOINT:4")?.postId)
        assertNull(CheckpointQrCheckInValidator.errorMessage("TNKS_CHECKPOINT:4"))
        assertNull(CheckpointQrParser.parse("bukan-qr-checkpoint"))
        for (qr in listOf(null, "", "   ")) {
            assertNull(CheckpointQrParser.parse(qr))
            assertEquals("QR code tidak boleh kosong", CheckpointQrCheckInValidator.errorMessage(qr))
        }
    }

    @Test
    fun `TDD-A02 - koordinat valid diterima dan koordinat QR boleh tidak diisi`() {
        assertNull(CheckpointQrCheckInValidator.errorMessage("TNKS_CHECKPOINT:4", -1.6975, 101.2635))
        assertNull(CheckpointQrCheckInValidator.errorMessage("TNKS_CHECKPOINT:4"))
        assertEquals("Latitude tidak valid", CheckpointQrCheckInValidator.errorMessage("TNKS_CHECKPOINT:4", -100.0, 101.2635))
        assertEquals("Longitude tidak valid", CheckpointQrCheckInValidator.errorMessage("TNKS_CHECKPOINT:4", -1.6975, 200.0))
    }

    @Test
    fun `TDD-A03 - hasil radius dipetakan sebagai dasar konfirmasi GPS`() {
        val dekat = GpsNearestPost(id = 4, nama = "Puncak Kerinci", distanceMeters = 12.0, withinRadius = true, radiusMeter = 50)
        assertTrue(GpsGateMapper.toGateState(dekat) is GpsGateState.DalamRadius)
        assertTrue(GpsGateMapper.toGateState(dekat.copy(distanceMeters = 500.0, withinRadius = false)) is GpsGateState.LuarRadius)
    }

    @Test
    fun `TDD-A04 - itinerary dan progres membedakan naik turun tanpa menggandakan puncak`() {
        val visits = TrackingVisitUiMapper.fromPosts(listOf(
            post(3, "Puncak Kerinci", 3).copy(isSummit = true),
            post(1, "Pos 1", 1), post(2, "Pos 2", 2)
        ))
        assertEquals(listOf("naik:1", "naik:2", "naik:3", "turun:2", "turun:1"), visits.map { it.visitKey })
        assertEquals(1, visits.count { it.isSummit })
        val awal = TrackingVisitUiMapper.project(hikerV2("p1", "Budi").copy(visits = visits), emptyList())
        val uiAwal = TrackingVisitUiMapper.toCheckpointUi(awal.visits, awal.nextVisitKey, listOf(awal))
        assertEquals("naik:1", uiAwal.single { it.state == CheckpointState.Current }.visitKey)
        assertTrue(uiAwal.filter { it.phase == "turun" }.all { it.state == CheckpointState.Upcoming })

        val naikSelesai = visits.map { it.copy(completed = it.phase == "naik") }
        val turun = TrackingVisitUiMapper.project(awal.copy(visits = naikSelesai), emptyList())
        assertEquals("turun", turun.trackingPhase)
        assertEquals(PhaseProgress(3, 3, 100), turun.ascentProgress)
        assertEquals(PhaseProgress(0, 2, 0), turun.descentProgress)
        assertEquals(PhaseProgress(3, 5, 60), turun.overallProgress)
        assertEquals("turun:2", turun.nextVisitKey)
        val uiTurun = TrackingVisitUiMapper.toCheckpointUi(turun.visits, turun.nextVisitKey, listOf(turun))
        assertEquals("turun:2", uiTurun.single { it.state == CheckpointState.Current }.visitKey)
        assertEquals(3, uiTurun.count { it.state == CheckpointState.Completed })
    }

    @Test
    fun `TDD-A05 - identitas kunjungan dan respons ulang harus sesuai sebelum dianggap terkirim`() = runBlocking {
        val dao = FakeCheckpointLogDao()
        val repository = TrackingRepository(apiTidakDipakai(), checkpointLogDao = dao)
        val log = logCheckpoint("booking-1", "pendaki-1", 4).copy(clientEventId = "event-1")
        dao.upsert(log)
        assertTrue(repository.hasCheckedIn("booking-1", "pendaki-1", 4, "naik"))
        assertFalse(repository.hasCheckedIn("booking-2", "pendaki-1", 4, "naik"))
        assertFalse(repository.hasCheckedIn("booking-1", "pendaki-2", 4, "naik"))
        assertFalse(repository.hasCheckedIn("booking-1", "pendaki-1", 5, "naik"))
        assertFalse(repository.hasCheckedIn("booking-1", "pendaki-1", 4, "turun"))

        val data = CheckpointV2Data(checkpointLogId = 15, clientEventId = "event-1", pendakiId = "pendaki-1",
            phase = "naik", post = CheckpointQrPost(4, "Pos 4", 4), visitKey = "naik:4")
        val sukses = Response.success(CheckpointV2Response(true, null, data, null))
        assertEquals(CheckpointSyncResult.Sent(15), classifyCheckpointResponse(sukses, log, "event-1"))
        assertEquals(CheckpointSyncResult.Sent(15), classifyCheckpointResponse(duplicateResponse("naik:4"), log, "event-1"))
        assertEquals(CheckpointSyncResult.Rejected, classifyCheckpointResponse(duplicateResponse("turun:4"), log, "event-1"))
        assertEquals(CheckpointSyncResult.Rejected, classifyCheckpointResponse(
            Response.success(CheckpointV2Response(true, null, data.copy(pendakiId = "pendaki-2"), null)), log, "event-1"))
        assertEquals(CheckpointSyncResult.Rejected, classifyCheckpointResponse(
            Response.error<CheckpointV2Response>(403, "{}".toResponseBody("application/json".toMediaType())), log, "event-1"))
    }

    @Test
    fun `TDD-A06 - kehadiran server dan antrean lokal hanya berlaku pada pos dan fase yang sesuai`() {
        val visits = TrackingVisitUiMapper.fromPosts(listOf(post(1, "Pos 1", 1), post(4, "Puncak", 2).copy(isSummit = true)))
        val hikers = listOf(
            hikerV2("anggota-1", "Ani"),
            hikerV2("anggota-2", "Budi").copy(visits = visits.map { it.copy(completed = it.visitKey == "naik:4") }),
            hikerV2("anggota-3", "Cici")
        )
        val marks = listOf(LocalMemberMark(postId = 4, pendakiId = "anggota-1", pending = true, phase = "naik"))
        val members = TrackingProgressUiMapper.toMembers(hikers, 4, "naik", marks)
        assertTrue(members[0].checked)
        assertTrue(members[0].pending)
        assertTrue(members[1].checked)
        assertFalse(members[1].pending)
        assertFalse(members[2].checked)
        assertFalse(members[2].pending)
        assertTrue(TrackingProgressUiMapper.toMembers(hikers, 4, "turun", marks).none { it.checked || it.pending })
        assertTrue(TrackingProgressUiMapper.toMembers(hikers, 1, "naik", marks).none { it.checked || it.pending })
    }

    @Test
    fun `TDD-A07 - pembatalan menghapus kunjungan terpilih tanpa menghapus fase lain`() = runBlocking {
        val repository = TrackingRepository(apiTidakDipakai(), checkpointLogDao = FakeCheckpointLogDao())
        repository.saveServerLogId("booking-1", "pendaki-1", 4, "naik", serverId = 15, method = "qr")
        repository.saveServerLogId("booking-1", "pendaki-1", 4, "turun", serverId = 16, method = "qr")
        assertTrue(repository.hasCheckedIn("booking-1", "pendaki-1", 4, "naik"))
        repository.deleteCheckInLocal("booking-1", "pendaki-1", 4, "naik")
        assertFalse(repository.hasCheckedIn("booking-1", "pendaki-1", 4, "naik"))
        assertNull(repository.getServerLogId("booking-1", "pendaki-1", 4, "naik"))
        assertTrue(repository.hasCheckedIn("booking-1", "pendaki-1", 4, "turun"))
        assertEquals(16, repository.getServerLogId("booking-1", "pendaki-1", 4, "turun"))
    }

    private fun duplicateResponse(visitKey: String) = Response.error<CheckpointV2Response>(409,
        """{"success":false,"errors":{"code":"CHECKPOINT_ALREADY_RECORDED","visit_key":"$visitKey","checkpoint_log_id":15}}"""
            .toResponseBody("application/json".toMediaType()))



    private fun post(id: Int, nama: String, urutan: Int) = TrackingPost(
        id = id,
        nama = nama,
        urutan = urutan,
        latitude = "-1.6975",
        longitude = "101.2635",
        altitude = null,
        radiusMeter = null
    )

    private fun hiker(id: String, nama: String, postId: Int) = TrackingProgressHiker(
        pendakiId = id,
        nama = nama,
        completed = 1,
        total = 3,
        percentage = 33,
        checkpoints = listOf(
            TrackingProgressCheckpoint(
                postId = postId, nama = "Pos $postId", urutan = postId,
                completed = true, checkedAt = null, method = "qr"
            )
        )
    )

    private fun hikerV2(id: String, nama: String) = TrackingProgressV2Hiker(
        pendakiId = id,
        nama = nama,
        isLeader = false,
        trackingPhase = TrackingPhase.NAIK.serialized,
        nextVisitKey = null,
        ascentProgress = PhaseProgress(0, 3, 0),
        descentProgress = PhaseProgress(0, 2, 0),
        overallProgress = PhaseProgress(0, 5, 0),
        routeReturnCompleted = false,
        visits = emptyList()
    )

    private fun logCheckpoint(bookingId: String, pendakiId: String, postId: Int) =
        CheckpointLogEntity(
            localId = checkpointLocalId(bookingId, pendakiId, postId, TrackingPhase.NAIK.serialized),
            serverId = null,
            bookingId = bookingId,
            pendakiId = pendakiId,
            postId = postId,
            phase = TrackingPhase.NAIK.serialized,
            method = "qr",
            latitude = -1.6975,
            longitude = 101.2635,
            checkedAt = 1_000L
        )

    /**
     * `hasCheckedIn()` hanya membaca DAO, jadi instance `ApiService` di sini
     * tidak pernah dipanggil — cukup dibuat lewat Retrofit tanpa server.
     */
    private fun apiTidakDipakai(): ApiService =
        Retrofit.Builder().baseUrl("http://localhost/").build().create(ApiService::class.java)

    /** DAO in-memory minimal untuk tes Pelacakan Jejak. */
    private class FakeCheckpointLogDao : CheckpointLogDao {

        private val logs = mutableListOf<CheckpointLogEntity>()

        override suspend fun upsert(log: CheckpointLogEntity) {
            logs.removeAll { it.localId == log.localId }
            logs += log
        }

        override suspend fun countExistingCheckIn(bookingId: String, pendakiId: String, postId: Int, phase: String): Int =
            logs.count {
                it.bookingId == bookingId && it.pendakiId == pendakiId && it.postId == postId && it.phase == phase
            }

        override suspend fun getPendingSync(pending: String, failed: String) = error("tidak dipakai")
        override suspend fun getLastCheckpoint(bookingId: String) = error("tidak dipakai")
        override suspend fun updateStatus(localId: String, status: String) = error("tidak dipakai")
        override suspend fun setClientEventId(localId: String, clientEventId: String) = error("tidak dipakai")
        override suspend fun markSent(localId: String, serverId: Int?, status: String) = error("tidak dipakai")
        override suspend fun markFailed(localId: String, status: String) = error("tidak dipakai")
        override suspend fun markPermanentFailure(localId: String) = error("tidak dipakai")
        override suspend fun resetStuckSyncing() = error("tidak dipakai")
        override suspend fun resetRetriableFailures(bookingId: String): Int = error("tidak dipakai")
        override suspend fun getAscentLogs(bookingId: String, pendakiId: String) = error("tidak dipakai")
        override suspend fun countSentSummit(bookingId: String, pendakiId: String): Int = error("tidak dipakai")

        // Dipakai saat membatalkan kehadiran (toggle dimatikan).
        override suspend fun getServerId(bookingId: String, pendakiId: String, postId: Int, phase: String): Int? =
            logs.firstOrNull {
                it.bookingId == bookingId && it.pendakiId == pendakiId &&
                    it.postId == postId && it.phase == phase && it.serverId != null
            }?.serverId

        override suspend fun deleteCheckIn(bookingId: String, pendakiId: String, postId: Int, phase: String) {
            logs.removeAll {
                it.bookingId == bookingId && it.pendakiId == pendakiId && it.postId == postId && it.phase == phase
            }
        }

        override suspend fun getLog(bookingId: String, pendakiId: String, postId: Int, phase: String): CheckpointLogEntity? =
            logs.lastOrNull {
                it.bookingId == bookingId && it.pendakiId == pendakiId && it.postId == postId && it.phase == phase
            }

        override suspend fun getByBooking(bookingId: String): List<CheckpointLogEntity> =
            logs.filter { it.bookingId == bookingId }

        override suspend fun removeStaleServerLogs(bookingId: String, pendakiId: String, serverIds: List<Int>) {
            logs.removeAll { it.bookingId == bookingId && it.pendakiId == pendakiId &&
                it.syncStatus == "SENT" && it.serverId !in serverIds }
        }
    }
}
