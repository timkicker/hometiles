package org.biglau.sms

import java.util.Calendar
import java.util.Date

/**
 * Wann eine Nachricht kam - und wann das Datum dazugehoert.
 *
 * In der Unterhaltung stand bisher gar keine Zeit. Wer eine Nachricht liest, will wissen,
 * ob sie von eben ist oder von letzter Woche; ohne Zeitangabe muss man raten, und bei
 * einer Nachricht wie „bin unterwegs" ist das der ganze Unterschied.
 *
 * Unter jeder Nachricht steht deshalb die Uhrzeit, und ueber der ersten eines Tages der
 * Tag. Das Datum an jede Zeile zu haengen kostet auf drei Zoll eine Zeile pro Nachricht
 * und sagt dreimal dasselbe.
 */
object MessageStamps {

    /** Braucht diese Nachricht eine Tagesueberschrift? */
    fun startsNewDay(previousMillis: Long?, millis: Long): Boolean {
        if (previousMillis == null) return true
        return !sameDay(previousMillis, millis)
    }

    fun sameDay(a: Long, b: Long): Boolean {
        val erste = Calendar.getInstance().apply { time = Date(a) }
        val zweite = Calendar.getInstance().apply { time = Date(b) }
        return erste.get(Calendar.YEAR) == zweite.get(Calendar.YEAR) &&
            erste.get(Calendar.DAY_OF_YEAR) == zweite.get(Calendar.DAY_OF_YEAR)
    }
}
