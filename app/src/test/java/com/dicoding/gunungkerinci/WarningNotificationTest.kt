package com.dicoding.gunungkerinci

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import com.dicoding.gunungkerinci.pref.UserPreference
import com.dicoding.gunungkerinci.data.repository.PeringatanDiniApiException
import com.dicoding.gunungkerinci.ui.peringatan_dini.*
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@LooperMode(LooperMode.Mode.PAUSED)
class WarningNotificationTest {
    private lateinit var context: Context
    private val token = "test-session"
    private fun warning(id: Int = 1, severity: String = "high") = PeringatanDini(
        id, "admin_broadcast", "Hujan deras", "Berlindung di pos terdekat", severity = severity,
        createdAt = "2026-10-15T10:00:00Z"
    )

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        UserPreference(context).saveToken(token)
        WarningReceiptStore(context).clear()
        WarningMonitoring.enable(context)
        WarningNotifications.createChannels(context)
    }

    @After fun cleanup() {
        UserPreference(context).clearToken()
        WarningMonitoring.setRinging(false)
        WarningMonitoring.setRunning(false)
    }

    @Test fun `receipts survive recreation and empty polls but new warnings and severity alert again`() {
        val store = WarningReceiptStore(context)
        store.markReceived(token, listOf(warning()))
        assertTrue(WarningReceiptStore(context).unseen(token, listOf(warning())).isEmpty())
        store.markReceived(token, emptyList())
        assertTrue(store.unseen(token, listOf(warning())).isEmpty())
        assertEquals(listOf(warning(2)), store.unseen(token, listOf(warning(), warning(2))))
        assertEquals(1, store.unseen(token, listOf(warning(severity = "critical"))).size)
        assertEquals(1, store.unseen("another-session", listOf(warning())).size)
    }

    @Test fun `alarm notification is public high priority ongoing and opens warning without stopping it`() {
        val manager = context.getSystemService(NotificationManager::class.java)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, manager.getNotificationChannel(WarningNotifications.ALERT_CHANNEL).importance)
        val notification = WarningNotifications.alert(context, warning(), true)
        assertEquals(Notification.VISIBILITY_PUBLIC, notification.visibility)
        assertEquals(Notification.CATEGORY_ALARM, notification.category)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals("Hentikan alarm", notification.actions.single().title)
        val open = shadowOf(notification.contentIntent).savedIntent
        assertEquals(MainActivity::class.java.name, open.component?.className)
        assertTrue(open.getBooleanExtra(WarningNotifications.OPEN_WARNING, false))
        assertEquals(WarningMonitoringService.STOP_ALARM, shadowOf(notification.actions.single().actionIntent).savedIntent.action)
    }

    @Test fun `stop action cancels vibration keeps warning and repeat polls never restart alarm`() {
        val controller = Robolectric.buildService(WarningMonitoringService::class.java).create()
        val service = controller.get()
        service.warningSource = { awaitCancellation() }
        try {
            service.acceptWarnings(token, listOf(warning()))
            assertTrue(WarningMonitoring.ringing.value)
            val vibrator = context.getSystemService(Vibrator::class.java)
            assertTrue(shadowOf(vibrator).isVibrating)
            service.onStartCommand(Intent(context, WarningMonitoringService::class.java).setAction(WarningMonitoringService.STOP_ALARM), 0, 1)
            assertFalse(WarningMonitoring.ringing.value)
            assertFalse(shadowOf(vibrator).isVibrating)
            assertTrue(WarningMonitoring.running.value)
            val manager = context.getSystemService(NotificationManager::class.java)
            val stopped = shadowOf(manager).getNotification(WarningNotifications.ALERT_ID)
            assertNotNull(stopped)
            assertEquals(0, stopped.flags and Notification.FLAG_ONGOING_EVENT)
            service.acceptWarnings(token, listOf(warning()))
            assertFalse(WarningMonitoring.ringing.value)
            service.acceptWarnings(token, listOf(warning(), warning(2)))
            assertTrue(WarningMonitoring.ringing.value)
        } finally { controller.destroy() }
    }

    @Test fun `server withdrawal stops alarm and removes notification`() {
        val controller = Robolectric.buildService(WarningMonitoringService::class.java).create()
        try {
            controller.get().acceptWarnings(token, listOf(warning()))
            controller.get().acceptWarnings(token, emptyList())
            assertFalse(WarningMonitoring.ringing.value)
            assertNull(shadowOf(context.getSystemService(NotificationManager::class.java)).getNotification(WarningNotifications.ALERT_ID))
        } finally { controller.destroy() }
    }

    @Test fun `late response from previous session cannot sound alarm`() {
        val controller = Robolectric.buildService(WarningMonitoringService::class.java).create()
        try {
            UserPreference(context).saveToken("new-session")
            controller.get().acceptWarnings(token, listOf(warning()))
            assertFalse(WarningMonitoring.ringing.value)
        } finally { controller.destroy() }
    }

    @Test fun `disabled notifications prevent monitoring`() {
        val manager = context.getSystemService(NotificationManager::class.java)
        shadowOf(manager).setNotificationsEnabled(false)
        assertFalse(WarningMonitoring.start(context))
        shadowOf(manager).setNotificationsEnabled(true)
    }

    @Test fun `transient network failure preserves ringing warning`() {
        val controller = Robolectric.buildService(WarningMonitoringService::class.java).create()
        try {
            val service = controller.get()
            service.acceptWarnings(token, listOf(warning()))
            service.warningSource = { Result.failure(java.io.IOException("Offline")) }
            service.onStartCommand(Intent(context, WarningMonitoringService::class.java), 0, 1)
            assertTrue(WarningMonitoring.ringing.value)
            assertEquals(1, WarningMonitoring.alert.value?.id)
            assertFalse(shadowOf(service).isStoppedBySelf)
        } finally { controller.destroy() }
    }

    @Test fun `inactive hike stops monitoring instead of replaying old warning`() {
        val controller = Robolectric.buildService(WarningMonitoringService::class.java).create()
        try {
            val service = controller.get()
            service.warningSource = { Result.failure(PeringatanDiniApiException(403, "Tidak ada pendakian aktif")) }
            service.onStartCommand(Intent(context, WarningMonitoringService::class.java), 0, 1)
            assertTrue(shadowOf(service).isStoppedBySelf)
        } finally { controller.destroy() }
        assertFalse(WarningMonitoring.ringing.value)
        assertFalse(WarningMonitoring.running.value)
    }
}
