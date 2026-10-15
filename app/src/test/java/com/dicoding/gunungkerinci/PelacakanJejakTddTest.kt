package com.dicoding.gunungkerinci

import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogDao
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogEntity
import com.dicoding.gunungkerinci.data.repository.TrackingRepository
import com.dicoding.gunungkerinci.model.GpsNearestPost
import com.dicoding.gunungkerinci.model.TrackingPost
import com.dicoding.gunungkerinci.model.TrackingProgressCheckpoint
import com.dicoding.gunungkerinci.model.TrackingProgressHiker
import com.dicoding.gunungkerinci.network.ApiService
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.CheckpointQrCheckInValidator
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.CheckpointQrParser
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.CheckpointState
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.GpsGateMapper
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.GpsGateState
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.LocalMemberMark
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.TrackingPostUiMapper
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.TrackingProgressUiMapper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

/**
 * Test Case TDD — fitur Pelacakan Jejak.
 *
 * Satu method = satu baris Tabel 16 (TDD-A01–TDD-A10). Semua murni JVM:
 * tanpa emulator, tanpa mock, tanpa Robolectric.
 */
class PelacakanJejakTddTest {

    // TDD-A01 — parsing QR checkpoint dengan format valid
    @Test
    fun `TDD-A01 - parsing QR checkpoint format valid`() {
        val hasil = CheckpointQrParser.parse("TNKS_CHECKPOINT:4")

        assertEquals(4, hasil?.postId)
    }

    // TDD-A02 — parsing QR checkpoint dengan format tidak valid
    @Test
    fun `TDD-A02 - parsing QR checkpoint format tidak valid`() {
        assertNull(CheckpointQrParser.parse("bukan-qr-checkpoint"))
        assertNull(CheckpointQrParser.parse(""))
        assertNull(CheckpointQrParser.parse(null))
    }

    // TDD-A03 — validasi QR check-in kosong
    @Test
    fun `TDD-A03 - validasi QR kosong`() {
        val pesanSpasi = CheckpointQrCheckInValidator.errorMessage(qrCodeValue = "   ")
        val pesanKosong = CheckpointQrCheckInValidator.errorMessage(qrCodeValue = "")
        val pesanTidakAda = CheckpointQrCheckInValidator.errorMessage(qrCodeValue = null)

        assertEquals("QR code tidak boleh kosong", pesanSpasi)
        assertEquals("QR code tidak boleh kosong", pesanKosong)
        assertEquals("QR code tidak boleh kosong", pesanTidakAda)
    }

    // TDD-A04 — validasi koordinat lokasi di luar rentang valid
    @Test
    fun `TDD-A04 - validasi koordinat di luar rentang`() {
        val lat = CheckpointQrCheckInValidator.errorMessage(
            qrCodeValue = "TNKS_CHECKPOINT:4",
            latitude = -100.0,
            longitude = 101.2635
        )
        val lon = CheckpointQrCheckInValidator.errorMessage(
            qrCodeValue = "TNKS_CHECKPOINT:4",
            latitude = -1.6975,
            longitude = 200.0
        )

        assertEquals("Latitude tidak valid", lat)
        assertEquals("Longitude tidak valid", lon)
    }

    // TDD-A05 — pemetaan data post/progress ke tampilan checkpoint
    @Test
    fun `TDD-A05 - pemetaan data post dan progress ke checkpoint`() {
        val posts = listOf(
            post(id = 3, nama = "Puncak Kerinci", urutan = 3),
            post(id = 1, nama = "Pos 1", urutan = 1),
            post(id = 2, nama = "Pos 2", urutan = 2)
        )
        val dariPost = TrackingPostUiMapper.toCheckpointUi(posts, completedPostIds = setOf(1))

        // Urutan tampil mengikuti urutan pos, bukan urutan data dari server.
        assertEquals(listOf("Pos 1", "Pos 2", "Puncak Kerinci"), dariPost.map { it.name })
        assertEquals(
            listOf(CheckpointState.Completed, CheckpointState.Current, CheckpointState.Upcoming),
            dariPost.map { it.state }
        )

        val checkpoints = listOf(
            TrackingProgressCheckpoint(postId = 2, nama = "Pos 2", urutan = 2, completed = false, checkedAt = null, method = null),
            TrackingProgressCheckpoint(postId = 1, nama = "Pos 1", urutan = 1, completed = true, checkedAt = null, method = "qr")
        )
        val dariProgress = TrackingProgressUiMapper.toCheckpointUi(
            checkpoints,
            hikers = listOf(hiker("p1", "Budi", postId = 1))
        )

        // Urut naik walau input dari server acak, dan pos yang sudah check-in berstatus Completed.
        assertEquals(listOf(1, 2), dariProgress.map { it.postId })
        assertEquals(listOf(CheckpointState.Completed, CheckpointState.Current), dariProgress.map { it.state })
        assertEquals(listOf("Budi"), dariProgress.first { it.postId == 1 }.memberNames)
    }

    // TDD-A06 — deteksi radius: pendaki berada DALAM radius pos
    @Test
    fun `TDD-A06 - pendaki berada dalam radius pos`() {
        val status = GpsGateMapper.toGateState(
            GpsNearestPost(
                id = 4,
                nama = "Puncak Kerinci",
                distanceMeters = 12.0,
                withinRadius = true,
                radiusMeter = 50
            )
        )

        assertTrue(status is GpsGateState.DalamRadius)
    }

    // TDD-A07 — deteksi radius: pendaki berada DI LUAR radius pos
    @Test
    fun `TDD-A07 - pendaki berada di luar radius pos`() {
        val status = GpsGateMapper.toGateState(
            GpsNearestPost(
                id = 4,
                nama = "Puncak Kerinci",
                distanceMeters = 500.0,
                withinRadius = false,
                radiusMeter = 50
            )
        )

        assertTrue(status is GpsGateState.LuarRadius)
    }

    // TDD-A08 — pencegahan duplikasi check-in pada pos yang sama
    @Test
    fun `TDD-A08 - check-in ulang di pos yang sama ditolak`() = runBlocking {
        val dao = FakeCheckpointLogDao()
        val repository = TrackingRepository(apiTidakDipakai(), checkpointLogDao = dao)

        dao.upsert(logCheckpoint(bookingId = "booking-1", pendakiId = "pendaki-1", postId = 4))

        // Pos 4 sudah tercatat → ditolak. Pos lain → masih boleh.
        assertTrue(repository.hasCheckedIn("booking-1", "pendaki-1", 4))
        assertFalse(repository.hasCheckedIn("booking-1", "pendaki-1", 5))
    }

    // TDD-A09 — pencatatan kehadiran anggota oleh ketua tim
    @Test
    fun `TDD-A09 - kehadiran anggota menggabungkan status server dan penanda lokal`() {
        val hikers = listOf(
            TrackingProgressHiker(
                pendakiId = "anggota-1",
                nama = "Ani",
                completed = 0,
                total = 3,
                percentage = 0,
                checkpoints = emptyList()
            ),
            TrackingProgressHiker(
                pendakiId = "anggota-2",
                nama = "Budi",
                completed = 0,
                total = 3,
                percentage = 0,
                checkpoints = emptyList()
            )
        )
        val localMarks = listOf(LocalMemberMark(postId = 4, pendakiId = "anggota-1", pending = true))

        val members = TrackingProgressUiMapper.toMembers(hikers, postId = 4, localMarks = localMarks)

        val ani = members.first { it.pendakiId == "anggota-1" }
        val budi = members.first { it.pendakiId == "anggota-2" }
        assertTrue(ani.checked)
        assertTrue(ani.pending)
        assertFalse(budi.checked)
        assertFalse(budi.pending)
    }

    // TDD-A10 — pembatalan kehadiran anggota membersihkan catatan lokal
    @Test
    fun `TDD-A10 - pembatalan kehadiran membersihkan catatan lokal`() = runBlocking {
        val dao = FakeCheckpointLogDao()
        val repository = TrackingRepository(apiTidakDipakai(), checkpointLogDao = dao)

        repository.saveServerLogId("booking-1", "pendaki-1", 4, serverId = 15, method = "qr")
        assertTrue(repository.hasCheckedIn("booking-1", "pendaki-1", 4))

        repository.deleteCheckInLocal("booking-1", "pendaki-1", 4)

        // Setelah dibatalkan, pos itu dianggap belum tercatat → bisa check-in lagi.
        assertFalse(repository.hasCheckedIn("booking-1", "pendaki-1", 4))
        assertNull(repository.getServerLogId("booking-1", "pendaki-1", 4))
    }

    // ==================== helper ====================

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

    private fun logCheckpoint(bookingId: String, pendakiId: String, postId: Int) =
        CheckpointLogEntity(
            localId = "${bookingId}_${pendakiId}_$postId",
            serverId = null,
            bookingId = bookingId,
            pendakiId = pendakiId,
            postId = postId,
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
            logs += log
        }

        override suspend fun countExistingCheckIn(bookingId: String, pendakiId: String, postId: Int): Int =
            logs.count {
                it.bookingId == bookingId && it.pendakiId == pendakiId && it.postId == postId
            }

        override suspend fun getPendingSync(pending: String, failed: String) = error("tidak dipakai")
        override suspend fun getLastCheckpoint(bookingId: String) = error("tidak dipakai")
        override suspend fun updateStatus(localId: String, status: String) = error("tidak dipakai")
        override suspend fun markSent(localId: String, serverId: Int?, status: String) = error("tidak dipakai")
        override suspend fun markFailed(localId: String, status: String) = error("tidak dipakai")
        override suspend fun markPermanentFailure(localId: String) = error("tidak dipakai")
        override suspend fun resetStuckSyncing() = error("tidak dipakai")
        override suspend fun resetRetriableFailures(bookingId: String): Int = error("tidak dipakai")

        // Dipakai saat membatalkan kehadiran (toggle dimatikan).
        override suspend fun getServerId(bookingId: String, pendakiId: String, postId: Int): Int? =
            logs.firstOrNull {
                it.bookingId == bookingId && it.pendakiId == pendakiId &&
                    it.postId == postId && it.serverId != null
            }?.serverId

        override suspend fun deleteCheckIn(bookingId: String, pendakiId: String, postId: Int) {
            logs.removeAll {
                it.bookingId == bookingId && it.pendakiId == pendakiId && it.postId == postId
            }
        }

        override suspend fun getLog(bookingId: String, pendakiId: String, postId: Int): CheckpointLogEntity? =
            logs.lastOrNull {
                it.bookingId == bookingId && it.pendakiId == pendakiId && it.postId == postId
            }

        override suspend fun getByBooking(bookingId: String): List<CheckpointLogEntity> =
            logs.filter { it.bookingId == bookingId }
    }
}
