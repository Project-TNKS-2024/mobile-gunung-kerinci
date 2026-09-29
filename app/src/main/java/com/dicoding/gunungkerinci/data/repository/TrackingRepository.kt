package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogDao
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogEntity
import com.dicoding.gunungkerinci.data.local.checkpoint.TrackingPostDao
import com.dicoding.gunungkerinci.data.local.checkpoint.TrackingPostEntity
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
        trackingPostDao?.upsertAll(posts.map { it.toEntity(gateId) })
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

    suspend fun hasCheckedIn(bookingId: String, pendakiId: String, postId: Int): Boolean {
        return (checkpointLogDao?.countExistingCheckIn(bookingId, pendakiId, postId) ?: 0) > 0
    }

    /** ID log di server untuk satu pos — dipakai saat membatalkan kehadiran. */
    suspend fun getServerLogId(bookingId: String, pendakiId: String, postId: Int): Int? {
        return checkpointLogDao?.getServerId(bookingId, pendakiId, postId)
    }

    /** Simpan ID log yang dikembalikan server supaya kehadiran bisa dibatalkan nanti. */
    suspend fun saveServerLogId(
        bookingId: String,
        pendakiId: String,
        postId: Int,
        serverId: Int,
        method: String
    ) {
        if (checkpointLogDao == null) return
        val localId = "${bookingId}_${pendakiId}_$postId"
        val existing = checkpointLogDao.getServerId(bookingId, pendakiId, postId)
        // Sudah tersimpan — tidak perlu menimpa.
        if (existing != null) return
        checkpointLogDao.upsert(
            CheckpointLogEntity(
                localId = localId,
                serverId = serverId,
                bookingId = bookingId,
                pendakiId = pendakiId,
                postId = postId,
                method = method,
                latitude = null,
                longitude = null,
                checkedAt = System.currentTimeMillis(),
                syncStatus = "SENT"
            )
        )
    }

    /** Hapus catatan lokal setelah kehadiran dibatalkan di server. */
    suspend fun deleteCheckInLocal(bookingId: String, pendakiId: String, postId: Int) {
        checkpointLogDao?.deleteCheckIn(bookingId, pendakiId, postId)
    }

    suspend fun getLastCheckpoint(bookingId: String): CheckpointLogEntity? {
        return checkpointLogDao?.getLastCheckpoint(bookingId)
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
            status = "active"
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
            radiusMeter = radiusMeter.toInt()
        )
    }
}
