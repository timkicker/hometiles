package org.biglau.sms.probe

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import org.biglau.sms.SmsDeliverReceiver
import org.biglau.sms.WapPushDeliverReceiver

/**
 * Legt BigLau eine Nachricht hin, die nie durch ein Funknetz kam.
 *
 *     adb shell am start -n org.biglau.debug/org.biglau.sms.probe.ProbeNachrichtActivity
 *     adb shell am start -n ... -e text "Hallo" -e was raeumen
 *
 * **Warum der Empfaenger von Hand gerufen wird und nicht per Rundruf:**
 * `android.provider.Telephony.SMS_DELIVER` ist ein geschuetzter Rundruf - den darf nur das
 * System schicken, weder `adb` noch diese App. Der Aufruf von `onReceive` geht denselben
 * Weg ab dem ersten Schritt in BigLau: dieselbe Klasse, dasselbe Intent, dieselbe
 * Auswertung durch `Telephony.Sms.Intents.getMessagesFromIntent`.
 *
 * Was damit **nicht** geprueft ist: dass Android den Rundruf ueberhaupt an BigLau schickt.
 * Das zeigt erst eine echte Nachricht.
 */
class ProbeNachrichtActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.getStringExtra("was") == "raeumen") {
            raeumen()
            finish()
            return
        }

        // Der MMS-Hinweis. Der Empfaenger wertet die Bildnachricht gar nicht aus - er sagt
        // nur Bescheid, dass eine da ist, und schickt zur Nachrichten-App des Telefons.
        // Deshalb genuegt hier ein leeres Intent: geprueft wird der Hinweis, nicht die
        // Auswertung, die es nicht gibt.
        if (intent?.getStringExtra("was") == "mms") {
            Log.i("BigLau", "Probe: MMS-Hinweis")
            WapPushDeliverReceiver().onReceive(this, Intent())
            finish()
            return
        }

        val text = intent?.getStringExtra("text") ?: "Probenachricht ohne Funk"
        val pdu = ProbePdu.baue(ProbeNachricht.ABSENDER, text)
        val hinein = Intent(Telephony.Sms.Intents.SMS_DELIVER_ACTION).apply {
            putExtra("pdus", arrayOf<Any>(pdu))
            putExtra("format", "3gpp")
        }
        Log.i("BigLau", "Probenachricht: ${pdu.size} Bytes von ${ProbeNachricht.ABSENDER}")
        SmsDeliverReceiver().onReceive(this, hinein)
        finish()
    }

    /** Raeumt die Proben wieder weg - dieselbe Ueberlegung wie beim Probeanruf. */
    private fun raeumen() {
        val weg = runCatching {
            contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms.ADDRESS} LIKE ?",
                arrayOf("%" + ProbeNachricht.ABSENDER.takeLast(9)),
            )
        }.getOrDefault(-1)
        Log.i("BigLau", "Probenachrichten geraeumt: $weg")
    }
}

/** Wer die Probenachricht schickt. */
object ProbeNachricht {

    /**
     * Eine andere Nummer als beim Probeanruf, damit die beiden Proben in der Oberflaeche
     * nicht zu einem Gespraech verschmelzen. Derselbe reservierte Bereich.
     */
    const val ABSENDER = "+447700900124"
}
