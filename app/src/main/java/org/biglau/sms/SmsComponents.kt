package org.biglau.sms

import org.biglau.notify.RespondNotice
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony
import org.biglau.contacts.ContactRepository
import org.biglau.data.ConfigStore
import org.biglau.notify.SmsNotifications

/**
 * Die vier Pflichtkomponenten einer Standard-SMS-App.
 *
 * Android verlangt sie alle vier, sonst erscheint die App in der Auswahl der Standard-SMS-App
 * gar nicht - unabhaengig davon, ob sie funktionieren wuerde. Sie stehen deshalb zusammen in
 * einer Datei: wer eine davon loescht, sieht die anderen drei daneben und stutzt.
 *
 * 1. [SmsDeliverReceiver] - bekommt eingehende SMS
 * 2. [WapPushDeliverReceiver] - bekommt MMS-Benachrichtigungen
 * 3. [RespondViaMessageService] - "Anruf mit Nachricht ablehnen"
 * 4. Die SENDTO-Activity, siehe Manifest bei SmsActivity
 */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Wer die Rolle haelt, muss selbst speichern: SMS_DELIVER geht nur an die
        // Standard-App, und schreibt die nicht, hat die Nachricht niemand. Am Emulator
        // gesehen - Rolle genommen, Nachricht geschickt, und sie war nirgends.
        if (SmsDelivery.mayWrite(Telephony.Sms.getDefaultSmsPackage(context), context.packageName)) {
            speichern(context, intent)
        }
        SmsRepository.notifyChanged()
    }

    private fun speichern(context: Context, intent: Intent) {
        val teile = Telephony.Sms.Intents.getMessagesFromIntent(intent).orEmpty().mapNotNull { nachricht ->
            val absender = nachricht?.displayOriginatingAddress ?: return@mapNotNull null
            SmsDelivery.Part(
                address = absender,
                body = nachricht.displayMessageBody.orEmpty(),
                timestamp = nachricht.timestampMillis,
            )
        }
        // Schlaegt das Schreiben fehl, ist die Nachricht weg - aber ein Absturz im
        // Empfaenger nimmt zusaetzlich die App mit, und zwar bei jeder weiteren SMS.
        runCatching {
            SmsDelivery.merge(teile).forEach { ganz ->
                melden(context, ganz)
                context.contentResolver.insert(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, ganz.address)
                        put(Telephony.Sms.BODY, ganz.body)
                        // DATE ist die Ankunft hier, DATE_SENT der Stempel des Netzes.
                        // Am Emulator stand eine gerade eingegangene Nachricht sonst mit
                        // 14:46 in der Liste, waehrend es 13:47 war: der Stempel des
                        // Absendernetzes muss nicht zur Uhr dieses Geraets passen, und die
                        // Liste sortiert nach der Ankunft.
                        put(Telephony.Sms.DATE, System.currentTimeMillis())
                        put(Telephony.Sms.DATE_SENT, ganz.timestamp)
                        // Ungelesen und ungesehen: die Nachricht ist gerade erst gekommen.
                        put(Telephony.Sms.READ, 0)
                        put(Telephony.Sms.SEEN, 0)
                    },
                )
            }
        }
    }
}

/**
 * Sagt Bescheid. Ohne das laege die Nachricht in der Datenbank und niemand wuesste davon,
 * bis er von sich aus die Liste oeffnet - fuer ein Telefon in der Tasche dasselbe wie
 * verloren.
 */
private fun melden(context: Context, ganz: SmsDelivery.Incoming) {
    val config = ConfigStore.get(context).current.sms
    SmsNotifications.show(
        context,
        SmsMessage(
            id = 0,
            threadId = 0,
            address = ganz.address,
            body = ganz.body,
            timestamp = ganz.timestamp,
            incoming = true,
            read = false,
        ),
        name = ContactRepository.get(context).nameFor(ganz.address),
        config = config,
    )
}

class WapPushDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SmsRepository.notifyChanged()
    }
}

/**
 * „Anruf mit Nachricht ablehnen" - der Weg, auf dem eine andere App uns bittet, eine
 * Nachricht zu schicken.
 *
 * **Hier stand ein stummer Leerlauf:** der Dienst nahm die Bitte an, tat nichts und hielt
 * sich fuer fertig. Wer im System-Dialer „Kann jetzt nicht sprechen" antippte, bekam keine
 * Fehlermeldung - und der Anrufer bekam keine Nachricht. Das ist die schlimmste Sorte
 * Fehler in dieser App: eine, die aussieht wie Erfolg.
 *
 * BigLau sendet hier **nicht von sich aus**. Eine Nachricht, die eine fremde App auslöst
 * und die niemand mehr zu sehen bekommt, waere genau das Gegenteil dessen, was diese App
 * verspricht. Stattdessen fuehrt eine Meldung in die Unterhaltung, mit dem Text schon im
 * Feld - abschicken tut der Mensch, dem das Telefon gehoert.
 */
class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nummer = intent?.data?.schemeSpecificPart.orEmpty()
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        RespondNotice.show(this, nummer, text)
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
