package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.PeringatanDiniDto
import com.dicoding.gunungkerinci.network.ApiService
import com.dicoding.gunungkerinci.ui.peringatan_dini.PeringatanDini

/**
 * Exception API peringatan dini — membawa kode HTTP supaya pemanggil bisa
 * membedakan gagal sementara (jaringan/5xx) dari gagal permanen (401/403).
 */
internal class PeringatanDiniApiException(val code: Int, message: String) : Exception(message)

/**
 * Sumber peringatan dini bahaya untuk pendaki (GET /api/emergency/active).
 *
 * Catatan: endpoint mengembalikan SEMUA tipe (admin_broadcast & hiker_alert);
 * pemilihan peringatan ditampilkan dilakukan di PeringatanDiniSelector (client).
 */
internal class PeringatanDiniRepository(private val api: ApiService) {

    suspend fun getActiveWarnings(token: String): Result<List<PeringatanDini>> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.getActivePeringatanDini(bearer)
        val body = response.body()
        if (response.isSuccessful && body?.success == true) {
            body.data.orEmpty().map { it.toWarning() }
        } else {
            throw PeringatanDiniApiException(
                response.code(),
                body?.message ?: when (response.code()) {
                    401 -> "Sesi berakhir, silakan login ulang"
                    403 -> "Tidak ada pendakian aktif"
                    else -> "Gagal memuat peringatan (${response.code()})"
                }
            )
        }
    }
}

private fun PeringatanDiniDto.toWarning() = PeringatanDini(
    id = id,
    type = type,
    title = title ?: "(Tanpa judul)",
    description = description.orEmpty(),
    severity = severity ?: "low",
    createdAt = createdAt.orEmpty()
)
