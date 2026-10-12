package com.dicoding.gunungkerinci.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dicoding.gunungkerinci.data.local.TnksDatabase
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogEntity
import com.dicoding.gunungkerinci.data.local.sync.SyncDefaults
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
 * Kirim antrean check-in yang tersimpan saat offline.
 * 409 dihitung berhasil (sudah check-in di pos itu), 403/404/422 berhenti,
 * sisanya dicoba lagi sampai [SyncDefaults.MAX_RETRY] kali.
 */
class CheckpointSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = TnksDatabase.getInstance(applicationContext).checkpointLogDao()
        // Sisa worker yang mati di tengah kirim.
        dao.resetStuckSyncing()
        val antrean = dao.getPendingSync().filter { it.retryCount < SyncDefaults.MAX_RETRY }
        // Antrean kosong: tidak ada kerjaan, sudahi sebelum cek login.
        if (antrean.isEmpty()) return Result.success()

        val token = UserPreference(applicationContext).getToken().orEmpty()
        if (token.isBlank()) return Result.retry() // belum login, coba lagi nanti

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

        // Kirim titik posisi check-in yang baru tersinkron. Kalau gagal, diamkan saja.
        val titik = berhasil.mapNotNull { log ->
            val lat = log.latitude ?: return@mapNotNull null
            val lon = log.longitude ?: return@mapNotNull null
            TrackingPointRequest(
                latitude = lat,
                longitude = lon,
                altitude = log.altitude,
                accuracy = log.accuracy,
                batteryLevel = log.batteryLevel,
                recordedAt = isoUtc(log.checkedAt) // waktu kejadian, bukan waktu sinkron
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
                        // Bisa berisi penandaan anggota oleh ketua, jadi pakai pendakiId dari log.
                        pendakiId = log.pendakiId,
                        latitude = log.latitude,
                        longitude = log.longitude
                    )
                )
                klasifikasi(response.isSuccessful, response.code(), response.body()?.data?.checkpointLogId)
            }

            METHOD_MANUAL -> {
                val latitude = log.latitude
                val longitude = log.longitude
                if (latitude == null || longitude == null) {
                    // Tanpa koordinat, server pasti menolak.
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
                    klasifikasi(response.isSuccessful, response.code(), response.body()?.data?.checkpointLogId)
                }
            }

            else -> HasilKirim.Permanen
        }
    }.getOrElse { error ->
        // Tidak ada sinyal atau timeout: coba lagi nanti. Error lain dianggap permanen.
        if (error is java.io.IOException) HasilKirim.Sementara else HasilKirim.Permanen
    }

    private fun klasifikasi(success: Boolean, code: Int, serverId: Int?): HasilKirim {
        return when {
            success -> HasilKirim.Sukses(serverId)
            code == HTTP_CONFLICT -> HasilKirim.Sukses(null) // sudah check-in di pos ini
            code == HTTP_FORBIDDEN || code == HTTP_NOT_FOUND || code == HTTP_UNPROCESSABLE ->
                HasilKirim.Permanen
            else -> HasilKirim.Sementara // 5xx dan lainnya, layak dicoba lagi
        }
    }

    private sealed interface HasilKirim {
        data class Sukses(val serverId: Int?) : HasilKirim
        data object Sementara : HasilKirim
        data object Permanen : HasilKirim
    }

    private companion object {
        const val METHOD_QR = "qr"
        const val METHOD_MANUAL = "manual"
        const val QR_PREFIX = "TNKS_CHECKPOINT:"
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
        const val HTTP_UNPROCESSABLE = 422
    }
}
