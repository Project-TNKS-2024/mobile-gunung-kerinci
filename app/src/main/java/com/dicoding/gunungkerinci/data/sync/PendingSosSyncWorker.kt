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
 * Tidak ada batas jumlah percobaan: antrean baru berhenti kalau terkirim
 * atau ditolak server.
 *
 * Klasifikasi hasil per item (aturan lengkap ada di [SosQueueRule]):
 * - sukses → SENT
 * - IOException / 401 / 408 / 429 / 5xx → FAILED, worker minta retry (backoff)
 * - sisanya (403 / 404 / 422 dan kode 4xx lain) → REJECTED, tidak dicoba lagi
 */
class PendingSosSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = TnksDatabase.getInstance(applicationContext).pendingSosDao()
        // Reset baris SYNCING sisa worker yang mati di tengah kirim (A13).
        dao.resetStuckSyncing()
        // Buang baris yang sudah terkirim dari sesi sebelumnya.
        dao.deleteSent()
        // SOS tidak dibatasi jumlah percobaan: selama ada jaringan terus dicoba.
        // Yang menghentikannya cuma berhasil terkirim atau ditolak server.
        val antrean = dao.getPendingSync()
        // Antrean kosong: tidak ada kerjaan, sudahi sebelum cek login.
        if (antrean.isEmpty()) return Result.success()

        val token = UserPreference(applicationContext).getToken().orEmpty()
        if (token.isBlank()) return Result.retry() // belum login — coba lagi nanti

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
                severity = sos.severity.lowercase(),
                message = sos.message
            )
        )
        if (response.isSuccessful) Hasil.Sukses
        else if (SosQueueRule.harusKirimUlang(response.code(), null)) Hasil.Sementara
        else Hasil.Permanen
    }.getOrElse { error ->
        if (SosQueueRule.harusKirimUlang(null, error)) Hasil.Sementara else Hasil.Permanen
    }

    private sealed interface Hasil {
        data object Sukses : Hasil
        data object Sementara : Hasil
        data object Permanen : Hasil
    }
}
