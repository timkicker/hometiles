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
 * Die Meldung zu „Anruf mit Nachricht ablehnen".
 *
 * Eine andere App hat BigLau gebeten, eine Nachricht zu schicken. BigLau schickt sie
 * **nicht von sich aus** — siehe [org.biglau.sms.RespondViaMessageService]. Damit das kein
 * stummes Nichts bleibt, steht die Bitte als Meldung da und führt in die Unterhaltung, den
 * Text schon im Feld.
 */
object RespondNotice {

    const val CHANNEL_ID = "respond-via-message"

    /**
     * Was in der Meldung steht.
     *
     * Ohne Text hätte die andere App nichts mitgegeben; dann steht dort der Hinweis statt
     * einer leeren Zeile — eine Meldung ohne Inhalt ist schlimmer als keine.
     */
    fun body(text: String, fallback: String): String = text.trim().ifEmpty { fallback }

    /** Eine Kennung je Nummer: eine zweite Bitte ersetzt die erste. */
    fun notificationId(address: String): Int =
        ("respond" + PhoneNumbers.clean(address).ifEmpty { address }).hashCode()

    fun show(context: Context, address: String, text: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        // Texte in der Sprache der App, nicht der des Telefons. Siehe AppLocale.forApp.
        val texte = AppLocale.forApp(context)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                texte.getString(R.string.messages),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val oeffnen = Intent(context, SmsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(SmsActivity.EXTRA_ADDRESS, address)
            .putExtra(SmsActivity.EXTRA_BODY, text)
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(texte.getString(R.string.respond_not_sent))
            .setContentText(body(text, texte.getString(R.string.respond_not_sent_hint)))
            .setStyle(
                Notification.BigTextStyle()
                    .bigText(body(text, texte.getString(R.string.respond_not_sent_hint))),
            )
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    notificationId(address),
                    oeffnen,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
        manager.notify(notificationId(address), notification)
    }
}
