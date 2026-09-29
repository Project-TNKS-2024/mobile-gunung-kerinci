package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.SosCallOption
import com.dicoding.gunungkerinci.network.ApiService

/** Kontak darurat (tel/WA). Endpoint: GET /api/sos/call-options. */
internal class SosCallRepository(private val api: ApiService) {

    suspend fun getCallOptions(token: String): Result<List<SosCallOption>> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.getSosCallOptions(bearer)
        val body = response.body()
        if (response.isSuccessful && body?.success == true) {
            body.data.orEmpty()
        } else {
            throw Exception(body?.message ?: "Gagal memuat kontak darurat (${response.code()})")
        }
    }
}
