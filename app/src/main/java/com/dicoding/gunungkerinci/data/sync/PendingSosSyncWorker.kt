package com.dicoding.gunungkerinci.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dicoding.gunungkerinci.data.local.TnksDatabase
import com.dicoding.gunungkerinci.data.local.sos.PendingSosEntity
import com.dicoding.gunungkerinci.data.local.sync.SyncStatus
import com.dicoding.gunungkerinci.model.SosTriggerRequest
import com.dicoding.gunungkerinci.network.ApiConfig
import com.dicoding.gunungkerinci.network.ApiService
import com.dicoding.gunungkerinci.pref.UserPreference

/**
 * Worker pengirim antrean SOS offline.
 *
 * Alur: pendaki menekan SOS tanpa sinyal → data disimpan ke Room (`pending_sos`,
 * status PENDING) → worker ini mengirimnya saat internet tersedia.
 *
 * Klasifikasi hasil per item:
 * - sukses → SENT
 * - 429 (cooldown) / 5xx / IOException → FAILED, worker minta retry (backoff)
 * - 403 / 404 / 422 (ditolak permanen) → FAILED, tidak dicoba lagi
 */
class PendingSosSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val token = UserPreference(applicationContext).getToken().orEmpty()
        if (token.isBlank()) return Result.retry() // belum login — coba lagi nanti

        val dao = TnksDatabase.getInstance(applicationContext).pendingSosDao()
        val antrean = dao.getPendingSync().filter { it.retryCount < MAX_RETRY }
        if (antrean.isEmpty()) return Result.success()

        val api = ApiConfig.getApiService(applicationContext)
        var masihAdaSisa = false

        for (sos in antrean) {
            dao.updateStatus(sos.localId, SyncStatus.SYNCING.name)
            when (kirim(api, token, sos)) {
                is Hasil.Sukses -> dao.markSent(sos.localId)
                is Hasil.Sementara -> {
                    dao.markFailed(sos.localId)
                    masihAdaSisa = true
                }
                is Hasil.Permanen -> dao.markPermanentFailure(sos.localId)
            }
        }

        return if (masihAdaSisa) Result.retry() else Result.success()
    }

    private suspend fun kirim(
        api: ApiService,
        token: String,
        sos: PendingSosEntity
    ): Hasil = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.triggerSos(
            bearer,
            SosTriggerRequest(
                latitude = sos.latitude,
                longitude = sos.longitude,
                severity = sos.severity,
                message = sos.message
            )
        )
        klasifikasi(response.isSuccessful, response.code())
    }.getOrElse { error ->
        if (error is java.io.IOException) Hasil.Sementara else Hasil.Permanen
    }

    private fun klasifikasi(success: Boolean, code: Int): Hasil = when {
        success -> Hasil.Sukses
        code == HTTP_TOO_MANY_REQUESTS -> Hasil.Sementara // cooldown, coba lagi nanti
        code == HTTP_FORBIDDEN || code == HTTP_NOT_FOUND || code == HTTP_UNPROCESSABLE ->
            Hasil.Permanen
        else -> Hasil.Sementara // 5xx / lainnya
    }

    private sealed interface Hasil {
        data object Sukses : Hasil
        data object Sementara : Hasil
        data object Permanen : Hasil
    }

    private companion object {
        const val MAX_RETRY = 5
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_UNPROCESSABLE = 422
    }
}
