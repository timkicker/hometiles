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
    fun compose(
        text: String,
        latitude: Double?,
        longitude: Double?,
        fallback: String,
        /**
         * Steht dabei, wenn der Standort **alt** ist - etwa „Standort von vor 3 Stunden".
         *
         * Ein Standort ohne Alter liest sich wie „hier ist er jetzt". Ist er in Wahrheit von
         * gestern, faehrt die Hilfe an den falschen Ort und sucht dort. Alt und beschriftet
         * ist besser als gar keiner: es bleibt ein Anhaltspunkt.
         */
        ageNote: String? = null,
    ): String {
        val body = text.trim().ifEmpty { fallback.trim() }
        if (latitude == null || longitude == null) return body
        val link = mapsLink(latitude, longitude)
        return listOfNotNull(body, link, ageNote?.trim()?.ifEmpty { null }).joinToString("\n")
    }

    /**
     * Ab wann ein Standort als alt gilt.
     *
     * Fuenf Minuten: kuerzer waere Rauschen (jede Positionsbestimmung ist ein paar Sekunden
     * alt), laenger hiesse, eine viertelstundenalte Position als aktuell auszugeben.
     */
    const val AGE_THRESHOLD_MINUTES = 5L

    /**
     * Muss das Alter dabeistehen? Null heisst: kein Standort oder frisch genug.
     *
     * Gibt Minuten oder Stunden zurueck - was der Empfaenger im Kopf leichter einordnet.
     */
    fun ageNote(minutes: Long?): Pair<AgeUnit, Int>? {
        if (minutes == null || minutes < AGE_THRESHOLD_MINUTES) return null
        if (minutes < 120) return AgeUnit.MINUTES to minutes.toInt()
        return AgeUnit.HOURS to (minutes / 60).toInt()
    }

    enum class AgeUnit { MINUTES, HOURS }

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
