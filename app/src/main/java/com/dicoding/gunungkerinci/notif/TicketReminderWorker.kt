package com.dicoding.gunungkerinci.notif

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dicoding.gunungkerinci.network.ApiConfig
import com.dicoding.gunungkerinci.pref.UserPreference

/**
 * Pekerjaan berkala pemeriksaan tiket untuk notifikasi lokal (bukan push
 * server). Memanggil `/api/mytiket` sekali, menilai state dari waktu server,
 * lalu membuat notifikasi lokal untuk akun pemilik. Waktu HP tidak dipakai
 * sebagai sumber kebenaran keterlambatan.
 */
class TicketReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val token = UserPreference(applicationContext).getToken().orEmpty()
        if (token.isBlank()) return Result.failure()

        TicketNotifications.createChannel(applicationContext)

        return try {
            val api = ApiConfig.getApiService(applicationContext)
            val bearer = "Bearer $token"

            val myUserId = api.getPendakiIdentity(bearer).body()?.data?.id_user
            val response = api.getMyTiket(bearer)
            val body = response.body()

            if (!response.isSuccessful || body?.success != true) {
                // Offline / error sementara: jangan buat status/biaya sendiri.
                return Result.retry()
            }

            // Akun boleh berganti saat request berjalan; jangan tampilkan utk akun lama.
            if (UserPreference(applicationContext).getToken() != token) return Result.success()
            if (myUserId == null) return Result.success()

            val bookings = body.data.orEmpty()
            val serverNow = bookings.firstNotNullOfOrNull { it.overstay?.serverNow }
            val accountKey = "u$myUserId"
            val shown = TicketNotifDedup.allShown(applicationContext)

            val events = TicketNotificationSelection.selectEvents(
                bookings = bookings,
                myUserId = myUserId,
                serverNowIso = serverNow,
                reminderTime = REMINDER_TIME,
                accountKey = accountKey,
                alreadyShown = shown
            )

            for (event in events) {
                if (!TicketNotifications.allowed(applicationContext)) continue
                TicketNotifications.show(applicationContext, event)
                TicketNotifDedup.markShown(applicationContext, event.eventKey)
            }

            reconcileOverdue(bookings, myUserId, accountKey, serverNow)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * Hapus notifikasi overdue yang sudah tidak relevan (semua sudah turun)
     * pada pemeriksaan berikutnya.
     */
    private fun reconcileOverdue(
        bookings: List<com.dicoding.gunungkerinci.model.MyTiketItem>,
        myUserId: Int,
        accountKey: String,
        serverNowIso: String?
    ) {
        val today = TicketNotificationSelection.serverDate(serverNowIso) ?: return

        val current = bookings
            .filter { it.idUser == myUserId && (it.statusBooking ?: -1) in 4..8 }
            .filter { (it.tanggalKeluar?.take(10) ?: "") < today }
            .filter { TicketNotificationSelection.hasActivePendaki(it.overstay) }
            .map { it.id }
            .toSet()

        val previous = TicketNotifDedup.activeOverdueBookingIds(applicationContext)
        for (id in previous - current) {
            val key = "$accountKey:overdue:$id"
            TicketNotifications.cancel(applicationContext, key)
            TicketNotifDedup.clearEvent(applicationContext, key)
        }
        TicketNotifDedup.setActiveOverdue(applicationContext, current)
    }

    private companion object {
        const val REMINDER_TIME = "07:00"
    }
}
