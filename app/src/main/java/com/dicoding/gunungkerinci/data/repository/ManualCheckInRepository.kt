package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.ManualCheckInData
import com.dicoding.gunungkerinci.model.ManualCheckInRequest
import com.dicoding.gunungkerinci.network.ApiService

internal class ManualCheckInRepository(private val api: ApiService) {

    /**
     * Check-in manual — pendaki memilih pos dari daftar dan menekan tombol.
     * Backend mencatat [isManualOverride] = true jika jarak > 500m.
     */
    suspend fun checkIn(
        token: String,
        postId: Int,
        latitude: Double,
        longitude: Double
    ): Result<ManualCheckInData> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.checkInManual(
            bearer,
            ManualCheckInRequest(
                postId = postId,
                latitude = latitude,
                longitude = longitude
            )
        )
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            val msg = body?.message
                ?: when (response.code()) {
                    403 -> "Tidak ada pendakian aktif"
                    404 -> "Pos tidak ditemukan"
                    409 -> "Sudah check-in di pos ini sebelumnya"
                    422 -> "Data tidak valid"
                    else -> "Check-in manual gagal (${response.code()})"
                }
            throw Exception(msg)
        }
    }
}
