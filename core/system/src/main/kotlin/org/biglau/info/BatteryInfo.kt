package org.biglau.info

/** Rohwerte, wie sie ACTION_BATTERY_CHANGED liefert. */
data class BatteryReading(
    val level: Int,
    val scale: Int,
    val status: Int,
    val plugged: Int,
)

/**
 * Rechnet die Rohwerte in etwas um, das auf einer Kachel stehen kann.
 *
 * Die Skala ist nicht immer 100 - manche Geraete melden 255. Wer das uebersieht, zeigt
 * "47 %" bei halbvollem Akku und "12 %" bei fast vollem.
 */
object BatteryInfo {

    // Werte aus android.os.BatteryManager, hier ohne Android-Abhaengigkeit
    const val STATUS_CHARGING = 2
    const val STATUS_FULL = 5

    /** Ladestand in Prozent, oder null wenn die Werte unbrauchbar sind. */
    fun percent(level: Int, scale: Int): Int? {
        if (level < 0 || scale <= 0) return null
        return ((level.toFloat() / scale) * 100f).toInt().coerceIn(0, 100)
    }

    fun percent(reading: BatteryReading): Int? = percent(reading.level, reading.scale)

    fun isCharging(status: Int, plugged: Int): Boolean =
        status == STATUS_CHARGING || status == STATUS_FULL || plugged > 0

    fun isCharging(reading: BatteryReading): Boolean = isCharging(reading.status, reading.plugged)

    /** Ab wann die Anzeige warnen soll. */
    fun isLow(percent: Int?): Boolean = percent != null && percent <= 15

    /**
     * Fuellstand als Anteil zwischen 0 und 1 fuer die Balkendarstellung.
     * Bei unbekanntem Stand null - dann zeigt die Kachel ein Fragezeichen statt eines
     * leeren Balkens, der wie "leer" aussaehe.
     */
    fun fraction(percent: Int?): Float? = percent?.let { it / 100f }
}
