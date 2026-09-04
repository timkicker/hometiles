package org.biglau.tiles

import org.biglau.data.Cell
import org.biglau.data.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Wohin der Fokus geht, entscheidet das Raster und nicht die Reihenfolge in der Datei.
 *
 * PLAN.md 10.3.2. Die Zellen stehen in `config.json` in der Reihenfolge, in der sie angelegt
 * wurden. Danach zu gehen waere einfach und falsch: der Fokus spraenge ueber den Bildschirm,
 * und zwar jedes Mal anders, je nachdem in welcher Reihenfolge jemand seine Kacheln belegt
 * hat. Gerechnet wird deshalb aus den Rechtecken.
 *
 * Der Fall, an dem sich das entscheidet, ist die **gemerkte Spalte**. Eine Kachel darf
 * mehrere Felder breit sein. Wer von einer schmalen Kachel nach unten auf eine breite geht
 * und wieder hoch, will zurueck auf die schmale, aus der er kam, und nicht auf deren linken
 * Nachbarn. Ohne Gedaechtnis rutscht der Fokus bei jedem Auf und Ab nach links, bis er in
 * der ersten Spalte klebt. Das merkt man erst am Geraet, und dann ist es muehsam.
 *
 * Am Rand passiert nichts: der Screenwechsel bekommt eigene Tasten, sonst rutscht man beim
 * Blaettern in einer Kachelreihe unversehens auf einen anderen Screen.
 */
class FokusfolgeTest {

    private fun zelle(x: Int, y: Int, w: Int = 1, h: Int = 1) = Cell(x = x, y = y, w = w, h = h)

    /** Ein gewoehnliches Raster mit zwei Spalten und drei Zeilen. */
    private val raster = listOf(
        zelle(0, 0), zelle(1, 0),
        zelle(0, 1), zelle(1, 1),
        zelle(0, 2), zelle(1, 2),
    )

    @Test
    fun `nach rechts kommt der rechte Nachbar`() {
        assertEquals(zelle(1, 1), Fokusfolge.nachbar(raster, zelle(0, 1), Richtung.RECHTS))
    }

    @Test
    fun `nach links kommt der linke Nachbar`() {
        assertEquals(zelle(0, 1), Fokusfolge.nachbar(raster, zelle(1, 1), Richtung.LINKS))
    }

    @Test
    fun `nach unten kommt die Zelle darunter`() {
        assertEquals(zelle(1, 2), Fokusfolge.nachbar(raster, zelle(1, 1), Richtung.RUNTER))
    }

    @Test
    fun `nach oben kommt die Zelle darueber`() {
        assertEquals(zelle(1, 0), Fokusfolge.nachbar(raster, zelle(1, 1), Richtung.HOCH))
    }

    @Test
    fun `am Rand passiert nichts`() {
        assertNull("rechts vom rechten Rand", Fokusfolge.nachbar(raster, zelle(1, 1), Richtung.RECHTS))
        assertNull("links vom linken Rand", Fokusfolge.nachbar(raster, zelle(0, 1), Richtung.LINKS))
        assertNull("ueber der ersten Zeile", Fokusfolge.nachbar(raster, zelle(0, 0), Richtung.HOCH))
        assertNull("unter der letzten Zeile", Fokusfolge.nachbar(raster, zelle(0, 2), Richtung.RUNTER))
    }

    /**
     * Die Reihenfolge in der Liste darf nichts aendern. Genau das ist der Grund fuer diese
     * Rechnerei.
     */
    @Test
    fun `die Reihenfolge in der Datei aendert nichts`() {
        val verdreht = raster.reversed()
        assertEquals(
            Fokusfolge.nachbar(raster, zelle(0, 1), Richtung.RECHTS),
            Fokusfolge.nachbar(verdreht, zelle(0, 1), Richtung.RECHTS),
        )
    }

    /** Eine breite Kachel unten, zwei schmale oben. */
    private val mitBreiter = listOf(
        zelle(0, 0), zelle(1, 0),
        zelle(0, 1, w = 2),
        zelle(0, 2), zelle(1, 2),
    )

    @Test
    fun `von der schmalen Kachel nach unten trifft die breite`() {
        assertEquals(zelle(0, 1, w = 2), Fokusfolge.nachbar(mitBreiter, zelle(1, 0), Richtung.RUNTER))
    }

    /**
     * Der Fall, um den es geht: runter auf die breite und wieder hoch. Ohne die gemerkte
     * Spalte landet man auf (0,0), also einen Platz links von dem, aus dem man kam.
     */
    @Test
    fun `mit gemerkter Spalte geht es zurueck, wo man herkam`() {
        val breite = zelle(0, 1, w = 2)
        assertEquals(
            "ohne Gedaechtnis rutscht der Fokus nach links",
            zelle(1, 0),
            Fokusfolge.nachbar(mitBreiter, breite, Richtung.HOCH, merkspalte = 1),
        )
        assertEquals(
            "ohne Merker gilt die eigene linke Kante",
            zelle(0, 0),
            Fokusfolge.nachbar(mitBreiter, breite, Richtung.HOCH),
        )
    }

    @Test
    fun `die gemerkte Spalte gilt auch nach unten`() {
        val breite = zelle(0, 1, w = 2)
        assertEquals(zelle(1, 2), Fokusfolge.nachbar(mitBreiter, breite, Richtung.RUNTER, merkspalte = 1))
        assertEquals(zelle(0, 2), Fokusfolge.nachbar(mitBreiter, breite, Richtung.RUNTER, merkspalte = 0))
    }

    /**
     * Eine gemerkte Spalte, die es in der Zielzeile nicht gibt, darf nicht ins Leere fuehren.
     * Dann gilt die naechstgelegene.
     */
    @Test
    fun `eine Spalte, die es unten nicht gibt, faellt auf die naechste zurueck`() {
        val luecke = listOf(zelle(0, 0), zelle(1, 0), zelle(2, 0), zelle(0, 1))
        assertEquals(zelle(0, 1), Fokusfolge.nachbar(luecke, zelle(2, 0), Richtung.RUNTER, merkspalte = 2))
    }

    /** Ein leeres Raster hat keinen Nachbarn, und das darf nicht werfen. */
    @Test
    fun `ohne Zellen gibt es keinen Nachbarn`() {
        assertNull(Fokusfolge.nachbar(emptyList(), zelle(0, 0), Richtung.RECHTS))
    }

    /**
     * Ein leerer Platz ist anklickbar, also muss er erreichbar sein.
     *
     * Am 04.09.2026 am Emulator gemessen, einen Zug nachdem die Reihenfolge stand: bei
     * offenem Ordner blieb der Fokus auf der einen belegten Kachel stehen, acht Tastendruecke
     * lang. Die sieben leeren Plaetze daneben tragen ein `Antippen` und oeffnen den Editor,
     * mit dem Finger erreicht man sie sofort. Mit Tasten gar nicht.
     *
     * Der Grund war, dass die Rechnung nur `screen.cells` kannte, also nur die belegten. Das
     * ist genau die Luecke, die in PLAN.md 10.3.6 als Gegenstueck zu `kleine-knoepfe.py`
     * steht: nicht "gross genug", sondern **erreichbar**. Sie war schneller da als das
     * Werkzeug, das sie finden soll.
     */
    @Test
    fun `leere Plaetze gehoeren in die Reihenfolge`() {
        val schirm = Screen(
            id = "s",
            name = "S",
            cols = 2,
            rows = 2,
            cells = listOf(zelle(0, 0)),
        )
        val ziele = Fokusfolge.ziele(schirm)
        assertEquals(
            "Vier Felder, eines belegt: alle vier muessen erreichbar sein",
            4,
            ziele.size,
        )
        assertEquals(
            "vom belegten Platz nach rechts kommt der leere daneben",
            zelle(1, 0),
            Fokusfolge.nachbar(ziele, zelle(0, 0), Richtung.RECHTS),
        )
    }

    /**
     * Die Zifferntasten zaehlen, wie man liest.
     *
     * PLAN.md 10.3.3. Eins ist oben links, dann nach rechts, dann die naechste Zeile. Nicht
     * die Reihenfolge in der Datei, aus demselben Grund wie bei den Pfeiltasten: die haengt
     * daran, in welcher Reihenfolge jemand seine Kacheln belegt hat.
     *
     * Gezaehlt werden **Plaetze**, nicht belegte Kacheln. Wer auf einen leeren Platz zeigt,
     * meint den dritten von oben links, egal ob dort schon etwas liegt. Zaehlte man nur die
     * belegten, verschoeben sich alle Nummern, sobald eine Kachel dazukommt oder wegfaellt,
     * und die Zahl, die sich jemand gemerkt hat, waere falsch.
     *
     * Nur die ersten neun. Mehr Ziffern gibt es nicht, und eine zweistellige Eingabe mit
     * Wartezeit waere eine Falle fuer langsame Haende.
     */
    @Test
    fun `eine Ziffer zaehlt wie gelesen`() {
        assertEquals(zelle(0, 0), Fokusfolge.nummer(raster, 1))
        assertEquals(zelle(1, 0), Fokusfolge.nummer(raster, 2))
        assertEquals(zelle(0, 1), Fokusfolge.nummer(raster, 3))
        assertEquals(zelle(1, 2), Fokusfolge.nummer(raster, 6))
    }

    @Test
    fun `die Reihenfolge in der Datei aendert die Nummer nicht`() {
        assertEquals(
            Fokusfolge.nummer(raster, 3),
            Fokusfolge.nummer(raster.reversed(), 3),
        )
    }

    @Test
    fun `eine breite Kachel zaehlt einmal`() {
        assertEquals(zelle(0, 1, w = 2), Fokusfolge.nummer(mitBreiter, 3))
        assertEquals(zelle(0, 2), Fokusfolge.nummer(mitBreiter, 4))
    }

    @Test
    fun `es gibt nur neun Ziffern`() {
        val gross = (0 until 4).flatMap { y -> (0 until 3).map { x -> zelle(x, y) } }
        assertEquals(zelle(2, 2), Fokusfolge.nummer(gross, 9))
        assertNull("die zehnte Kachel hat keine Taste", Fokusfolge.nummer(gross, 10))
        assertNull("null ist keine Kachelnummer", Fokusfolge.nummer(gross, 0))
    }

    @Test
    fun `eine Ziffer ohne Platz trifft nichts`() {
        assertNull(Fokusfolge.nummer(listOf(zelle(0, 0)), 2))
    }

    /**
     * Der erste Fokus sitzt oben links, nicht auf dem, was zufaellig zuerst in der Liste
     * steht.
     */
    @Test
    fun `der erste Fokus sitzt oben links`() {
        assertEquals(zelle(0, 0), Fokusfolge.erste(raster.reversed()))
        assertNull(Fokusfolge.erste(emptyList()))
    }
}
