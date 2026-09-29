package com.dicoding.gunungkerinci.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dicoding.gunungkerinci.data.local.TnksDatabase
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogEntity
import com.dicoding.gunungkerinci.data.local.sync.SyncStatus
import com.dicoding.gunungkerinci.data.repository.TrackingGpsRepository
import com.dicoding.gunungkerinci.data.repository.isoUtc
import com.dicoding.gunungkerinci.model.CheckpointQrRequest
import com.dicoding.gunungkerinci.model.ManualCheckInRequest
import com.dicoding.gunungkerinci.model.TrackingPointRequest
import com.dicoding.gunungkerinci.network.ApiConfig
import com.dicoding.gunungkerinci.network.ApiService
import com.dicoding.gunungkerinci.pref.UserPreference

/**
 * Worker pengirim antrean check-in offline.
 *
 * Alur: pendaki check-in saat tidak ada sinyal → log disimpan ke Room
 * (`checkpoint_logs`, status PENDING) → worker ini jalan saat internet tersedia
 * → tiap log dikirim ke endpoint yang sesuai (QR / manual) → status berubah
 * jadi SENT (berhasil) atau FAILED (gagal).
 *
 * Klasifikasi hasil per log:
 * - sukses / 409 (sudah pernah check-in) → SENT
 * - 403 / 404 / 422 (server menolak permanen) → FAILED, tidak dicoba lagi
 * - error jaringan / 5xx → FAILED, worker minta retry (backoff WorkManager)
 */
class CheckpointSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val token = UserPreference(applicationContext).getToken().orEmpty()
        if (token.isBlank()) return Result.retry() // belum login — coba lagi nanti

        val dao = TnksDatabase.getInstance(applicationContext).checkpointLogDao()
        val antrean = dao.getPendingSync().filter { it.retryCount < MAX_RETRY }
        if (antrean.isEmpty()) return Result.success()

        val api = ApiConfig.getApiService(applicationContext)
        var masihAdaSisa = false

        val berhasil = mutableListOf<CheckpointLogEntity>()

        for (log in antrean) {
            dao.updateStatus(log.localId, SyncStatus.SYNCING.name)
            when (val hasil = kirim(api, token, log)) {
                is HasilKirim.Sukses -> {
                    dao.markSent(log.localId, hasil.serverId)
                    berhasil += log
                }
                is HasilKirim.Sementara -> { dao.markFailed(log.localId); masihAdaSisa = true }
                is HasilKirim.Permanen -> dao.markPermanentFailure(log.localId)
            }
        }

        // Titik posisi untuk check-in yang baru saja tersinkron — satu request batch
        // (tanpa rate limit). Kalau gagal: diamkan, posisi bukan data kritis.
        val titik = berhasil.mapNotNull { log ->
            val lat = log.latitude ?: return@mapNotNull null
            val lon = log.longitude ?: return@mapNotNull null
            TrackingPointRequest(
                latitude = lat,
                longitude = lon,
                altitude = log.altitude,
                accuracy = log.accuracy,
                batteryLevel = log.batteryLevel,
                recordedAt = isoUtc(log.checkedAt) // waktu KEJADIAN, bukan waktu sinkron
            )
        }
        if (titik.isNotEmpty()) {
            runCatching { TrackingGpsRepository(api).kirimTitikBatch(token, titik) }
        }

        return if (masihAdaSisa) Result.retry() else Result.success()
    }

    private suspend fun kirim(
        api: ApiService,
        token: String,
        log: CheckpointLogEntity
    ): HasilKirim = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        when (log.method) {
            METHOD_QR -> {
                val response = api.checkInQr(
                    bearer,
                    CheckpointQrRequest(
                        qrCodeValue = "$QR_PREFIX${log.postId}",
                        latitude = log.latitude,
                        longitude = log.longitude
                    )
                )
                klasifikasi(response.isSuccessful, response.code(), null)
            }

            METHOD_MANUAL -> {
                val latitude = log.latitude
                val longitude = log.longitude
                if (latitude == null || longitude == null) {
                    // Data tidak lengkap — server menolak tanpa koordinat.
                    HasilKirim.Permanen
                } else {
                    val response = api.checkInManual(
                        bearer,
                        ManualCheckInRequest(
                            postId = log.postId,
                            latitude = latitude,
                            longitude = longitude
                        )
                    )
                    klasifikasi(response.isSuccessful, response.code(), null)
                }
            }

            else -> HasilKirim.Permanen
        }
    }.getOrElse { error ->
        // IOException (tidak ada sinyal / timeout) dan error tak terduga → coba lagi nanti.
        if (error is java.io.IOException) HasilKirim.Sementara else HasilKirim.Permanen
    }

    private fun klasifikasi(success: Boolean, code: Int, serverId: Int?): HasilKirim {
        return when {
            success -> HasilKirim.Sukses(serverId)
            code == HTTP_CONFLICT -> HasilKirim.Sukses(null) // sudah check-in di pos ini
            code == HTTP_FORBIDDEN || code == HTTP_NOT_FOUND || code == HTTP_UNPROCESSABLE ->
                HasilKirim.Permanen
            else -> HasilKirim.Sementara // 5xx / lainnya — layak dicoba lagi
        }
    }

    private sealed interface HasilKirim {
        data class Sukses(val serverId: Int?) : HasilKirim
        data object Sementara : HasilKirim
        data object Permanen : HasilKirim
    }

    private companion object {
        const val MAX_RETRY = 5
        const val METHOD_QR = "qr"
        const val METHOD_MANUAL = "manual"
        const val QR_PREFIX = "TNKS_CHECKPOINT:"
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
        const val HTTP_UNPROCESSABLE = 422
    }
}
