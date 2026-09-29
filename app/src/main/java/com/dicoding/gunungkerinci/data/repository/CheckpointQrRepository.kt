package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.CheckpointQrData
import com.dicoding.gunungkerinci.model.CheckpointQrRequest
import com.dicoding.gunungkerinci.network.ApiService

internal class CheckpointQrRepository (private val api: ApiService) {
    suspend fun checkIn (
        token: String,
        qrCodeValue: String,
        latitude: Double? = null,
        longitude: Double? = null,
        /**
         * UUID anggota yang dicatat. `null` = kehadiran diri sendiri.
         * Diisi = ketua tim mewakili anggota (server memverifikasi hak akses).
         */
        pendakiId: String? = null
    ): Result<CheckpointQrData> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.checkInQr(
            bearer,
            CheckpointQrRequest(
                qrCodeValue = qrCodeValue,
                pendakiId = pendakiId,
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
                    404 -> "QR code tidak valid atau pos tidak ditemukan"
                    409 -> "Sudah check-in di pos ini sebelumnya"
                    // 403 punya 3 sebab berbeda — pesan server dipakai bila ada,
                    // fallback ini hanya untuk respons tanpa body.
                    403 -> "Tidak berhak mencatat kehadiran ini"
                    429 -> "Terlalu banyak request, coba lagi nanti"
                    else -> "Check-in gagal (${response.code()})"
                }
            throw Exception(msg)
        }
    }
}