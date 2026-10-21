package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.checkpointException
import com.dicoding.gunungkerinci.model.CheckpointV2Data
import com.dicoding.gunungkerinci.model.ManualCheckInRequest
import com.dicoding.gunungkerinci.network.ApiService

internal class ManualCheckInRepository(private val api: ApiService) {

    /** Check-in manual kontrak v2 (fase + idempotensi event). */
    suspend fun checkInV2(
        token: String,
        postId: Int,
        phase: String,
        clientEventId: String,
        recordedAt: String,
        latitude: Double,
        longitude: Double
    ): Result<CheckpointV2Data> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.checkInManualV2(
            bearer,
            ManualCheckInRequest(
                postId = postId,
                latitude = latitude,
                longitude = longitude,
                phase = phase,
                clientEventId = clientEventId,
                recordedAt = recordedAt
            )
        )
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw response.checkpointException("Check-in manual gagal")
        }
    }
}
