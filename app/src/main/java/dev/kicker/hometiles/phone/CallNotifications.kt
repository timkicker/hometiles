package dev.kicker.hometiles.phone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.kicker.hometiles.R
import dev.kicker.hometiles.ui.AppLocale

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

    /**
     * the same notice while the call screen is in front. on the loud channel it lay over the
     * screen as a banner and hid the caller's name (emulator, 25.10.2026, issue #2); here it
     * waits quietly in the shade with its buttons.
     */
    const val CHANNEL_SHOWN = "call-shown"

    /** one id for the ringing call: a second call replaces the notice instead of stacking. */
    const val NOTIFICATION_ID = 1

    /** who is ringing, so the notice can move between the channels. null when nothing rings. */
    private var ringing: Pair<String?, String>? = null

    /** whether the call screen is in front; set by InCallActivity. */
    private var screenShown = false

    fun showIncoming(context: Context, name: String?, number: String) {
        ringing = name to number
        post(context)
    }

    /** InCallActivity moves the ringing notice between loud and quiet with this. */
    fun screenShown(context: Context, shown: Boolean) {
        screenShown = shown
        if (ringing != null) post(context)
    }

    private fun post(context: Context) {
        val (name, number) = ringing ?: return
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
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SHOWN,
                texts.getString(R.string.phone),
                NotificationManager.IMPORTANCE_LOW,
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
        val builder = Notification.Builder(context, if (screenShown) CHANNEL_SHOWN else CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle(name ?: PhoneNumbers.forDisplay(number).ifBlank {
                texts.getString(R.string.call_unknown)
            })
            .setCategory(Notification.CATEGORY_CALL)
            // not swipeable while it rings: the way to the call screen must not be losable.
            .setOngoing(true)
            .setContentIntent(screen)
            // with the screen on the full screen intent stays a small notice. without buttons
            // the call could not be taken from there at all. see CallActionReceiver.
            .addAction(action(context, CallActionReceiver.ANSWER, texts.getString(R.string.incall_answer)))
            .addAction(action(context, CallActionReceiver.REJECT, texts.getString(R.string.incall_reject)))
        if (!screenShown) builder.setFullScreenIntent(screen, true)
        // a channel cannot change under a posted notice, and a banner already standing stays
        // up through an update. so away first, then anew.
        manager.cancel(NOTIFICATION_ID)
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun action(context: Context, what: String, label: String): Notification.Action {
        val intent = PendingIntent.getBroadcast(
            context,
            what.hashCode(),
            Intent(context, CallActionReceiver::class.java).setAction(what),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(null, label, intent).build()
    }

    fun clear(context: Context) {
        ringing = null
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }
}
