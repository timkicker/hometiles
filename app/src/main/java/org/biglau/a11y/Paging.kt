package org.biglau.a11y

/**
 * Blättern in langen Listen ohne Wischen.
 *
 * Wischen setzt eine ruhige Hand voraus; wer zittert, schiebt die Liste entweder gar nicht
 * oder gleich um dreißig Einträge weiter. Zwei große Knöpfe blättern stattdessen um genau
 * eine Seite - mit **einer Zeile Überlappung**, damit beim Sprung keine Zeile übersprungen
 * wird. Eine übersprungene Zeile ist auf einer Kontaktliste nicht bloß unschön: der gesuchte
 * Name ist dann einfach nicht da.
 */
object Paging {

    /** Eine Listenzeile, so wie sie gerade auf dem Schirm liegt. */
    data class Row(val index: Int, val offset: Int, val size: Int)

    /**
     * Nur **ganz** sichtbare Zeilen zählen. Am oberen und unteren Rand hängt fast immer eine
     * angeschnittene Zeile; zählte man sie mit, wäre der Sprung eine Zeile zu weit und genau
     * die angeschnittene ginge verloren - unsichtbar, weil man sie ja nie ganz gesehen hat.
     */
    fun fullyVisible(rows: List<Row>, viewportStart: Int, viewportEnd: Int): List<Row> =
        rows.filter { it.offset >= viewportStart && it.offset + it.size <= viewportEnd }

    fun firstFullyVisibleIndex(rows: List<Row>, viewportStart: Int, viewportEnd: Int, fallback: Int): Int =
        fullyVisible(rows, viewportStart, viewportEnd).firstOrNull()?.index ?: fallback

    /** Wie weit ein Druck blättert: eine Seite minus eine Zeile, mindestens aber eine Zeile. */
    fun step(visibleCount: Int): Int = maxOf(1, visibleCount - 1)

    fun down(firstVisible: Int, visibleCount: Int, total: Int): Int =
        (firstVisible + step(visibleCount)).coerceIn(0, lastStart(visibleCount, total))

    fun up(firstVisible: Int, visibleCount: Int): Int =
        maxOf(0, firstVisible - step(visibleCount))

    /**
     * Der letzte sinnvolle Startindex. Weiter zu springen wäre wirkungslos - der Knopf würde
     * gedrückt aussehen und nichts tun.
     */
    fun lastStart(visibleCount: Int, total: Int): Int = maxOf(0, total - maxOf(1, visibleCount))

    fun canGoUp(firstVisible: Int): Boolean = firstVisible > 0

    fun canGoDown(firstVisible: Int, visibleCount: Int, total: Int): Boolean =
        firstVisible < lastStart(visibleCount, total)
}
