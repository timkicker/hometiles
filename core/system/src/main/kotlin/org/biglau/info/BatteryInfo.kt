package org.biglau.info

/** raw values as ACTION_BATTERY_CHANGED delivers them. */
data class BatteryReading(
    val level: Int,
    val scale: Int,
    val status: Int,
    val plugged: Int,
)

/**
 * turns the raw values into something a tile can show.
 *
 * the scale is not always 100 - some devices report 255. missing that shows "47 %" on a
 * half-full battery.
 */
object BatteryInfo {

    // from android.os.BatteryManager, repeated here to keep this file free of android.
    const val STATUS_CHARGING = 2
    const val STATUS_FULL = 5

    fun percent(level: Int, scale: Int): Int? {
        if (level < 0 || scale <= 0) return null
        return ((level.toFloat() / scale) * 100f).toInt().coerceIn(0, 100)
    }

    fun percent(reading: BatteryReading): Int? = percent(reading.level, reading.scale)

    fun isCharging(status: Int, plugged: Int): Boolean =
        status == STATUS_CHARGING || status == STATUS_FULL || plugged > 0

    fun isCharging(reading: BatteryReading): Boolean = isCharging(reading.status, reading.plugged)

    fun isLow(percent: Int?): Boolean = percent != null && percent <= 15

    /** null with an unknown level, so the tile shows a question mark instead of an empty bar. */
    fun fraction(percent: Int?): Float? = percent?.let { it / 100f }
}
