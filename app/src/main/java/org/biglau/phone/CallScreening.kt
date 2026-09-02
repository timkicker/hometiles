package org.biglau.phone

import android.telecom.Call
import android.telecom.CallScreeningService
import org.biglau.data.ConfigStore

/**
 * Gesperrte Nummern abweisen, **bevor** das Telefon klingelt.
 *
 * Bis hierher geschah das Abweisen in [BigInCallService.onCallAdded] - also erst, nachdem
 * Android den Anruf angenommen, den Klingelton gestartet und die Gespraechsansicht gebunden
 * hatte. Am Emulator nachgesehen: der Anruf wurde zwar abgewiesen, aber das System hatte
 * vorher schon geklingelt, und in der Anrufliste stand er als **abgelehnt** - so, als haette
 * der Nutzer ihn weggedrueckt. Wer eine Nummer sperrt, will genau das nicht sehen muessen.
 *
 * Dafuer gibt es diesen Dienst. Android fragt ihn, bevor irgendetwas geschieht; die Antwort
 * bestimmt, ob geklingelt wird, ob eine Meldung erscheint und wie der Anruf in der Liste
 * steht. Nur die Standard-Telefon-App wird gefragt - hat BigLau die Rolle nicht, bleibt es
 * beim Abweisen im Dienst, und deshalb steht das dort weiterhin.
 */
class CallScreening : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val nummer = callDetails.handle?.schemeSpecificPart.orEmpty()
        val gesperrt = CallBlocking.blocksIncoming(
            number = nummer,
            incoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING,
            blocked = ConfigStore.get(this).current.phone.blockedNumbers,
        )
        respondToCall(callDetails, antwort(gesperrt))
    }

    /**
     * Die Antwort an Android.
     *
     * `skipCallLog = false`: der Anruf **gehoert** in die Liste. Eine Sperre, die Anrufe
     * spurlos verschwinden laesst, ist nicht zu ueberpruefen - und wer eine Nummer
     * versehentlich sperrt, merkte es nie. Die Meldung dagegen faellt weg; das ist ja der
     * Sinn der Sache.
     */
    private fun antwort(gesperrt: Boolean): CallResponse = CallResponse.Builder()
        .setDisallowCall(gesperrt)
        .setRejectCall(gesperrt)
        .setSkipCallLog(false)
        .setSkipNotification(gesperrt)
        .build()
}
