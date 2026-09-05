package org.biglau.notify

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * counts active notifications per package so tiles can blink. needs the notification access
 * the user grants by hand.
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
        category = notification.category,
        // the template lives in the extras, not in a flag: a paused media notification is
        // no longer `ongoing` but still carries the template.
        mediaStyle = notification.extras
            ?.getString("android.template")
            ?.endsWith("MediaStyle") == true,
    )
}
