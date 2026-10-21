package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.checkpointException
import com.dicoding.gunungkerinci.model.TrackingProgressV2Data
import com.dicoding.gunungkerinci.network.ApiService

internal class TrackingProgressRepository(
    private val api: ApiService,
    private val sessionStore: CheckpointSessionStore? = null
) {

    fun removeCachedVisit(token: String, bookingId: String, pendakiId: String, visitKey: String) {
        val cached = sessionStore?.readProgress(token, bookingId) ?: return
        val hikers = cached.hikers.map { hiker ->
            if (hiker.pendakiId != pendakiId) hiker else hiker.copy(
                visits = hiker.visits.map { visit ->
                    if (visit.visitKey != visitKey) visit else visit.copy(
                        completed = false, checkedAt = null, checkpointLogId = null, method = null
                    )
                }
            )
        }
        sessionStore.saveProgress(token, bookingId, cached.copy(hikers = hikers))
    }

    suspend fun saveMemberNote(token: String, bookingId: String, request: com.dicoding.gunungkerinci.model.SaveCheckpointMemberNoteRequest): Result<com.dicoding.gunungkerinci.model.CheckpointMemberNote> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.saveCheckpointMemberNote(bearer, request)
        val body = response.body()
        if (!response.isSuccessful || body?.success != true || body.data == null) {
            throw response.checkpointException("Catatan belum tersimpan. Coba simpan lagi.")
        }
        val saved = body.data
        sessionStore?.readProgress(token, bookingId)?.let { cached ->
            sessionStore.saveProgress(token, bookingId, cached.copy(memberNotes = cached.memberNotes.orEmpty().filter { it.key != saved.key } + saved))
        }
        saved
    }

    /** Progres v2: itinerary naik–turun per pendaki. */
    suspend fun getProgressV2(token: String, bookingId: String): Result<TrackingProgressV2Data> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.getTrackingProgressV2(bearer, bookingId)
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            sessionStore?.saveProgress(token, bookingId, body.data)
            body.data
        } else {
            throw response.checkpointException("Gagal mengambil progress pendakian")
        }
    }.recoverCatching { error ->
        if (error is java.io.IOException) sessionStore?.readProgress(token, bookingId)?.copy(fromCache = true) ?: throw error
        else throw error
    }
}
