package com.dicoding.gunungkerinci.ui.peringatan_dini

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.dicoding.gunungkerinci.data.repository.PeringatanDiniApiException
import com.dicoding.gunungkerinci.data.repository.PeringatanDiniRepository
import com.dicoding.gunungkerinci.network.ApiConfig
import com.dicoding.gunungkerinci.pref.UserPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WarningMonitoringService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var polling: Job? = null
    private lateinit var audio: WarningAlarmAudio
    private lateinit var receipts: WarningReceiptStore
    private var currentWarning: PeringatanDini? = null
    private var sessionToken: String? = null
    private var monitorMessage = "Memeriksa peringatan setiap 15 detik"
    private val notifications get() = NotificationManagerCompat.from(this)
    private val repository by lazy { PeringatanDiniRepository(ApiConfig.getApiService(applicationContext)) }
    internal var warningSource: suspend (String) -> Result<List<PeringatanDini>> = { token ->
        repository.getActiveWarnings(token)
    }

    override fun onCreate() {
        super.onCreate()
        WarningNotifications.createChannels(this)
        audio = WarningAlarmAudio(this)
        receipts = WarningReceiptStore(this)
        scope.launch {
            WarningMonitoring.fetchedWarnings.collect { batch ->
                acceptWarnings(batch.token, batch.warnings)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP_MONITORING) {
            WarningMonitoring.disable(this)
            stopSelf()
            return START_NOT_STICKY
        }
        val token = UserPreference(this).getToken().orEmpty()
        if (token.isBlank() || !WarningMonitoring.enabled(this) || !WarningNotifications.allowed(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (sessionToken == token && polling?.isActive == true && intent?.action == null) return START_STICKY
        if (sessionToken != null && sessionToken != token) {
            polling?.cancel()
            polling = null
            stopAlarm(keepNotification = false)
        }
        sessionToken = token
        if (intent?.action == STOP_ALARM) stopAlarm(keepNotification = true)
        promote(WarningMonitoring.ringing.value)
        WarningMonitoring.setRunning(true)
        if (polling?.isActive != true) polling = scope.launch { poll(token) }
        return START_STICKY
    }

    private suspend fun poll(token: String) {
        while (scope.isActive) {
            if (!sessionValid(token) || !WarningNotifications.allowed(this)) {
                stopSelf()
                return
            }
            val result = warningSource(token)
            currentCoroutineContext().ensureActive()
            // Logout/ganti akun dapat terjadi ketika request masih berjalan.
            if (!sessionValid(token)) return
            result.onSuccess { warnings ->
                updateMonitorMessage("Memeriksa peringatan setiap 15 detik")
                acceptWarnings(token, warnings)
            }.onFailure { error ->
                if ((error as? PeringatanDiniApiException)?.code in listOf(401, 403)) {
                    stopSelf()
                    return
                }
                updateMonitorMessage("Koneksi tertunda • Mencoba kembali saat jaringan tersedia")
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun sessionValid(token: String) =
        UserPreference(this).getToken() == token && WarningMonitoring.enabled(this)

    internal fun acceptWarnings(token: String, warnings: List<PeringatanDini>) {
        if (!sessionValid(token)) return
        if (!WarningNotifications.allowed(this)) {
            stopSelf()
            return
        }
        if (currentWarning != null && warnings.none { it.id == currentWarning?.id }) {
            stopAlarm(keepNotification = false)
        }
        val unseen = receipts.unseen(token, warnings)
        val warning = PeringatanDiniSelector.select(unseen) ?: return
        currentWarning = warning
        WarningMonitoring.setAlert(warning)
        // Naikkan type/notification SEBELUM audio focus dan playback (Android 15+).
        WarningMonitoring.setRinging(true)
        promote(ringing = true)
        audio.start()
        receipts.markReceived(token, unseen)
    }

    private fun foregroundTypes(ringing: Boolean): Int {
        val monitoringType = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        return monitoringType or if (ringing && Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
    }

    private fun promote(ringing: Boolean) {
        val warning = currentWarning
        val notification = if (ringing && warning != null) WarningNotifications.alert(this, warning, true)
            else WarningNotifications.monitoring(this, monitorMessage)
        val id = if (ringing && warning != null) WarningNotifications.ALERT_ID else WarningNotifications.MONITOR_ID
        ServiceCompat.startForeground(this, id, notification, foregroundTypes(ringing))
        if (ringing) notifications.cancel(WarningNotifications.MONITOR_ID)
    }

    private fun updateMonitorMessage(message: String) {
        if (monitorMessage == message) return
        monitorMessage = message
        if (!WarningMonitoring.ringing.value) promote(ringing = false)
    }

    private fun stopAlarm(keepNotification: Boolean) {
        audio.stop()
        WarningMonitoring.setRinging(false)
        promote(ringing = false)
        val warning = currentWarning
        if (keepNotification && warning != null && WarningNotifications.allowed(this)) {
            notifications.notify(WarningNotifications.ALERT_ID, WarningNotifications.alert(this, warning, false))
        } else {
            notifications.cancel(WarningNotifications.ALERT_ID)
            currentWarning = null
            WarningMonitoring.setAlert(null)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        audio.stop()
        WarningMonitoring.setRinging(false)
        WarningMonitoring.setRunning(false)
        WarningMonitoring.setAlert(null)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notifications.cancel(WarningNotifications.MONITOR_ID)
        notifications.cancel(WarningNotifications.ALERT_ID)
        super.onDestroy()
    }

    companion object {
        const val STOP_ALARM = "com.dicoding.gunungkerinci.STOP_WARNING_ALARM"
        const val STOP_MONITORING = "com.dicoding.gunungkerinci.STOP_WARNING_MONITORING"
        private const val POLL_INTERVAL_MS = 15_000L
    }
}
