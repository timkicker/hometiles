package dev.kicker.hometiles.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.kicker.hometiles.R
import dev.kicker.hometiles.data.SmsConfig
import dev.kicker.hometiles.phone.PhoneNumbers
import dev.kicker.hometiles.ui.AppLocale

/**
 * the notice about a new message.
 *
 * it hangs on the sms role: whoever is the default app is also the one that says so.
 *
 * the vibration length from `PLAN.md` 4.7 sits in the *channel* and not in a vibrate call
 * of our own, because only then does the notice honour do-not-disturb and the silent
 * switch. a channel cannot be changed afterwards, so its id carries the length: another
 * setting gives a new channel and the old one is cleared away.
 */
object SmsNotifications {

    /** vibration lengths to choose from, in milliseconds. zero means no vibration. */
    val VIBRATION_CHOICES = listOf(0, 200, 500, 1000)

    /**
     * the prefix by which old channels are recognised, written once and reused when clearing
     * up: as a literal in both places they would drift apart, and silent unused channels
     * would pile up in the settings with nobody noticing. `SmsChannelTest` holds both sides.
     */
    const val CHANNEL_PREFIX = "sms-"

    fun channelId(vibrationMs: Int): String = "$CHANNEL_PREFIX$vibrationMs"

    /** what is filtered out does not announce itself, or hiding would only half work. */
    fun shouldNotify(message: SmsMessage, config: SmsConfig): Boolean =
        message.incoming && !SmsFilter.hidden(message, config.hiddenNumbers, config.hiddenWords)

    /** one id per sender: the second message replaces the first instead of stacking. */
    fun notificationId(address: String): Int =
        PhoneNumbers.clean(address).ifEmpty { address }.hashCode()

    fun show(context: Context, message: SmsMessage, name: String?, config: SmsConfig) {
        if (!shouldNotify(message, config)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        // texts in the app's language, not the phone's. see AppLocale.forApp.
        val texts = AppLocale.forApp(context)
        val channel = channelId(config.vibrationMs)
        manager.notificationChannels
            .filter { it.id.startsWith(CHANNEL_PREFIX) && it.id != channel }
            .forEach { manager.deleteNotificationChannel(it.id) }
        manager.createNotificationChannel(
            NotificationChannel(channel, texts.getString(R.string.messages), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(config.vibrationMs > 0)
                if (config.vibrationMs > 0) vibrationPattern = longArrayOf(0, config.vibrationMs.toLong())
            },
        )
        val open = Intent(context, SmsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(SmsActivity.EXTRA_ADDRESS, message.address)
        val fullScreen = Intent(open).putExtra(SmsActivity.EXTRA_FULL_SCREEN, true)
        val notification = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(name ?: PhoneNumbers.forDisplay(message.address))
            .setContentText(message.body)
            // the whole text, not one line of it: on three inches half a sentence fits.
            .setStyle(Notification.BigTextStyle().bigText(message.body))
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    notificationId(message.address),
                    open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            // `PLAN.md` 4.7: full screen on a new message. it leads to the same screen as
            // tapping does; a full-screen view of its own would be a second way there.
            .also { builder ->
                if (config.fullScreenAlert) {
                    builder.setFullScreenIntent(
                        PendingIntent.getActivity(
                            context,
                            notificationId(message.address) + 1,
                            fullScreen,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                        true,
                    )
                }
            }
            .build()
        manager.notify(notificationId(message.address), notification)
        // `PLAN.md` 4.7: again every n minutes while unread. at no reminder the same call
        // clears an old alarm away.
        MessageReminderReceiver.schedule(context, config.repeatMinutes)
    }

    /**
     * own channel for the mms hint, one per vibration length.
     *
     * a channel is immutable once created: `createNotificationChannel` on an existing one
     * changes nothing but name and description, so a channel from an earlier try reported
     * `mVibrationEnabled=false` while the setting said 500 ms. the setting therefore sits in
     * the id. a different prefix from [CHANNEL_PREFIX], or that clean-up loop takes it too.
     */
    const val MMS_CHANNEL_PREFIX = "mms-hinweis-"

    fun mmsChannelId(vibrationMs: Int): String = "$MMS_CHANNEL_PREFIX$vibrationMs"

    /** like [MMS_CHANNEL_PREFIX]: own channel, so no clean-up loop takes it along. */
    const val ERROR_CHANNEL = "sendefehler"

    /** fixed id: a second picture replaces the hint instead of lying beside it. */
    const val MMS_NOTIFICATION_ID = 424242

    /** a number of its own, so a send failure does not overwrite the mms hint. */
    const val ERROR_NOTIFICATION_ID = 424243

    /**
     * says that a picture message arrived which HomeTiles cannot show.
     *
     * `PLAN.md` 6 asks for it explicitly: report a failure visibly instead of swallowing it.
     * the hint names the reason (no network access) and the way out (the phone's messaging
     * app), because naming a problem without a way is only half of it.
     *
     * own channel, not starting with [CHANNEL_PREFIX], or [show] would clear it away.
     */
    fun showMmsHint(context: Context, config: SmsConfig) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val texts = AppLocale.forApp(context)
        val channel = mmsChannelId(config.vibrationMs)
        // the channel was once called plainly `mms-hinweis`, without the length. it matches
        // no prefix and would lie in the settings forever, under the same name as the new one.
        manager.notificationChannels
            .filter { (it.id.startsWith(MMS_CHANNEL_PREFIX) || it.id == "mms-hinweis") && it.id != channel }
            .forEach { manager.deleteNotificationChannel(it.id) }
        manager.createNotificationChannel(
            NotificationChannel(
                channel,
                texts.getString(R.string.mms_arrived_title),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                // the same vibration setting as for an sms: hanging on none, this channel
                // vibrated for a picture message even with vibration switched off, and a
                // setting that holds for only some messages is none.
                enableVibration(config.vibrationMs > 0)
                if (config.vibrationMs > 0) {
                    vibrationPattern = longArrayOf(0, config.vibrationMs.toLong())
                }
            },
        )
        val open = Intent(context, SmsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        manager.notify(
            MMS_NOTIFICATION_ID,
            Notification.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_stat_message)
                .setContentTitle(texts.getString(R.string.mms_arrived_title))
                .setContentText(texts.getString(R.string.mms_arrived_body))
                .setStyle(
                    Notification.BigTextStyle().bigText(texts.getString(R.string.mms_arrived_body)),
                )
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        MMS_NOTIFICATION_ID,
                        open,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .build(),
        )
    }

    /**
     * the message did *not* go out.
     *
     * it has to catch the eye by itself: whoever tapped send has usually left the screen,
     * and a notice inside the conversation would be seen only by someone already looking.
     *
     * own channel like the mms hint and without [CHANNEL_PREFIX], so [show] leaves it alone.
     */
    fun showSendFailed(context: Context, reason: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val texts = AppLocale.forApp(context)
        manager.createNotificationChannel(
            NotificationChannel(
                ERROR_CHANNEL,
                texts.getString(R.string.sms_send_failed),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val open = Intent(context, SmsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        manager.notify(
            ERROR_NOTIFICATION_ID,
            Notification.Builder(context, ERROR_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_message)
                .setContentTitle(texts.getString(R.string.sms_send_failed))
                .setContentText(reason)
                .setStyle(Notification.BigTextStyle().bigText(reason))
                .setCategory(Notification.CATEGORY_ERROR)
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        ERROR_NOTIFICATION_ID,
                        open,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .build(),
        )
    }

    /** cleared once the conversation is open: it has been seen. */
    fun clear(context: Context, address: String) {
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(notificationId(address))
    }
}
