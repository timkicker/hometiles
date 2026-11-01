package dev.kicker.hometiles.ui

import dev.kicker.hometiles.data.ThemeName
import dev.kicker.hometiles.ui.theme.paletteFor
import dev.kicker.hometiles.ui.theme.wantsDarkSystemIcons
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the clock and icons in the status bar have to be readable on the app's own background.
 *
 * `enableEdgeToEdge` picks them by the phone's dark mode. with the phone light and the theme
 * dark they were black on black, measured on the emulator on 29.10.2026: brightness 4 to 10
 * of 255 where the clock stood.
 */
class SystemBarIconsTest {

    @Test
    fun `every theme gets icons that stand out from its background`() {
        val darkIcons = ThemeName.entries.associateWith { paletteFor(it, systemIsDark = false).wantsDarkSystemIcons() }
        assertEquals(
            mapOf(
                ThemeName.DARK to false,
                ThemeName.HIGH_CONTRAST to false,
                ThemeName.LIGHT to true,
                ThemeName.SYSTEM to true,
                ThemeName.PAPER to true,
            ),
            darkIcons,
        )
    }

    @Test
    fun `a dark theme on a light phone keeps light icons`() {
        assertEquals(false, paletteFor(ThemeName.DARK, systemIsDark = false).wantsDarkSystemIcons())
        assertEquals(false, paletteFor(ThemeName.SYSTEM, systemIsDark = true).wantsDarkSystemIcons())
    }

    /** the choice has to reach the window, in the one place every screen goes through. */
    @Test
    fun `the theme sets the bars itself`() {
        val theme = File("src/main/kotlin/dev/kicker/hometiles/ui/theme/Theme.kt").readText()
        assertTrue(theme.contains("isAppearanceLightStatusBars = light"))
        assertTrue(theme.contains("val light = palette.wantsDarkSystemIcons()"))
    }

    /**
     * set during onCreate alone, android 15 lays the theme's value over it once the window is
     * attached: the clock stood white on the light theme (emulator, 01.11.2026). android 8 and
     * 11 kept it, so a test on those alone would have passed.
     */
    @Test
    fun `the choice is made again once the window is attached`() {
        val theme = File("src/main/kotlin/dev/kicker/hometiles/ui/theme/Theme.kt").readText()
        assertTrue(theme.contains("view.post { apply() }"))
    }
}
