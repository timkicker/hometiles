package org.biglau.sms

/**
 * Die eigene, gerade gesendete Nachricht in die Datenbank schreiben.
 *
 * Android legt eine gesendete SMS **nur dann** von selbst ab, wenn die sendende App nicht
 * die Standard-App ist. Wer die Rolle hält, schreibt selbst - sonst verschwindet die eigene
 * Nachricht in dem Moment, in dem sie hinausgeht: die Unterhaltung zeigt dann nur noch die
 * Gegenseite, und man sieht nicht mehr, was man selbst geschrieben hat.
 *
 * Dieselbe Falle wie beim Empfangen, nur andersherum - siehe [SmsDelivery]. Beide Male ist
 * die Annahme „das System macht das schon" mit der Rolle hinfällig.
 *
 * Hier steht nur, **was** geschrieben wird. Ob überhaupt geschrieben werden darf, entscheidet
 * [SmsDelivery.mayWrite]; das Schreiben selbst macht [SmsActivity].
 */
object SmsOutbox {

    /** Die Spalten einer gesendeten Nachricht. Nur Zeichenketten und Zahlen, damit prüfbar. */
    fun values(address: String, body: String, timestamp: Long): Map<String, Any> = mapOf(
        "address" to address,
        "body" to body,
        "date" to timestamp,
        "date_sent" to timestamp,
        // Gelesen und gesehen: man hat sie ja selbst geschrieben. Stünde sie als ungelesen
        // da, zählte die eigene Nachricht in der Liste als neu - und die Erinnerung an
        // ungelesene Nachrichten erinnerte an die eigene.
        "read" to 1,
        "seen" to 1,
    )
}
