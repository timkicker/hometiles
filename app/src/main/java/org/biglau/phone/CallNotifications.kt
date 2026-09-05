package org.biglau.phone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.biglau.R
import org.biglau.ui.AppLocale

/**
 * the notice that carries the incoming call screen.
 *
 * why a notice at all, when the service could start the activity itself: from android 10 on a
 * background start is refused, and an incall service runs in the background exactly when it
 * matters - screen off, phone in a pocket. then the call screen would simply not appear. a
 * full screen intent is the way android grants for this, and it also works on a locked screen.
 *
 * played through on this device on 05.09.2026 with the probe call and the screen asleep: the
 * notice stood on channel `call-incoming` with a fullscreenIntent and importance HIGH, and
 * `InCallActivity` was the resumed activity. after hanging up no notice was left.
 */
object CallNotifications {

    /**
     * the channel lies on the device and keeps its id.
     *
     * the prefix must not be `sms-`: SmsNotifications deletes every channel starting with that
     * when the vibration setting changes, and would sweep this one away with it.
     */
    const val CHANNEL = "call-incoming"

    /** one id for the ringing call: a second call replaces the notice instead of stacking. */
    const val NOTIFICATION_ID = 1

    fun showIncoming(context: Context, name: String?, number: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        // texts in the app's language, not the phone's. see AppLocale.forApp.
        val texts = AppLocale.forApp(context)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                texts.getString(R.string.phone),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
        val open = Intent(context, InCallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val screen = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(name ?: PhoneNumbers.forDisplay(number).ifBlank {
                texts.getString(R.string.call_unknown)
            })
            .setCategory(Notification.CATEGORY_CALL)
            // not swipeable while it rings: the way to the call screen must not be losable.
            .setOngoing(true)
            .setContentIntent(screen)
            .setFullScreenIntent(screen, true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun clear(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }
}
