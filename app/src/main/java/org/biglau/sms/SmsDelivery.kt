package org.biglau.sms

/**
 * Was mit einer eingehenden SMS geschieht, sobald BigLau die SMS-Rolle hält.
 *
 * **Der Fall, der hier verhindert wird, ist Verlust.** Android stellt `SMS_DELIVER` nur der
 * Standard-SMS-App zu, und mit der Rolle kommt die Pflicht, die Nachricht selbst in die
 * Anbieter-Datenbank zu schreiben - sonst hat sie niemand. Am Emulator nachgestellt: Rolle
 * genommen, Nachricht geschickt, und sie war **nirgends**. Weder in der Liste noch in der
 * Datenbank. Der alte Empfänger meldete nur „es hat sich etwas geändert" und verließ sich
 * darauf, dass die vorherige Standard-App mitschreibt - was genau dann nicht mehr stimmt,
 * wenn jemand BigLau zur Standard-App macht.
 *
 * Hier steht nur das Rechnen; das Schreiben macht [SmsDeliverReceiver] mit den
 * Android-Klassen. So bleibt der Teil prüfbar, an dem etwas verlorengehen kann.
 */
object SmsDelivery {

    /** Ein Stück einer Nachricht, wie es aus dem Funkweg kommt. */
    data class Part(val address: String, val body: String, val timestamp: Long)

    /** Eine ganze Nachricht, so wie sie in der Datenbank stehen soll. */
    data class Incoming(val address: String, val body: String, val timestamp: Long)

    /**
     * Fügt die Teile einer langen SMS wieder zusammen.
     *
     * Über 160 Zeichen kommt eine Nachricht in mehreren Teilen an. Jeden einzeln zu
     * speichern ergäbe drei halbe Nachrichten hintereinander statt einer ganzen. Der
     * Zeitstempel ist der des ersten Teils - der Zeitpunkt, zu dem der Absender geschrieben
     * hat, nicht der, zu dem der letzte Teil ankam.
     */
    fun merge(parts: List<Part>): List<Incoming> {
        if (parts.isEmpty()) return emptyList()
        val zusammen = mutableListOf<Incoming>()
        parts.forEach { teil ->
            val letzte = zusammen.lastOrNull()
            if (letzte != null && letzte.address == teil.address) {
                zusammen[zusammen.lastIndex] = letzte.copy(body = letzte.body + teil.body)
            } else {
                zusammen += Incoming(teil.address, teil.body, teil.timestamp)
            }
        }
        return zusammen
    }

    /**
     * Darf BigLau schreiben?
     *
     * Nur als Standard-App. Schriebe es daneben mit, stünde jede Nachricht zweimal in der
     * Datenbank - einmal von der echten Standard-App, einmal von uns.
     */
    fun mayWrite(defaultSmsPackage: String?, ownPackage: String): Boolean =
        defaultSmsPackage != null && defaultSmsPackage == ownPackage
}
