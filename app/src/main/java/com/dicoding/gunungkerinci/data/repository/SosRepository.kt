package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.data.local.sos.PendingSosDao
import com.dicoding.gunungkerinci.data.local.sos.PendingSosEntity
import com.dicoding.gunungkerinci.model.SosActiveData
import com.dicoding.gunungkerinci.model.SosTriggerData
import com.dicoding.gunungkerinci.model.SosTriggerRequest
import com.dicoding.gunungkerinci.network.ApiService
import java.util.UUID

/**
 * SOS panic button: kirim sinyal darurat & cek status SOS aktif.
 *
 * Endpoint backend:
 * - POST /api/sos/trigger  (lat, lon, severity wajib; message opsional)
 * - GET  /api/sos/active   (data null bila tidak ada SOS aktif)
 */
internal class SosRepository(
    private val api: ApiService,
    private val pendingSosDao: PendingSosDao? = null
) {

    suspend fun trigger(
        token: String,
        latitude: Double,
        longitude: Double,
        severity: String,
        message: String?
    ): Result<SosTriggerData> = runCatching {
        val response = api.triggerSos(
            bearerOf(token),
            SosTriggerRequest(
                latitude = latitude,
                longitude = longitude,
                severity = severity,
                message = message
            )
        )
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            if (response.code() == HTTP_TOO_MANY_REQUESTS) {
                throw SosCooldownException(
                    SosApiErrorParser.waitSeconds(response.errorBody()?.string()) ?: 0
                )
            }
            throw Exception(body?.message ?: messageFor(response.code()))
        }
    }

    suspend fun getActive(token: String): Result<SosActiveData?> = runCatching {
        val response = api.getActiveSos(bearerOf(token))
        val body = response.body()
        if (response.isSuccessful && body?.success == true) {
            body.data
        } else {
            throw Exception(body?.message ?: messageFor(response.code()))
        }
    }

    /** Simpan SOS ke antrean lokal saat tidak ada jaringan (US-12). */
    suspend fun savePendingSos(
        latitude: Double,
        longitude: Double,
        severity: String,
        message: String?,
        createdAt: Long = System.currentTimeMillis()
    ): Boolean {
        val dao = pendingSosDao ?: return false
        return runCatching {
            dao.upsert(
                PendingSosEntity(
                    localId = UUID.randomUUID().toString(),
                    latitude = latitude,
                    longitude = longitude,
                    severity = severity,
                    message = message,
                    createdAt = createdAt
                )
            )
            true
        }.getOrDefault(false)
    }

    private fun messageFor(code: Int) = when (code) {
        403 -> "Tidak ada pendakian aktif"
        404 -> "Destinasi tidak ditemukan"
        422 -> "Data SOS tidak valid"
        else -> "Gagal mengirim SOS ($code)"
    }

    private fun bearerOf(token: String) =
        if (token.startsWith("Bearer ")) token else "Bearer $token"

    private companion object {
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
