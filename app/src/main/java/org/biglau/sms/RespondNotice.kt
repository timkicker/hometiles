package org.biglau.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.biglau.R
import org.biglau.phone.PhoneNumbers
import org.biglau.ui.AppLocale

/**
 * the notice for rejecting a call with a message.
 *
 * another app asked BigLau to send one; BigLau does not send it by itself (see
 * [org.biglau.sms.RespondViaMessageService]). so that this is not a silent nothing, the
 * request stands as a notice leading into the conversation with the text already in place.
 */
object RespondNotice {

    const val CHANNEL_ID = "respond-via-message"

    /** with no text from the other app the hint stands there: an empty notice is worse than none. */
    fun body(text: String, fallback: String): String = text.trim().ifEmpty { fallback }

    /** one id per number: a second request replaces the first. */
    fun notificationId(address: String): Int =
        ("respond" + PhoneNumbers.clean(address).ifEmpty { address }).hashCode()

    fun show(context: Context, address: String, text: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        // texts in the app's language, not the phone's. see AppLocale.forApp.
        val texts = AppLocale.forApp(context)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                texts.getString(R.string.messages),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val open = Intent(context, SmsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(SmsActivity.EXTRA_ADDRESS, address)
            .putExtra(SmsActivity.EXTRA_BODY, text)
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(texts.getString(R.string.respond_not_sent))
            .setContentText(body(text, texts.getString(R.string.respond_not_sent_hint)))
            .setStyle(
                Notification.BigTextStyle()
                    .bigText(body(text, texts.getString(R.string.respond_not_sent_hint))),
            )
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    notificationId(address),
                    open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
        manager.notify(notificationId(address), notification)
    }
}
