package org.biglau.notify

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

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
