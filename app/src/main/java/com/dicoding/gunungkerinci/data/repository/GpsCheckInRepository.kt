package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.GpsCheckData
import com.dicoding.gunungkerinci.model.GpsCheckRequest
import com.dicoding.gunungkerinci.network.ApiService

internal class GpsCheckInRepository(private val api: ApiService) {

    /**
     * Cek apakah pendaki berada dalam radius pos terdekat.
     * Endpoint ini TIDAK mencatat check-in — hanya deteksi kedekatan.
     * Jika [GpsCheckData.nearestPost.withinRadius] == true, tampilkan tombol check-in manual.
     */
    suspend fun checkNearbyPost(
        token: String,
        latitude: Double,
        longitude: Double,
        accuracy: Double? = null
    ): Result<GpsCheckData> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.checkNearbyPostGps(
            bearer,
            GpsCheckRequest(
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy
            )
        )
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            val msg = body?.message
                ?: when (response.code()) {
                    403 -> "Tidak ada pendakian aktif"
                    422 -> "Koordinat GPS tidak valid"
                    else -> "Gagal cek posisi GPS (${response.code()})"
                }
            throw Exception(msg)
        }
    }
}
