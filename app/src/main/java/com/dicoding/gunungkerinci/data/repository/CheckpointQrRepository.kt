package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.CancelCheckpointData
import com.dicoding.gunungkerinci.model.checkpointException
import com.dicoding.gunungkerinci.model.CheckpointQrRequest
import com.dicoding.gunungkerinci.model.CheckpointV2Data
import com.dicoding.gunungkerinci.network.ApiService

internal class CheckpointQrRepository(private val api: ApiService) {

    /** Check-in QR kontrak v2 (fase + idempotensi event). */
    suspend fun checkInV2(
        token: String,
        qrCodeValue: String,
        phase: String,
        clientEventId: String,
        recordedAt: String,
        latitude: Double? = null,
        longitude: Double? = null,
        pendakiId: String? = null
    ): Result<CheckpointV2Data> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.checkInQrV2(
            bearer,
            CheckpointQrRequest(
                qrCodeValue = qrCodeValue,
                pendakiId = pendakiId,
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
            throw response.checkpointException("Check-in gagal")
        }
    }

    /** Batalkan kehadiran (v2) pada satu pos + fase. */
    suspend fun cancelCheckInV2(token: String, checkpointLogId: Int): Result<CancelCheckpointData> =
        runCatching {
            val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val response = api.cancelCheckpointV2(bearer, checkpointLogId)
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                body.data
            } else {
                throw response.checkpointException("Gagal membatalkan kehadiran")
            }
        }

}
