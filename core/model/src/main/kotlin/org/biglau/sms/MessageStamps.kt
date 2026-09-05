package org.biglau.sms

import java.util.Calendar
import java.util.Date

/**
 * the time under every message, the day above the first message of that day.
 *
 * a date on every line costs one line per message on three inches.
 */
object MessageStamps {

    fun startsNewDay(previousMillis: Long?, millis: Long): Boolean {
        if (previousMillis == null) return true
        return !sameDay(previousMillis, millis)
    }

    fun sameDay(a: Long, b: Long): Boolean {
        val first = Calendar.getInstance().apply { time = Date(a) }
        val second = Calendar.getInstance().apply { time = Date(b) }
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }
}
