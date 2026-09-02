package org.biglau.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.biglau.R
import org.biglau.data.SmsConfig
import org.biglau.phone.PhoneNumbers
import org.biglau.sms.SmsActivity
import org.biglau.sms.SmsFilter
import org.biglau.sms.SmsMessage

/**
 * Die Meldung über eine neue Nachricht.
 *
 * Sie hängt an der SMS-Rolle: wer die Standard-App ist, ist auch die Stelle, die es sagt.
 * Vorher sagte es niemand - am Emulator kam eine Nachricht an, wurde gespeichert und blieb
 * unsichtbar, bis jemand von sich aus die Liste öffnete. Für ein Telefon, das in der Tasche
 * liegt, ist das dasselbe wie verloren.
 *
 * Die Vibrationsdauer aus `PLAN.md` 4.7 steht im **Kanal**, nicht in einem eigenen
 * Vibrationsaufruf: nur so hält sich die Meldung an „Bitte nicht stören" und an den
 * Lautlos-Schalter. Ein Kanal lässt sich nachträglich nicht ändern, deshalb trägt seine
 * Kennung die Dauer - eine andere Einstellung ergibt einen neuen Kanal, und der alte wird
 * weggeräumt.
 */
object SmsNotifications {

    /** Vibrationsdauern zur Wahl, in Millisekunden. Null heisst: nicht vibrieren. */
    val VIBRATION_CHOICES = listOf(0, 200, 500, 1000)

    fun channelId(vibrationMs: Int): String = "sms-$vibrationMs"

    /**
     * Wird über diese Nachricht überhaupt gemeldet?
     *
     * Was ausgefiltert ist, meldet sich auch nicht - sonst hätte das Ausblenden nur die
     * halbe Wirkung und die Werbenachricht klingelte weiter.
     */
    fun shouldNotify(message: SmsMessage, config: SmsConfig): Boolean =
        message.incoming && !SmsFilter.hidden(message, config.hiddenNumbers, config.hiddenWords)

    /** Eine Kennung je Absender: die zweite Nachricht ersetzt die erste, statt sich zu stapeln. */
    fun notificationId(address: String): Int =
        PhoneNumbers.clean(address).ifEmpty { address }.hashCode()

    fun show(context: Context, message: SmsMessage, name: String?, config: SmsConfig) {
        if (!shouldNotify(message, config)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val kanal = channelId(config.vibrationMs)
        manager.notificationChannels
            .filter { it.id.startsWith("sms-") && it.id != kanal }
            .forEach { manager.deleteNotificationChannel(it.id) }
        manager.createNotificationChannel(
            NotificationChannel(kanal, context.getString(R.string.messages), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(config.vibrationMs > 0)
                if (config.vibrationMs > 0) vibrationPattern = longArrayOf(0, config.vibrationMs.toLong())
            },
        )
        val oeffnen = Intent(context, SmsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(SmsActivity.EXTRA_ADDRESS, message.address)
        val vollbild = Intent(oeffnen).putExtra(SmsActivity.EXTRA_FULL_SCREEN, true)
        val notification = Notification.Builder(context, kanal)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(name ?: PhoneNumbers.forDisplay(message.address))
            .setContentText(message.body)
            // Der ganze Text, nicht eine Zeile davon: auf drei Zoll passt sonst der halbe
            // Satz, und man muss die App oeffnen, um das Ende zu lesen.
            .setStyle(Notification.BigTextStyle().bigText(message.body))
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    notificationId(message.address),
                    oeffnen,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            // PLAN.md 4.7: Vollbild bei neuer Nachricht. Es fuehrt auf denselben
            // Bildschirm wie das Antippen - eine eigene Vollbild-Ansicht daneben waere
            // ein zweiter Weg zur selben Nachricht.
            .also { bauer ->
                if (config.fullScreenAlert) {
                    bauer.setFullScreenIntent(
                        PendingIntent.getActivity(
                            context,
                            notificationId(message.address) + 1,
                            vollbild,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                        true,
                    )
                }
            }
            .build()
        manager.notify(notificationId(message.address), notification)
        // PLAN.md 4.7: alle n Minuten noch einmal, solange sie ungelesen ist. Bei "nicht
        // erinnern" raeumt derselbe Aufruf einen alten Wecker weg.
        MessageReminderReceiver.schedule(context, config.repeatMinutes)
    }

    /** Weggeräumt, sobald der Nutzer die Unterhaltung offen hat - er hat sie ja gesehen. */
    fun clear(context: Context, address: String) {
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(notificationId(address))
    }
}
