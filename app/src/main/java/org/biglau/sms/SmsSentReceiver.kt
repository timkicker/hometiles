package org.biglau.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import org.biglau.R

/**
 * Die Quittung des Netzes fuer eine gesendete Nachricht.
 *
 * `SmsManager.sendTextMessage` kehrt sofort zurueck; ob das Netz die Nachricht ueberhaupt
 * genommen hat, steht erst Sekunden spaeter fest und kommt als Rundruf hierher. Bis zum
 * 03.09.2026 wurde dafuer `null` uebergeben - BigLau sagte „gesendet", weil der Aufruf
 * keine Ausnahme geworfen hatte, und eine vom Netz abgelehnte Nachricht sah danach aus wie
 * jede andere.
 *
 * Fuer ein Telefon, auf das sich jemand verlaesst, ist das der schlimmere Fall von beiden:
 * nicht senden zu koennen ist ein Problem, aber zu glauben, man haette gesendet, ist ein
 * Problem, von dem man nichts weiss.
 */
class SmsSentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (resultCode == Activity.RESULT_OK) return

        // Die Zeile, um die es geht, steht in den Daten des Intents - so gefunden, wie sie
        // beim Schreiben entstanden ist.
        intent.data?.let { zeile ->
            runCatching {
                context.contentResolver.update(
                    zeile,
                    ContentValues().apply {
                        put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_FAILED)
                    },
                    null,
                    null,
                )
            }
        }
        SmsRepository.notifyChanged()
        SmsNotifications.showSendFailed(context, context.getString(grundText(resultCode)))
    }

    private companion object {

        /**
         * Warum es nicht ging - in Worten, die etwas nuetzen.
         *
         * „Allgemeiner Fehler" ist keine Auskunft. „Kein Empfang" und „Flugmodus" sind
         * welche: man kann etwas dagegen tun.
         */
        fun grundText(code: Int): Int = when (code) {
            SmsManager.RESULT_ERROR_NO_SERVICE -> R.string.sms_send_no_service
            SmsManager.RESULT_ERROR_RADIO_OFF -> R.string.sms_send_radio_off
            else -> R.string.sms_send_generic
        }
    }
}
