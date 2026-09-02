package org.biglau.info

/** Rohwerte, wie sie das Telefonienetz liefert. */
data class SignalReading(
    /** 0 bis 4, wie `SignalStrength.getLevel()`; -1 heißt unbekannt. */
    val level: Int,
    /**
     * Ob BigLau überhaupt nachsehen darf. Ohne `READ_PHONE_STATE` weiß es nichts - und
     * „weiß nichts" ist etwas anderes als „keine Karte". Das zu verwechseln hieße, dem
     * Nutzer eine fehlende SIM zu melden, während sie steckt.
     */
    val mayRead: Boolean = true,
    val hasSim: Boolean,
    val inService: Boolean,
    val roaming: Boolean,
    /** „4G", „LTE", „3G" … oder leer, wenn das Netz nichts meldet. */
    val networkType: String,
)

/**
 * Was auf einer Signalkachel steht.
 *
 * Vier Zustände, die auseinandergehalten werden müssen, weil sie verschiedene Handlungen
 * nach sich ziehen: keine Karte (Karte einlegen), kein Netz (woandershin gehen), Netz mit
 * schwachem Empfang (näher ans Fenster), und Empfang in Ordnung. Ein einzelner Balken, der
 * bei allen dreien leer bleibt, sagt dem Nutzer nicht, was er tun soll.
 */
object SignalInfo {

    const val MAX_LEVEL = 4

    enum class State { NO_PERMISSION, NO_SIM, NO_SERVICE, WEAK, OK }

    fun stateOf(reading: SignalReading): State = when {
        !reading.mayRead -> State.NO_PERMISSION
        !reading.hasSim -> State.NO_SIM
        !reading.inService -> State.NO_SERVICE
        reading.level <= 1 -> State.WEAK
        else -> State.OK
    }

    /** Wie viele Balken zu füllen sind, 0 bis 4. Unbekannt zählt als leer. */
    fun bars(reading: SignalReading): Int =
        if (!reading.mayRead || !reading.hasSim || !reading.inService) {
            0
        } else {
            reading.level.coerceIn(0, MAX_LEVEL)
        }

    /**
     * Der Zusatz neben den Balken: die Netzart, bei Roaming mit einem vorangestellten „R".
     *
     * Roaming gehört auf die Kachel, weil es Geld kostet - und weil man es in der winzigen
     * Systemleiste eines Drei-Zoll-Geräts leicht übersieht.
     */
    fun caption(reading: SignalReading): String {
        val art = reading.networkType.trim()
        return when {
            !reading.mayRead || !reading.hasSim || !reading.inService -> ""
            reading.roaming && art.isNotEmpty() -> "R $art"
            reading.roaming -> "R"
            else -> art
        }
    }
}
