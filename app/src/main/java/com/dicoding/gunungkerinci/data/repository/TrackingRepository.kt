package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogDao
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogEntity
import com.dicoding.gunungkerinci.data.local.checkpoint.TrackingPostDao
import com.dicoding.gunungkerinci.data.local.checkpoint.TrackingPostEntity
import com.dicoding.gunungkerinci.data.local.checkpoint.checkpointLocalId
import com.dicoding.gunungkerinci.model.TrackingPhase
import com.dicoding.gunungkerinci.model.TrackingPost
import com.dicoding.gunungkerinci.network.ApiService

class TrackingRepository(
    private val api: ApiService,
    private val trackingPostDao: TrackingPostDao? = null,
    private val checkpointLogDao: CheckpointLogDao? = null
) {
    suspend fun getPosts(token: String, gateId: Int): Result<List<TrackingPost>> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.getTrackingPosts(bearer, gateId)
        if (!response.isSuccessful) {
            error(response.message().ifBlank { "Gagal mengambil data post" })
        }
        val body = response.body() ?: error("Respons post kosong")
        if (!body.success) {
            error(body.message ?: "Gagal mengambil data post")
        }
        val posts = body.data.orEmpty()
        trackingPostDao?.replaceForGate(gateId, posts.map { it.toEntity(gateId) })
        posts
    }.recoverCatching { throwable ->
        val cachedPosts = trackingPostDao?.getByGate(gateId).orEmpty()
        if (cachedPosts.isNotEmpty()) cachedPosts.map { it.toModel() } else throw throwable
    }

    suspend fun cachePosts(posts: List<TrackingPostEntity>) {
        trackingPostDao?.upsertAll(posts)
    }

    suspend fun getCachedPosts(gateId: Int): List<TrackingPostEntity> {
        return trackingPostDao?.getByGate(gateId).orEmpty()
    }

    suspend fun saveCheckpointPending(log: CheckpointLogEntity) {
        checkpointLogDao?.upsert(log)
    }

    suspend fun hasCheckedIn(bookingId: String, pendakiId: String, postId: Int, phase: String): Boolean {
        return (checkpointLogDao?.countExistingCheckIn(bookingId, pendakiId, postId, phase) ?: 0) > 0
    }

    /** ID log di server untuk satu pos + fase — dipakai saat membatalkan kehadiran. */
    suspend fun getServerLogId(bookingId: String, pendakiId: String, postId: Int, phase: String): Int? {
        return checkpointLogDao?.getServerId(bookingId, pendakiId, postId, phase)
    }

    /** Catatan lokal satu (booking, pendaki, pos, fase) — dipakai membatalkan sebelum tersinkron. */
    suspend fun getCheckpointLog(bookingId: String, pendakiId: String, postId: Int, phase: String): CheckpointLogEntity? {
        return checkpointLogDao?.getLog(bookingId, pendakiId, postId, phase)
    }

    /** Semua catatan lokal satu booking — dipakai overlay "menunggu kirim" di UI. */
    suspend fun getCheckpointLogs(bookingId: String): List<CheckpointLogEntity> {
        return checkpointLogDao?.getByBooking(bookingId).orEmpty()
    }

    /** Simpan ID log yang dikembalikan server supaya kehadiran bisa dibatalkan nanti. */
    suspend fun saveServerLogId(
        bookingId: String,
        pendakiId: String,
        postId: Int,
        phase: String,
        serverId: Int,
        method: String,
        clientEventId: String? = null,
        isSummit: Boolean = false,
        checkedAt: Long = System.currentTimeMillis()
    ) {
        if (checkpointLogDao == null) return
        val localId = checkpointLocalId(bookingId, pendakiId, postId, phase)
        val existing = checkpointLogDao.getLog(bookingId, pendakiId, postId, phase)
        checkpointLogDao.upsert(
            CheckpointLogEntity(
                localId = localId,
                serverId = serverId,
                bookingId = bookingId,
                pendakiId = pendakiId,
                postId = postId,
                phase = phase,
                clientEventId = clientEventId,
                isSummit = isSummit,
                method = method,
                latitude = null,
                longitude = null,
                checkedAt = checkedAt,
                syncStatus = "SENT"
            ).let { canonical ->
                existing?.copy(
                    serverId = serverId, isSummit = isSummit, checkedAt = checkedAt,
                    syncStatus = "SENT", retryCount = 0,
                    clientEventId = clientEventId ?: existing.clientEventId
                ) ?: canonical
            }
        )
    }

    suspend fun removeStaleServerLogs(bookingId: String, pendakiId: String, serverIds: List<Int>) {
        checkpointLogDao?.removeStaleServerLogs(bookingId, pendakiId, serverIds)
    }

    /** Hapus catatan lokal setelah kehadiran dibatalkan di server. */
    suspend fun deleteCheckInLocal(bookingId: String, pendakiId: String, postId: Int, phase: String) {
        checkpointLogDao?.deleteCheckIn(bookingId, pendakiId, postId, phase)
    }

    suspend fun getLastCheckpoint(bookingId: String): CheckpointLogEntity? {
        return checkpointLogDao?.getLastCheckpoint(bookingId)
    }

    /** Apakah summit naik pendaki sudah terkirim ke server (dipakai antrean turun). */
    suspend fun hasSentSummit(bookingId: String, pendakiId: String): Boolean {
        return (checkpointLogDao?.countSentSummit(bookingId, pendakiId) ?: 0) > 0
    }

    suspend fun getAscentLogs(bookingId: String, pendakiId: String): List<CheckpointLogEntity> {
        return checkpointLogDao?.getAscentLogs(bookingId, pendakiId).orEmpty()
    }

    /**
     * Buka percobaan baru untuk catatan yang sebelumnya menyerah. Mengembalikan jumlah
     * baris yang dibuka, supaya pemanggil hanya menjadwalkan sinkron bila memang perlu.
     */
    suspend fun resetRetriableFailures(bookingId: String): Int {
        return checkpointLogDao?.resetRetriableFailures(bookingId) ?: 0
    }

    private fun TrackingPost.toEntity(gateId: Int): TrackingPostEntity {
        return TrackingPostEntity(
            id = id,
            gateId = gateId,
            name = nama,
            order = urutan,
            latitude = latitude.toDoubleOrNull() ?: 0.0,
            longitude = longitude.toDoubleOrNull() ?: 0.0,
            altitude = altitude?.toDouble(),
            radiusMeter = radiusMeter?.toDouble() ?: 0.0,
            qrCodeValue = null,
            status = "active",
            estimatedTimeMinutes = estimatedTimeMinutes,
            infoIsDummy = infoIsDummy,
            temperatureC = temperatureC,
            distanceToSummitM = distanceToSummitM,
            distanceToNextM = distanceToNextM,
            mapImageUrl = mapImageUrl,
            isSummit = isSummit,
            estimatedDescentTimeMinutes = estimatedDescentTimeMinutes
        )
    }

    private fun TrackingPostEntity.toModel(): TrackingPost {
        return TrackingPost(
            id = id,
            nama = name,
            urutan = order,
            latitude = latitude.toString(),
            longitude = longitude.toString(),
            altitude = altitude?.toInt(),
            radiusMeter = radiusMeter.toInt(),
            estimatedTimeMinutes = estimatedTimeMinutes,
            infoIsDummy = infoIsDummy,
            temperatureC = temperatureC,
            distanceToSummitM = distanceToSummitM,
            distanceToNextM = distanceToNextM,
            mapImageUrl = mapImageUrl,
            isSummit = isSummit,
            estimatedDescentTimeMinutes = estimatedDescentTimeMinutes
        )
    }
}
