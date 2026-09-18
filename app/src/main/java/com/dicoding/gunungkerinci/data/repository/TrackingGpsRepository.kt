package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.TrackingBatchRequest
import com.dicoding.gunungkerinci.model.TrackingPointRequest
import com.dicoding.gunungkerinci.network.ApiService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pengirim titik posisi ke gk_tracking.
 *
 * Aturan: posisi BUKAN data kritis. 429 (rate limit 30 detik) dan 403 (tidak ada
 * pendakian aktif) diperlakukan sebagai "selesai" — tidak dilempar sebagai error,
 * supaya alur check-in yang memanggilnya tidak pernah terganggu.
 */
internal class TrackingGpsRepository(private val api: ApiService) {

    suspend fun kirimTitik(
        token: String,
        latitude: Double,
        longitude: Double,
        altitude: Double? = null,
        accuracy: Double? = null,
        batteryLevel: Int? = null
    ): Result<Unit> = runCatching {
        val bearer = bearerOf(token)
        val response = api.postTrackingGps(
            bearer,
            TrackingPointRequest(latitude, longitude, altitude, accuracy, batteryLevel)
        )
        if (!response.isSuccessful &&
            response.code() != HTTP_TOO_MANY_REQUESTS &&
            response.code() != HTTP_FORBIDDEN
        ) {
            error(response.body()?.message ?: "Gagal kirim titik posisi (${response.code()})")
        }
    }

    /** Maks 100 titik per request (batas validasi backend). */
    suspend fun kirimTitikBatch(
        token: String,
        points: List<TrackingPointRequest>
    ): Result<Int> = runCatching {
        if (points.isEmpty()) return@runCatching 0
        val bearer = bearerOf(token)
        val response = api.postTrackingGpsBatch(bearer, TrackingBatchRequest(points.take(MAX_BATCH)))
        val body = response.body()
        if (response.isSuccessful && body?.success == true) {
            body.data?.inserted ?: points.size
        } else {
            error(body?.message ?: "Gagal sinkron titik posisi (${response.code()})")
        }
    }

    private fun bearerOf(token: String) =
        if (token.startsWith("Bearer ")) token else "Bearer $token"

    private companion object {
        const val MAX_BATCH = 100
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_FORBIDDEN = 403
    }
}

/**
 * millis → ISO-8601 UTC, contoh "2026-09-08T03:00:00.000Z".
 * Dipakai sebagai `recorded_at` di jalur offline (wajib diisi di endpoint batch).
 *
 * CATATAN: minSdk 24 — JANGAN pakai java.time (butuh API 26 tanpa desugaring).
 */
internal fun isoUtc(millis: Long): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(millis))
}
