package com.dicoding.gunungkerinci.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dicoding.gunungkerinci.data.local.TnksDatabase
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogEntity
import com.dicoding.gunungkerinci.data.local.sync.SyncDefaults
import com.dicoding.gunungkerinci.data.local.sync.SyncStatus
import com.dicoding.gunungkerinci.data.repository.TrackingGpsRepository
import com.dicoding.gunungkerinci.data.repository.CheckpointSessionStore
import com.dicoding.gunungkerinci.data.repository.isoWib
import com.dicoding.gunungkerinci.model.CheckpointQrRequest
import com.dicoding.gunungkerinci.model.ManualCheckInRequest
import com.dicoding.gunungkerinci.model.TrackingPhase
import com.dicoding.gunungkerinci.model.TrackingPointRequest
import com.dicoding.gunungkerinci.network.ApiConfig
import com.dicoding.gunungkerinci.network.ApiService
import com.dicoding.gunungkerinci.pref.UserPreference
import java.util.UUID

/**
 * Kirim antrean check-in v2 (fase naik/turun) yang tersimpan saat offline.
 *
 * Aturan:
 *  - Urutkan per booking + pendaki: naik dulu, lalu turun, lalu waktu kejadian.
 *  - Turun tidak dikirim sampai summit naik target sudah terkirim (SENT).
 *  - client_event_id dipakai ulang pada retry; replay identik dianggap sukses.
 *  - HTTP 409 TIDAK otomatis sukses: parse kode error dan cocokkan identitas canonical.
 */
class CheckpointSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = TnksDatabase.getInstance(applicationContext).checkpointLogDao()
        dao.resetStuckSyncing()
        val bookingId = inputData.getString(SyncDefaults.BOOKING_ID).orEmpty()

        val antrean = dao.getPendingSync()
            .filter { it.retryCount < SyncDefaults.MAX_RETRY }
            .filter { bookingId.isBlank() || it.bookingId == bookingId }
            .sortedWith(
                compareBy(
                    { it.bookingId },
                    { if (it.phase == TrackingPhase.TURUN.serialized) 1 else 0 },
                    { it.checkedAt },
                    { it.pendakiId }
                )
            )

        if (antrean.isEmpty()) return Result.success()

        val token = UserPreference(applicationContext).getToken().orEmpty()
        if (token.isBlank()) return Result.retry()

        val api = ApiConfig.getApiService(applicationContext)
        var masihAdaSisa = false
        val berhasil = mutableListOf<CheckpointLogEntity>()
        val sessions = CheckpointSessionStore(applicationContext)
        // Cache keys include the login token. Old-account events must not be sent as
        // the next person who logs in, even when the worker survives logout.
        val bookings = antrean.map { it.bookingId }.distinct().associateWith { id ->
            sessions.readProgress(token, id) ?: runCatching {
                val response = api.getTrackingProgressV2("Bearer ${token.removePrefix("Bearer ")}", id)
                if (response.code() == 401 || response.code() == 408 || response.code() == 429 || response.code() >= 500) {
                    masihAdaSisa = true
                }
                response.body()?.takeIf { response.isSuccessful && it.success }?.data
                    ?.also { sessions.saveProgress(token, id, it) }
            }.onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                if (it is java.io.IOException) masihAdaSisa = true
            }.getOrNull()
        }

        for (log in antrean) {
            val progress = bookings[log.bookingId] ?: continue
            val currentHiker = progress.hikers.firstOrNull { it.pendakiId == progress.currentPendakiId }
                ?: continue
            if (progress.hikers.none { it.pendakiId == log.pendakiId } ||
                (!currentHiker.isLeader && log.pendakiId != currentHiker.pendakiId)) continue
            val localLogs = dao.getByBooking(log.bookingId)
            val dependencies = mutableListOf<CheckpointLogEntity>()
            // Only local summit events impose an ordering dependency. A summit recorded
            // online/on another device may have no local log; let the server validate it.
            if (log.phase == TrackingPhase.TURUN.serialized) {
                localLogs.firstOrNull {
                    it.pendakiId == log.pendakiId && it.isSummit &&
                        it.phase == TrackingPhase.NAIK.serialized
                }?.let { dependencies += it }
            }
            val leaderId = progress.hikers.firstOrNull { it.isLeader }?.pendakiId
            if (leaderId != null && log.pendakiId != leaderId) {
                localLogs.firstOrNull {
                    it.pendakiId == leaderId && it.postId == log.postId && it.phase == log.phase
                }?.let { dependencies += it }
            }
            val unsent = dependencies.filter { it.syncStatus != SyncStatus.SENT.name }
            if (unsent.isNotEmpty()) {
                if (unsent.any { it.syncStatus == SyncStatus.REJECTED.name || it.retryCount >= 99 }) {
                    dao.markPermanentFailure(log.localId)
                } else if (unsent.all { it.retryCount < SyncDefaults.MAX_RETRY }) {
                    masihAdaSisa = true
                }
                // An exhausted transient dependency stays pending until a user retry;
                // don't turn it into a permanent rejection or keep WorkManager spinning.
                continue
            }
            // Log lama (migrasi) belum punya client_event_id: buat sekali lalu simpan.
            var clientEventId = log.clientEventId
            if (clientEventId.isNullOrBlank()) {
                clientEventId = UUID.randomUUID().toString()
                dao.setClientEventId(log.localId, clientEventId)
            }

            dao.updateStatus(log.localId, SyncStatus.SYNCING.name)
            when (val hasil = kirim(api, token, log, clientEventId)) {
                is CheckpointSyncResult.Sent -> {
                    dao.markSent(log.localId, hasil.serverId)
                    berhasil += log
                }
                is CheckpointSyncResult.Retry -> {
                    dao.markFailed(log.localId)
                    masihAdaSisa = true
                }
                is CheckpointSyncResult.Rejected -> dao.markPermanentFailure(log.localId)
                is CheckpointSyncResult.WaitingForSummit -> dao.markPermanentFailure(log.localId)
            }
        }

        val titik = berhasil.mapNotNull { log ->
            val lat = log.latitude ?: return@mapNotNull null
            val lon = log.longitude ?: return@mapNotNull null
            TrackingPointRequest(
                latitude = lat,
                longitude = lon,
                altitude = log.altitude,
                accuracy = log.accuracy,
                batteryLevel = log.batteryLevel,
                recordedAt = isoWib(log.checkedAt)
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
        log: CheckpointLogEntity,
        clientEventId: String
    ): CheckpointSyncResult = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val recordedAt = isoWib(log.checkedAt)
        when (log.method) {
            METHOD_QR -> {
                val response = api.checkInQrV2(
                    bearer,
                    CheckpointQrRequest(
                        qrCodeValue = "$QR_PREFIX${log.postId}",
                        pendakiId = log.pendakiId,
                        latitude = log.latitude,
                        longitude = log.longitude,
                        phase = log.phase,
                        clientEventId = clientEventId,
                        recordedAt = recordedAt
                    )
                )
                classifyCheckpointResponse(response, log, clientEventId)
            }

            METHOD_MANUAL -> {
                val latitude = log.latitude
                val longitude = log.longitude
                if (latitude == null || longitude == null) {
                    CheckpointSyncResult.Rejected
                } else {
                    val response = api.checkInManualV2(
                        bearer,
                        ManualCheckInRequest(
                            postId = log.postId,
                            latitude = latitude,
                            longitude = longitude,
                            phase = log.phase,
                            clientEventId = clientEventId,
                            recordedAt = recordedAt
                        )
                    )
                    classifyCheckpointResponse(response, log, clientEventId)
                }
            }

            else -> CheckpointSyncResult.Rejected
        }
    }.getOrElse { error ->
        if (error is kotlinx.coroutines.CancellationException) throw error
        if (error is java.io.IOException) CheckpointSyncResult.Retry else CheckpointSyncResult.Rejected
    }

    private companion object {
        const val METHOD_QR = "qr"
        const val METHOD_MANUAL = "manual"
        const val QR_PREFIX = "TNKS_CHECKPOINT:"
    }
}
