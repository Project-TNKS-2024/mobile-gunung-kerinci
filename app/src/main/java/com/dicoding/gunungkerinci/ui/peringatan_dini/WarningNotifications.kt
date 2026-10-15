package com.dicoding.gunungkerinci.ui.peringatan_dini

import android.app.Notification
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

internal object WarningNotifications {
    const val MONITOR_CHANNEL = "warning_monitoring_v1"
    const val ALERT_CHANNEL = "early_warning_alarm_v1"
    const val MONITOR_ID = 4100
    const val ALERT_ID = 4101
    const val OPEN_WARNING = "open_early_warning"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(
            NotificationChannel(MONITOR_CHANNEL, "Pemantauan bahaya", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Status pemeriksaan peringatan dini selama pendakian"
                setSound(null, null)
            },
            NotificationChannel(ALERT_CHANNEL, "Alarm peringatan bahaya", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Peringatan bahaya dengan alarm sampai dihentikan"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                // Audio/getaran dikelola layanan supaya bisa diulang dan dihentikan bersama.
                setSound(null, null)
                enableVibration(false)
                setShowBadge(true)
            }
        ))
    }

    fun allowed(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(ALERT_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun contentIntent(context: Context, warning: Boolean) = PendingIntent.getActivity(
        context, if (warning) 1 else 0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(OPEN_WARNING, warning)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun action(context: Context, action: String) = PendingIntent.getService(
        context, action.hashCode(), Intent(context, WarningMonitoringService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun monitoring(context: Context, message: String): Notification =
        NotificationCompat.Builder(context, MONITOR_CHANNEL)
            .setSmallIcon(R.drawable.ic_warning_notification)
            .setContentTitle("Pemantauan bahaya aktif")
            .setContentText(message)
            .setContentIntent(contentIntent(context, false))
            .setOngoing(true).setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Hentikan pemantauan", action(context, WarningMonitoringService.STOP_MONITORING))
            .build()

    fun alert(context: Context, warning: PeringatanDini, ringing: Boolean): Notification =
        NotificationCompat.Builder(context, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_warning_notification)
            .setContentTitle("Peringatan bahaya: ${warning.title}")
            .setContentText(if (ringing) "Alarm aktif • Ketuk untuk melihat arahan" else "Alarm dihentikan • Ketuk untuk melihat arahan")
            .setStyle(NotificationCompat.BigTextStyle().bigText(warning.description))
            .setContentIntent(contentIntent(context, true))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(ringing).setAutoCancel(!ringing)
            .setOnlyAlertOnce(!ringing)
            .apply {
                if (ringing) addAction(0, "Hentikan alarm", action(context, WarningMonitoringService.STOP_ALARM))
            }.build()
}
