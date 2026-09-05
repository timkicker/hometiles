package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * numbers that change stand in tabular figures.
 *
 * `PLAN.md` 3.7 asks for it so columns do not jump. measured on the screen, before: "11 %"
 * in the header was 64 pixels wide, "88 %" 74. afterwards: 57 against 59.
 */
class TabularDigitsTest {

    /** places where a number changes in place. */
    private val places = listOf(
        "org/biglau/ui/HomeHeader.kt",
        "org/biglau/ui/InfoTiles.kt",
        "org/biglau/phone/InCallActivity.kt",
        "org/biglau/phone/DialerActivity.kt",
    )

    @Test
    fun `every running number gets tabular figures`() {
        val without = places.filterNot { "tabularFigures()" in Quelltext.file(it).readText() }
        assertTrue("without tabular figures: $without", without.isEmpty())
    }

    /**
     * `Text(style = ...)` replaces the surrounding style. a stock style setting only `tnum`
     * threw the chosen font away - the numbers stood in the system font while everything
     * beside them stood in the user's.
     */
    @Test
    fun `the tabular figures keep the surface font`() {
        val source = Quelltext.file("org/biglau/ui/TextSizing.kt").readText()
        assertTrue("tabularFigures builds a style of its own", "LocalTextStyle.current.copy(" in source)
        // the german name is the one the stock style once had; the check guards against
        // exactly it coming back.
        assertTrue("a stock style is there again", "val TabellenZiffern" !in source)
    }

    /** and the font can do it - otherwise the setting has no effect. */
    @Test
    fun `the shipped font knows tnum`() {
        listOf("atkinson_regular.ttf", "atkinson_bold.ttf").forEach { name ->
            val bytes = Quelltext.resource("font/$name").readBytes()
            val mark = "tnum".toByteArray()
            val inside = (0..bytes.size - mark.size).any { i ->
                mark.indices.all { bytes[i + it] == mark[it] }
            }
            assertTrue("$name has no tabular figures", inside)
        }
    }
}
