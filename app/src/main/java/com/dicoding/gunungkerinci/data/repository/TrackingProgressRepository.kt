package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.TrackingProgressData
import com.dicoding.gunungkerinci.network.ApiService

internal class TrackingProgressRepository(private val api: ApiService) {
    suspend fun getProgress(token: String, bookingId: String): Result<TrackingProgressData> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.getTrackingProgress(bearer, bookingId)
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.message ?: "Gagal mengambil progress pendakian (${response.code()})")
        }
    }
}
