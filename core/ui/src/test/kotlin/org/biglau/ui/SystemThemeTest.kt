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
 * "Wie das Telefon", the plan's fourth theme (PLAN.md 4.2, german because the plan is).
 *
 * it is no look of its own but a question to the system - and so the only choice whose result
 * changes without anyone touching anything in BigLau.
 */
class SystemThemeTest {

    @Test
    fun `with the phone set dark it is the dark theme`() {
        assertSame(paletteFor(ThemeName.DARK, true), paletteFor(ThemeName.SYSTEM, true))
    }

    @Test
    fun `with the phone set light it is the light theme`() {
        assertSame(paletteFor(ThemeName.LIGHT, false), paletteFor(ThemeName.SYSTEM, false))
    }

    @Test
    fun `the two states do not give the same`() {
        // otherwise the choice would be a dummy.
        assertNotEquals(paletteFor(ThemeName.SYSTEM, true), paletteFor(ThemeName.SYSTEM, false))
    }

    @Test
    fun `the other three themes do not ask the phone`() {
        // whoever expressly chooses light wants light - on a phone set dark as well.
        listOf(ThemeName.DARK, ThemeName.HIGH_CONTRAST, ThemeName.LIGHT).forEach { theme ->
            assertSame(
                "$theme must not let the system talk it round",
                paletteFor(theme, true),
                paletteFor(theme, false),
            )
        }
    }

    @Test
    fun `the contrast theme knows no tile colours`() {
        // PLAN.md 3.3: only black and yellow count there, so neither a palette colour nor a
        // freely chosen hue may win through.
        val contrast = paletteFor(ThemeName.HIGH_CONTRAST, true).tiles.map { it.toArgbLong() }
        assertEquals(false, FreeTileColor.themeUsesTileColours(contrast))
    }

    @Test
    fun `the other themes do know tile colours`() {
        listOf(ThemeName.DARK, ThemeName.LIGHT).forEach { theme ->
            val tiles = paletteFor(theme, true).tiles.map { it.toArgbLong() }
            assertEquals("$theme", true, FreeTileColor.themeUsesTileColours(tiles))
        }
    }

    @Test
    fun `there are exactly four themes to choose from`() {
        // PLAN.md 4.2: "Dunkel / Kontrast / Hell / Systemabhängig".
        assertEquals(4, ThemeName.entries.size)
    }
}
