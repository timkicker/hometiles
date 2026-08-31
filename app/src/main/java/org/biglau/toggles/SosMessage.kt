package org.biglau.toggles

/**
 * Der Text der Notruf-SMS.
 *
 * Getrennt vom Versand, weil hier zwei Dinge schieflaufen koennen, die man nicht am Geraet
 * ausprobieren will: eine Nachricht ohne Standort, die so aussieht als haette sie einen,
 * und Koordinaten in einer Schreibweise, die der Empfaenger nicht anklicken kann.
 */
object SosMessage {

    /**
     * Der Ersatztext wird uebergeben, nicht hier festgeschrieben: er muss in der Sprache
     * des Telefons stehen. Ein deutscher Satz auf einem englischen Geraet ist im Notfall
     * genau die falsche Ueberraschung.
     */
    fun compose(text: String, latitude: Double?, longitude: Double?, fallback: String): String {
        val body = text.trim().ifEmpty { fallback.trim() }
        if (latitude == null || longitude == null) return body
        return "$body\n" + mapsLink(latitude, longitude)
    }

    /** Ein Link, den jede Karten-App oeffnet - keine App-eigene Schreibweise. */
    fun mapsLink(latitude: Double, longitude: Double): String =
        "https://maps.google.com/?q=%.5f,%.5f".format(java.util.Locale.US, latitude, longitude)

    /**
     * Wie viele SMS der Text braucht. Ueber 160 Zeichen wird geteilt, und jede Teil-SMS
     * kostet - bei einer Notrufkette an drei Nummern summiert sich das.
     */
    fun partsNeeded(message: String): Int {
        if (message.isEmpty()) return 1
        val perPart = if (message.length <= 160) 160 else 153
        return ((message.length + perPart - 1) / perPart).coerceAtLeast(1)
    }
}
