package org.biglau.notify

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zaehlt aktive Benachrichtigungen pro Paket, damit Kacheln blinken koennen.
 * Braucht die vom Nutzer erteilte Benachrichtigungszugriff-Berechtigung.
 */
class BigNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        refresh()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val counts = runCatching {
            NotificationCounts.summarise(activeNotifications.orEmpty().map { it.toRow() })
        }.getOrDefault(emptyMap())
        NotificationRepository.publish(counts)
    }

    private fun StatusBarNotification.toRow() = NotificationRow(
        packageName = packageName,
        clearable = isClearable,
        ongoing = isOngoing,
        groupSummary = notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0,
        number = notification.number,
    )
}

object NotificationRepository {

    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    fun publish(value: Map<String, Int>) {
        _counts.value = value
    }

    fun countFor(packageName: String): Int = _counts.value[packageName] ?: 0

    fun isEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val us = ComponentName(context, BigNotificationListener::class.java)
        return flat.split(':').any {
            ComponentName.unflattenFromString(it)?.packageName == us.packageName
        }
    }
}
