package com.dicoding.gunungkerinci.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Penjadwal sinkronisasi antrean lokal → server.
 *
 * Dipakai saat check-in gagal karena jaringan (sinyal gunung putus): log disimpan
 * ke Room dengan status PENDING, lalu worker ini mengirimnya begitu ada internet.
 */
internal class SyncScheduler(context: Context) {

    private val appContext = context.applicationContext

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Kirim ulang semua check-in yang masih PENDING/FAILED. */
    fun scheduleCheckpointSync() {
        val request = OneTimeWorkRequestBuilder<CheckpointSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(WORK_NAME_CHECKPOINT, ExistingWorkPolicy.KEEP, request)
    }

    /** Kirim ulang semua SOS yang tersimpan lokal (dikirim saat offline). */
    fun scheduleSosSync() {
        val request = OneTimeWorkRequestBuilder<PendingSosSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(WORK_NAME_SOS, ExistingWorkPolicy.KEEP, request)
    }

    /** Kirim ulang semua pesan chat darurat yang tersimpan lokal (US-12). */
    fun scheduleChatSync() {
        val request = OneTimeWorkRequestBuilder<PendingChatSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(WORK_NAME_CHAT, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NAME_CHECKPOINT = "tnks_checkpoint_sync"
        const val WORK_NAME_SOS = "tnks_sos_sync"
        const val WORK_NAME_CHAT = "tnks_chat_sync"
    }
}
