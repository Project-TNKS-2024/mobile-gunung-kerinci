package com.dicoding.gunungkerinci.ui.peringatan_dini

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.dicoding.gunungkerinci.pref.UserPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object WarningMonitoring {
    private val runningState = MutableStateFlow(false)
    val running = runningState.asStateFlow()
    private val ringingState = MutableStateFlow(false)
    val ringing = ringingState.asStateFlow()
    private val alertState = MutableStateFlow<PeringatanDini?>(null)
    val alert = alertState.asStateFlow()
    internal fun setAlert(warning: PeringatanDini?) { alertState.value = warning }
    internal fun setRunning(value: Boolean) { runningState.value = value }
    internal fun setRinging(value: Boolean) { ringingState.value = value }
    private fun prefs(context: Context) = context.getSharedPreferences("warning_monitor", Context.MODE_PRIVATE)
    fun enabled(context: Context) = prefs(context).getBoolean("enabled", true)
    fun enable(context: Context) { prefs(context).edit().putBoolean("enabled", true).apply() }

    // Hanya dipanggil dari Activity terlihat, bukan restart tersembunyi di background.
    fun start(context: Context): Boolean {
        WarningNotifications.createChannels(context)
        if (!enabled(context) || !WarningNotifications.allowed(context) ||
            UserPreference(context).getToken().isNullOrBlank()) return false
        return runCatching {
            ContextCompat.startForegroundService(context, Intent(context, WarningMonitoringService::class.java))
            true
        }.getOrDefault(false)
    }

    fun stopAlarm(context: Context) {
        if (runningState.value) context.startService(
            Intent(context, WarningMonitoringService::class.java).setAction(WarningMonitoringService.STOP_ALARM)
        )
    }

    fun disable(context: Context) {
        prefs(context).edit().putBoolean("enabled", false).apply()
        context.stopService(Intent(context, WarningMonitoringService::class.java))
        setRunning(false)
        setRinging(false)
        setAlert(null)
    }

    fun clearSession(context: Context) {
        disable(context)
        WarningReceiptStore(context).clear()
        prefs(context).edit().clear().apply()
        androidx.core.app.NotificationManagerCompat.from(context).cancel(WarningNotifications.ALERT_ID)
    }
}
