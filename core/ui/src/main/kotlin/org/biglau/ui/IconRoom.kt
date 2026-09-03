package org.biglau.ui

import org.biglau.data.IconVisibility

/**
 * Ob auf dieser Kachel ein Symbol Platz hat. PLAN.md 4.2 „nur wenn Platz".
 *
 * Die Frage ist nicht, ob das Symbol irgendwie hineinpasst - [iconSizeDp] quetscht es
 * notfalls klein genug. Die Frage ist, ob es dafuer gequetscht werden musste. Wurde es
 * das, steht auf der Kachel ein Symbol in einer Groesse, die niemand mehr erkennt, und es
 * kostet trotzdem die halbe Hoehe. Dann lieber nur das Wort, gross und lesbar.
 *
 * Mit den Vorgaben greift das selten - die Zellmasse dieser App lassen dem Symbol Platz.
 * Es ist ein Ventil fuer die Faelle, in denen jemand das Raster dicht stellt oder die
 * Symbole gross: dann schuetzt es die Beschriftung, statt beides unleserlich zu machen.
 */
object IconRoom {

    /**
     * @param desiredDp die Groesse, die der eingestellte Anteil ergibt
     * @param fittedDp  was davon neben der Beschriftung uebrig bleibt
     */
    fun show(visibility: IconVisibility, desiredDp: Float, fittedDp: Float): Boolean =
        when (visibility) {
            IconVisibility.ALWAYS -> true
            IconVisibility.NEVER -> false
            IconVisibility.IF_ROOM -> fittedDp >= desiredDp
        }
}
