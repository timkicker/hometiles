package dev.kicker.hometiles.notify

import android.content.ComponentName
import android.content.Context
import android.os.Process
import android.os.UserManager
import dev.kicker.hometiles.apps.AppKey
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
        val rows = runCatching { activeNotifications.orEmpty().map { it.toRow() } }.getOrDefault(emptyList())
        NotificationRepository.publish(NotificationCounts.summarise(rows), NotificationCounts.voicemail(rows))
    }

    /**
     * a notice from the work profile counts on the work app's tile only (issue #6). without the
     * profile both tiles of an app showed the sum of both.
     */
    private fun StatusBarNotification.profile(): Long? =
        if (user == Process.myUserHandle()) {
            null
        } else {
            runCatching { getSystemService(UserManager::class.java).getSerialNumberForUser(user) }.getOrNull()
        }

    private fun StatusBarNotification.toRow() = NotificationRow(
        packageName = AppKey.notices(packageName, profile()),
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
        channel = notification.channelId,
    )
}
