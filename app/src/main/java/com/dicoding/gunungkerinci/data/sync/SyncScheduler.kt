package com.dicoding.gunungkerinci.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dicoding.gunungkerinci.data.local.sync.SyncDefaults

/** Penjadwal pengiriman ulang antrean lokal saat internet tersedia. */
internal class SyncScheduler(context: Context) {

    private val appContext = context.applicationContext

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Kirim ulang check-in PENDING/FAILED milik satu booking. Booking kosong = semua. */
    fun scheduleCheckpointSync(bookingId: String = "") {
        val request = OneTimeWorkRequestBuilder<CheckpointSyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(SyncDefaults.BOOKING_ID to bookingId))
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(WORK_NAME_CHECKPOINT, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    /** Kirim ulang SOS yang tersimpan saat offline. */
    fun scheduleSosSync() {
        val request = OneTimeWorkRequestBuilder<PendingSosSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(WORK_NAME_SOS, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    /** Kirim ulang pesan chat darurat yang tersimpan saat offline. */
    fun scheduleChatSync() {
        val request = OneTimeWorkRequestBuilder<PendingChatSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(WORK_NAME_CHAT, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val WORK_NAME_CHECKPOINT = "tnks_checkpoint_sync"
        const val WORK_NAME_SOS = "tnks_sos_sync"
        const val WORK_NAME_CHAT = "tnks_chat_sync"
    }
}
