package com.dicoding.gunungkerinci.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R

/**
 * Channel dan builder notifikasi lokal "Informasi tiket". Terpisah dari channel
 * alarm bahaya; tidak memakai alarm, audio berulang, atau full-screen intent.
 */
internal object TicketNotifications {

    const val CHANNEL = "ticket_info_v1"
    const val EXTRA_BOOKING_ID = "ticket_notif_booking_id"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Informasi tiket", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Pengingat jadwal turun dan status tiket"
                setSound(null, null)
                enableVibration(true)
            }
        )
    }

    fun allowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun show(context: Context, event: TicketEvent) {
        val id = notificationId(event.eventKey)
        val contentIntent = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_BOOKING_ID, event.bookingId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_warning_notification)
            .setContentTitle(event.title)
            .setContentText(event.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(event.body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun cancel(context: Context, eventKey: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(eventKey))
    }

    /** Hapus semua notifikasi tiket yang pernah ditampilkan (dipakai saat logout). */
    fun cancelShown(context: Context) {
        TicketNotifDedup.allShown(context).forEach { cancel(context, it) }
    }

    fun notificationId(eventKey: String): Int = eventKey.hashCode() and 0x7FFFFFFF
}
