package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.DisasterReportData
import com.dicoding.gunungkerinci.model.DisasterReportItem
import com.dicoding.gunungkerinci.network.ApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Laporan potensi bencana (multipart) + riwayat laporan milik pendaki.
 * Endpoint: POST /api/sos/disaster-report, GET /api/sos/disaster-reports.
 */
internal class DisasterReportRepository(private val api: ApiService) {

    suspend fun submit(
        token: String,
        potensiBencana: String,
        deskripsi: String,
        lokasi: String,
        latitude: Double? = null,
        longitude: Double? = null,
        lampiran: MultipartBody.Part? = null
    ): Result<DisasterReportData> = runCatching {
        val response = api.submitDisasterReport(
            bearerOf(token),
            potensiBencana = potensiBencana.toRequestBody(TEXT),
            deskripsi = deskripsi.toRequestBody(TEXT),
            lokasi = lokasi.toRequestBody(TEXT),
            latitude = latitude?.toString()?.toRequestBody(TEXT),
            longitude = longitude?.toString()?.toRequestBody(TEXT),
            lampiran = lampiran
        )
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.message ?: messageFor(response.code()))
        }
    }

    suspend fun getMyReports(token: String): Result<List<DisasterReportItem>> = runCatching {
        val response = api.getMyDisasterReports(bearerOf(token))
        val body = response.body()
        if (response.isSuccessful && body?.success == true) {
            body.data.orEmpty()
        } else {
            throw Exception(body?.message ?: "Gagal memuat laporan (${response.code()})")
        }
    }

    private fun messageFor(code: Int) = when (code) {
        403 -> "Tidak ada pendakian aktif"
        422 -> "Data laporan tidak valid"
        else -> "Gagal mengirim laporan ($code)"
    }

    private fun bearerOf(token: String) =
        if (token.startsWith("Bearer ")) token else "Bearer $token"

    private companion object {
        val TEXT = "text/plain".toMediaType()
    }
}
