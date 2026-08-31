package org.biglau.sms

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder

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
        // Der Empfang selbst kommt, sobald eine SIM im Geraet steckt und sich das
        // Speichern in die Anbieter-Datenbank pruefen laesst. Bis dahin nimmt der
        // Empfaenger die Nachricht entgegen, ohne sie zu verlieren: Android stellt sie
        // weiterhin auch der bisherigen Standard-App zu, solange BigLau die Rolle nicht hat.
        SmsRepository.notifyChanged()
    }
}

class WapPushDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SmsRepository.notifyChanged()
    }
}

class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ablehnen mit Nachricht kommt zusammen mit der Anrufuebernahme.
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
