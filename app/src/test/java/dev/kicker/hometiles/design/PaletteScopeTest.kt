package dev.kicker.hometiles.design

import java.io.File
import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a screen's colours come from its theme, not from the default value.
 *
 * `LocalBigPalette` is a `staticCompositionLocalOf { Dark }`. reading it **before** the
 * screen's own `HomeTilesTheme` call gives that default instead of the chosen theme - and
 * silently, since in the dark theme, which is the default anyway, it looks right.
 *
 * six screens did exactly that. it showed only after setting the emulator to light and
 * measuring the screenshots: the message list stood on #0A0A0A while the rest of the app
 * carried #E8EAEC, and its heading was practically invisible in the light theme's dark ink.
 * whoever needs the contrast theme - the person who needs it most - got something other than
 * what they had set.
 */
class PaletteScopeTest {

    private val sources: List<File> =
        Quelltext.files()

    /** line of the first match in this file, or null. */
    private fun firstLine(lines: List<String>, hit: (String) -> Boolean): Int? =
        lines.indexOfFirst(hit).takeIf { it >= 0 }

    @Test
    fun `no palette is read before its theme`() {
        val tooEarly = mutableListOf<String>()
        sources.forEach { file ->
            val lines = file.readLines()
            val theme = firstLine(lines) {
                it.contains("HomeTilesTheme(") && !it.contains("fun HomeTilesTheme")
            } ?: return@forEach
            val palette = firstLine(lines) { it.contains("LocalBigPalette.current") }
                ?: return@forEach
            if (palette < theme) {
                tooEarly += "${file.name}:${palette + 1} (theme only on line ${theme + 1})"
            }
        }
        assertEquals(
            "the default palette stands here instead of the chosen theme: $tooEarly",
            emptyList<String>(),
            tooEarly,
        )
    }

    @Test
    fun `every screen sets a theme at all`() {
        // an activity without HomeTilesTheme paints in the default throughout - the same fault,
        // only complete.
        val without = sources
            .filter { it.name.endsWith("Activity.kt") }
            .filter { it.readText().contains("setContent") }
            .filterNot { it.readText().contains("HomeTilesTheme(") }
            .map { it.name }
        assertEquals("these screens paint without a theme: $without", emptyList<String>(), without)
    }

    @Test
    fun `the rule would find the old state`() {
        // counter-check on an invented file.
        val broken = listOf(
            "val palette = LocalBigPalette.current",
            "HomeTilesTheme(theme) {",
        )
        val theme = broken.indexOfFirst { it.contains("HomeTilesTheme(") }
        val palette = broken.indexOfFirst { it.contains("LocalBigPalette.current") }
        assertTrue("the order has to show", palette < theme)
    }
}
