package org.biglau.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.biglau.R
import org.biglau.data.SmsConfig
import org.biglau.phone.PhoneNumbers
import org.biglau.ui.AppLocale

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

    /**
     * Das Praefix, an dem die alten Kanaele erkannt werden.
     *
     * Es steht hier **einmal** und wird beim Aufraeumen wiederverwendet. Stuende es dort als
     * Zeichenkette, wuerden die beiden Stellen eines Tages auseinanderlaufen: die Kennung
     * hiesse anders, das Aufraeumen fande nichts mehr, und in den Systemeinstellungen des
     * Nutzers sammelten sich stumme Kanaele, die keiner mehr benutzt. Niemand bekaeme davon
     * etwas mit - `SmsChannelTest` haelt beide Seiten zusammen.
     */
    const val CHANNEL_PREFIX = "sms-"

    fun channelId(vibrationMs: Int): String = "$CHANNEL_PREFIX$vibrationMs"

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
        // Texte in der Sprache der App, nicht der des Telefons. Siehe AppLocale.forApp.
        val texte = AppLocale.forApp(context)
        val kanal = channelId(config.vibrationMs)
        manager.notificationChannels
            .filter { it.id.startsWith(CHANNEL_PREFIX) && it.id != kanal }
            .forEach { manager.deleteNotificationChannel(it.id) }
        manager.createNotificationChannel(
            NotificationChannel(kanal, texte.getString(R.string.messages), NotificationManager.IMPORTANCE_HIGH).apply {
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

    /**
     * Eigener Kanal fuer den MMS-Hinweis - und einer je Vibrationsstaerke.
     *
     * **Ein Kanal ist nach dem Anlegen unveraenderlich.** `createNotificationChannel` auf
     * einen vorhandenen Kanal aendert nichts als Name und Beschreibung; Ton und Vibration
     * bleiben, wie sie beim ersten Mal waren. Am 03.09.2026 am Geraet gesehen: die
     * Einstellung stand auf 500 ms, der Kanal meldete `mVibrationEnabled=false`, weil er
     * aus einer frueheren Probe stammte.
     *
     * Der SMS-Weg loest das seit jeher, indem die Einstellung **in der Kennung** steckt und
     * alte Kanaele weggeraeumt werden. Der MMS-Hinweis macht es jetzt genauso. Der Praefix
     * ist ein anderer als [CHANNEL_PREFIX], sonst nimmt ihn die dortige Aufraeumschleife mit.
     */
    const val MMS_CHANNEL_PREFIX = "mms-hinweis-"

    fun mmsChannelId(vibrationMs: Int): String = "$MMS_CHANNEL_PREFIX$vibrationMs"

    /** Wie [MMS_CHANNEL_PREFIX]: eigener Kanal, damit keine Aufraeumschleife ihn mitnimmt. */
    const val FEHLER_CHANNEL = "sendefehler"

    /** Feste Kennung: ein zweites Bild soll den Hinweis ersetzen, nicht daneben legen. */
    const val MMS_NOTIFICATION_ID = 424242

    /** Eigene Nummer, damit ein Sendefehler den MMS-Hinweis nicht ueberschreibt. */
    const val FEHLER_NOTIFICATION_ID = 424243

    /**
     * Sagt Bescheid, dass eine Bildnachricht angekommen ist, die BigLau nicht zeigen kann.
     *
     * `PLAN.md` 6 verlangt es fuer MMS ausdruecklich: „bei Fehlschlag sichtbar an den Nutzer
     * melden statt still schlucken". Genau das tat der Empfaenger bisher - er merkte sich,
     * dass sich etwas geaendert hat, und schwieg. Wer ein Bild erwartet, wartet dann auf
     * etwas, das nie kommt, und haelt das Telefon fuer kaputt.
     *
     * Der Hinweis nennt auch den Grund (kein Netzzugang) und den Ausweg (die
     * Nachrichten-App des Telefons). Eine Meldung, die ein Problem nennt, ohne einen Weg zu
     * zeigen, ist nur die Haelfte.
     *
     * Der Kanal ist ein eigener und faengt **nicht** mit [CHANNEL_PREFIX] an: sonst raeumte
     * ihn die Aufraeumschleife in [show] beim naechsten SMS-Hinweis weg.
     */
    fun showMmsHint(context: Context, config: SmsConfig) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val texte = AppLocale.forApp(context)
        val kanal = mmsChannelId(config.vibrationMs)
        // Der Kanal hiess bis zum 03.09.2026 schlicht `mms-hinweis`, ohne die Staerke. Der
        // passt auf keinen Praefix und bliebe sonst fuer immer in den Systemeinstellungen
        // liegen - unter demselben Namen wie der neue.
        manager.notificationChannels
            .filter { (it.id.startsWith(MMS_CHANNEL_PREFIX) || it.id == "mms-hinweis") && it.id != kanal }
            .forEach { manager.deleteNotificationChannel(it.id) }
        manager.createNotificationChannel(
            NotificationChannel(
                kanal,
                texte.getString(R.string.mms_arrived_title),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                // Dieselbe Vibrationseinstellung wie bei einer SMS. Bis zum 03.09.2026 hing
                // dieser Kanal an keiner - wer die Vibration fuer Nachrichten ausgeschaltet
                // hatte, bekam sie bei einer Bildnachricht trotzdem. Eine Einstellung, die
                // nur fuer einen Teil der Nachrichten gilt, ist keine.
                enableVibration(config.vibrationMs > 0)
                if (config.vibrationMs > 0) {
                    vibrationPattern = longArrayOf(0, config.vibrationMs.toLong())
                }
            },
        )
        val oeffnen = Intent(context, SmsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        manager.notify(
            MMS_NOTIFICATION_ID,
            Notification.Builder(context, kanal)
                .setSmallIcon(R.drawable.ic_stat_message)
                .setContentTitle(texte.getString(R.string.mms_arrived_title))
                .setContentText(texte.getString(R.string.mms_arrived_body))
                .setStyle(
                    Notification.BigTextStyle().bigText(texte.getString(R.string.mms_arrived_body)),
                )
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        MMS_NOTIFICATION_ID,
                        oeffnen,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .build(),
        )
    }

    /**
     * Die Nachricht ist **nicht** hinausgegangen.
     *
     * Sie muss von selbst auffallen, denn niemand sieht nach: wer auf „Senden" getippt hat,
     * hat den Bildschirm meist schon verlassen. Eine Meldung im Gespraech waere nur zu
     * sehen, wenn man ohnehin hinschaut - und dann waere sie ueberfluessig.
     *
     * Eigener Kanal wie beim MMS-Hinweis und ohne [CHANNEL_PREFIX], damit die
     * Aufraeumschleife in [show] ihn nicht mitnimmt.
     */
    fun showSendFailed(context: Context, grund: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val texte = AppLocale.forApp(context)
        manager.createNotificationChannel(
            NotificationChannel(
                FEHLER_CHANNEL,
                texte.getString(R.string.sms_send_failed),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val oeffnen = Intent(context, SmsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        manager.notify(
            FEHLER_NOTIFICATION_ID,
            Notification.Builder(context, FEHLER_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_message)
                .setContentTitle(texte.getString(R.string.sms_send_failed))
                .setContentText(grund)
                .setStyle(Notification.BigTextStyle().bigText(grund))
                .setCategory(Notification.CATEGORY_ERROR)
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        FEHLER_NOTIFICATION_ID,
                        oeffnen,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .build(),
        )
    }

    /** Weggeräumt, sobald der Nutzer die Unterhaltung offen hat - er hat sie ja gesehen. */
    fun clear(context: Context, address: String) {
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(notificationId(address))
    }
}
