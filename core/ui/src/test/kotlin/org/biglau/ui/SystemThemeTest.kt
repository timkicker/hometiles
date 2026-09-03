package org.biglau.ui

import org.biglau.data.ThemeName
import org.biglau.ui.theme.FreeTileColor
import org.biglau.ui.theme.paletteFor
import org.biglau.ui.theme.toArgbLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * „Wie das Telefon" (PLAN.md 4.2, viertes Thema).
 *
 * Es ist kein eigenes Aussehen, sondern eine Frage ans System — und deshalb die einzige
 * Wahl, deren Ergebnis sich ändert, ohne dass jemand etwas in BigLau anfasst.
 */
class SystemThemeTest {

    @Test
    fun `steht das Telefon dunkel, ist es das dunkle Thema`() {
        assertSame(paletteFor(ThemeName.DARK, true), paletteFor(ThemeName.SYSTEM, true))
    }

    @Test
    fun `steht das Telefon hell, ist es das helle Thema`() {
        assertSame(paletteFor(ThemeName.LIGHT, false), paletteFor(ThemeName.SYSTEM, false))
    }

    @Test
    fun `die beiden Zustaende ergeben nicht dasselbe`() {
        // Sonst waere die Wahl eine Attrappe.
        assertNotEquals(paletteFor(ThemeName.SYSTEM, true), paletteFor(ThemeName.SYSTEM, false))
    }

    @Test
    fun `die anderen drei Themen fragen das Telefon nicht`() {
        // Wer ausdrücklich "hell" wählt, will hell - auch auf einem dunkel gestellten
        // Telefon. Sonst hätte die Wahl keinen Sinn.
        listOf(ThemeName.DARK, ThemeName.HIGH_CONTRAST, ThemeName.LIGHT).forEach { thema ->
            assertSame(
                "$thema darf sich vom System nicht umstimmen lassen",
                paletteFor(thema, true),
                paletteFor(thema, false),
            )
        }
    }

    @Test
    fun `das Kontrast-Thema kennt keine Kachelfarben`() {
        // PLAN.md 3.3: dort zaehlt nur Schwarz/Gelb. Am Emulator gesehen - schwarze
        // Kacheln mit gelbem Rand, 17,20:1 fuer Rand und Beschriftung -, und hier
        // festgehalten, damit es so bleibt: weder eine Palettenfarbe noch ein frei
        // gewaehlter Ton darf sich dort durchsetzen.
        val kontrast = paletteFor(ThemeName.HIGH_CONTRAST, true).tiles.map { it.toArgbLong() }
        assertEquals(false, FreeTileColor.themeUsesTileColours(kontrast))
    }

    @Test
    fun `die anderen Themen kennen sehr wohl Kachelfarben`() {
        listOf(ThemeName.DARK, ThemeName.LIGHT).forEach { thema ->
            val tiles = paletteFor(thema, true).tiles.map { it.toArgbLong() }
            assertEquals("$thema", true, FreeTileColor.themeUsesTileColours(tiles))
        }
    }

    @Test
    fun `es gibt genau vier Themen zur Wahl`() {
        // PLAN.md 4.2: "Dunkel / Kontrast / Hell / Systemabhängig".
        assertEquals(4, ThemeName.entries.size)
    }
}
