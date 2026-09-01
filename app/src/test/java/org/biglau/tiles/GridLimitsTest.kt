package org.biglau.tiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PLAN.md 4.1: „Raster pro Screen frei: 1–6 Spalten × 1–8 Zeilen".
 *
 * Gebaut war eine feste Liste von sechs Vorlagen, die bei drei Spalten endete — mit dem
 * Argument, mehr werde auf 349 dp zur Briefmarke. Das stimmt für dieses Gerät und ist
 * trotzdem die falsche Antwort: es macht die Grenze zu einer Zahl im Quelltext statt zu
 * einer Eigenschaft des Bildschirms.
 */
class GridLimitsTest {

    // Jelly 2: 349 dp breit, 2 % Rand auf jeder Seite, 4 dp Abstand.
    private val jellyBreite = 335f
    private val jellyHoehe = 551f

    @Test
    fun `auf drei zoll sind vier spalten das aeusserste`() {
        assertEquals(4, GridLimits.maxColumns(jellyBreite, gutterDp = 4))
        assertEquals(8, GridLimits.maxRows(jellyHoehe, gutterDp = 4))
    }

    @Test
    fun `ein breiterer bildschirm bekommt mehr`() {
        assertEquals(6, GridLimits.maxColumns(600f, gutterDp = 4))
    }

    // Die Obergrenze aus dem Plan gilt trotzdem: mehr als sechs Spalten ergibt auf keinem
    // Telefon eine Kachel, auf der ein Wort steht.
    @Test
    fun `ueber sechs geht es nie`() {
        assertEquals(GridLimits.MAX_COLUMNS, GridLimits.maxColumns(4000f, gutterDp = 0))
        assertEquals(GridLimits.MAX_ROWS, GridLimits.maxRows(4000f, gutterDp = 0))
    }

    // Auch auf einem winzigen Bildschirm bleibt eine Spalte uebrig - ein Raster mit null
    // Spalten waere ein leerer Startbildschirm.
    @Test
    fun `mindestens eine spalte bleibt immer`() {
        assertEquals(1, GridLimits.maxColumns(10f, gutterDp = 4))
        assertEquals(1, GridLimits.maxRows(10f, gutterDp = 4))
    }

    // Ein groesserer Abstand kostet Platz und damit unter Umstaenden eine Spalte.
    @Test
    fun `der abstand zaehlt mit`() {
        assertTrue(GridLimits.maxColumns(300f, gutterDp = 0) >= GridLimits.maxColumns(300f, gutterDp = 12))
    }

    @Test
    fun `jede angebotene spalte haelt das mindestmass ein`() {
        for (n in GridLimits.columns(jellyBreite, gutterDp = 4)) {
            val zelle = (jellyBreite - (n - 1) * 4) / n
            assertTrue("$n Spalten ergeben $zelle dp", zelle >= GridLimits.MIN_CELL_WIDTH_DP)
        }
    }

    // Gegenprobe: die naechstgroessere Zahl faellt tatsaechlich durch.
    @Test
    fun `eine spalte mehr waere zu schmal`() {
        val n = GridLimits.maxColumns(jellyBreite, gutterDp = 4) + 1
        val zelle = (jellyBreite - (n - 1) * 4) / n
        assertTrue("$n Spalten ergeben $zelle dp", zelle < GridLimits.MIN_CELL_WIDTH_DP)
    }

    // Die Vorgabe muss unter dem bleiben, was das Geraet traegt - sonst stuende die App
    // mit einem Raster da, das sie selbst nicht mehr anbietet.
    @Test
    fun `die vorgabe passt ins angebot`() {
        val vorgabe = org.biglau.data.Defaults.mainScreen()
        assertTrue(vorgabe.cols <= GridLimits.maxColumns(jellyBreite, gutterDp = 4))
        assertTrue(vorgabe.rows <= GridLimits.maxRows(jellyHoehe, gutterDp = 4))
    }
}
