package org.biglau.phone

import android.provider.CallLog

/**
 * Welche verpassten Anrufe noch niemand gesehen hat.
 *
 * Die Bedingung steht hier und nicht als Zeichenkette in der Abfrage, damit sie sich ohne
 * Telefon pruefen laesst - sie ist der Kern des Abzeichens auf der Kachel.
 *
 * **Zwei Bedingungen, und beide braucht es.** `NEW = 1` ist die Auskunft des Systems: wer
 * die Liste in der System-Telefon-App durchgeht, raeumt sie damit auch hier auf. Der
 * Zeitpunkt ist unsere eigene: BigLau darf die Anrufliste auf diesem Geraet nicht aendern
 * (`WRITE_CALL_LOG` ist nicht erteilt), und ohne ihn bliebe die Zahl fuer immer stehen.
 */
object MissedCalls {

    /** Die `WHERE`-Bedingung. */
    fun selection(): String =
        "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.NEW} = 1 AND ${CallLog.Calls.DATE} > ?"

    /** Die Werte dazu, in derselben Reihenfolge. */
    fun arguments(since: Long): Array<String> =
        arrayOf(CallLog.Calls.MISSED_TYPE.toString(), since.coerceAtLeast(0L).toString())

    /**
     * Der Zeitpunkt, der nach dem Lesen gespeichert wird.
     *
     * **Nicht „jetzt", sondern der juengste Anruf in der Liste.** Zwischen dem Auslesen der
     * Liste und dem Speichern vergeht Zeit; ein Anruf, der genau dazwischen kommt, waere
     * mit „jetzt" als gesehen abgehakt, ohne dass ihn jemand gesehen haette. Ist die Liste
     * leer, bleibt der alte Zeitpunkt stehen - es gab nichts zu sehen.
     */
    fun seenUpTo(previous: Long, entries: List<CallEntry>): Long =
        maxOf(previous, entries.maxOfOrNull { it.timestamp } ?: 0L)
}
