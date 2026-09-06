package dev.kicker.hometiles.design

import java.io.File
import kotlin.math.abs
import dev.kicker.hometiles.ui.theme.Tokens
import dev.kicker.hometiles.ui.theme.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the colour table in `PLAN.md` 3.3 agrees with the constants.
 *
 * the section carries the sentence that contrast is a function and not a matter of taste, and
 * three of its six rows stood there wrong by a hundredth. all trifles, and dangerous for
 * exactly that reason: a table with four digits looks calculated.
 *
 * `ContrastTest` checks the **thresholds** (stands out, is readable). this rule checks the
 * **numbers in the plan** - what someone reads who wants to change the palette and needs to
 * know where the room ends.
 */
class PaletteTableTest {

    private val plan = File("../PLAN.md").readText()

    /** name, hex, ratio against the ground, ratio for white text. */
    private data class Row(val name: String, val colour: Long, val ground: Double, val text: Double)

    private fun table(): List<Row> = Regex(
        // `\w` is ascii in kotlin - two colour names with umlauts fell out of the table that
        // way, and the rule reported "only four rows" instead of the actual thing.
        """^\| ([^|]+?) \| #([0-9A-F]{6}) \| (\d,\d\d):1 \| (\d,\d\d):1 \|$""",
        RegexOption.MULTILINE,
    ).findAll(plan).map {
        Row(
            name = it.groupValues[1],
            colour = 0xFF000000L or it.groupValues[2].toLong(16),
            ground = it.groupValues[3].replace(',', '.').toDouble(),
            text = it.groupValues[4].replace(',', '.').toDouble(),
        )
    }.toList()

    @Test
    fun `the table has six rows and names exactly the palette colours`() {
        val rows = table()
        assertEquals("the colour table in PLAN.md 3.3 is no longer to be found", 6, rows.size)
        assertEquals(
            "the colours in the plan are no longer those of the dark palette",
            Tokens.DARK_TILES,
            rows.map { it.colour },
        )
    }

    @Test
    fun `every given number is right to two places`() {
        table().forEach { row ->
            val ground = contrastRatio(row.colour, Tokens.DARK_BACKGROUND)
            val text = contrastRatio(0xFFFFFFFFL, row.colour)
            assertTrue(
                "${row.name} against the ground: the plan says ${row.ground}, computed " +
                    "${"%.4f".format(ground)}",
                abs(ground - row.ground) < 0.006,
            )
            assertTrue(
                "${row.name}, white text: the plan says ${row.text}, computed " +
                    "${"%.4f".format(text)}",
                // 0.006 and not 0.005: blue sits at 5.6250, exactly on the rounding boundary.
                // a rule standing on one falls over the rounding rather than the thing.
                abs(text - row.text) < 0.006,
            )
        }
    }

    /** the two numbers in the prose below as well. */
    @Test
    fun `white on both grounds is right`() {
        assertTrue("19,8:1 no longer stands in the plan", "19,8:1" in plan)
        assertTrue("18,1:1 no longer stands in the plan", "18,1:1" in plan)
        assertEquals(19.8, contrastRatio(0xFFFFFFFFL, Tokens.DARK_BACKGROUND), 0.05)
        assertEquals(18.1, contrastRatio(0xFFFFFFFFL, Tokens.DARK_EMPTY_TILE), 0.05)
    }

    /**
     * and the theme table above it - every colour standing there is a constant.
     *
     * the light row was wrong three times over and the dark row was missing its border
     * entirely. someone reads such values off to rebuild or adjust a colour and gets a result
     * that is *almost* right, which is worse than one that is obviously wrong.
     */
    @Test
    fun `the theme table names the actual colours`() {
        fun row(start: String): String =
            plan.lineSequence().firstOrNull { it.startsWith(start) }
                ?: throw AssertionError("row is gone from PLAN.md 3.3: $start")

        fun hex(value: Long) = "#%06X".format(value and 0xFFFFFFL)

        val dark = row("| **Dunkel**")
        listOf(
            Tokens.DARK_BACKGROUND, Tokens.DARK_EMPTY_TILE, Tokens.DARK_EMPTY_TILE_BORDER,
            Tokens.DARK_ON_BACKGROUND, Tokens.DARK_ACCENT, Tokens.DARK_DANGER,
            Tokens.DARK_DANGER_TEXT,
        ).forEach { assertTrue("dark: ${hex(it)} is missing from the row", hex(it) in dark) }

        val light = row("| **Hell**")
        listOf(
            Tokens.LIGHT_BACKGROUND, Tokens.LIGHT_EMPTY_TILE, Tokens.LIGHT_EMPTY_TILE_BORDER,
            Tokens.LIGHT_ON_BACKGROUND, Tokens.LIGHT_ACCENT, Tokens.LIGHT_DANGER,
            Tokens.LIGHT_DANGER_TEXT,
        ).forEach { assertTrue("light: ${hex(it)} is missing from the row", hex(it) in light) }

        val contrast = row("| **Kontrast**")
        listOf(
            Tokens.CONTRAST_BACKGROUND, Tokens.CONTRAST_INK, Tokens.CONTRAST_DANGER,
            Tokens.CONTRAST_DANGER_TEXT,
        ).forEach { assertTrue("contrast: ${hex(it)} is missing from the row", hex(it) in contrast) }
    }
}
