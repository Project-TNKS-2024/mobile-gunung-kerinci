package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.EmergencyWarningDto
import com.dicoding.gunungkerinci.network.ApiService
import com.dicoding.gunungkerinci.ui.peringatan_dini.EmergencyWarning

/**
 * Sumber peringatan dini bahaya untuk pendaki (GET /api/emergency/active).
 *
 * Catatan: endpoint mengembalikan SEMUA tipe (admin_broadcast & hiker_alert);
 * pemilihan peringatan ditampilkan dilakukan di EarlyWarningSelector (client).
 */
internal class EmergencyRepository(private val api: ApiService) {

    suspend fun getActiveWarnings(token: String): Result<List<EmergencyWarning>> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.getActiveEmergencies(bearer)
        val body = response.body()
        if (response.isSuccessful && body?.success == true) {
            body.data.orEmpty().map { it.toWarning() }
        } else {
            error(
                body?.message ?: when (response.code()) {
                    401 -> "Sesi berakhir, silakan login ulang"
                    403 -> "Tidak ada pendakian aktif"
                    else -> "Gagal memuat peringatan (${response.code()})"
                }
            )
        }
    }
}

private fun EmergencyWarningDto.toWarning() = EmergencyWarning(
    id = id,
    type = type,
    title = title ?: "(Tanpa judul)",
    description = description.orEmpty(),
    severity = severity ?: "low",
    createdAt = createdAt.orEmpty()
)
