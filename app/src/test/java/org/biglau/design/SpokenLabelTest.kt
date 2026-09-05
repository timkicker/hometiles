package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * what a tile **says** does not hang on what it **shows**.
 *
 * two settings hide the label - "label off" and "leave out cut labels". both are made for
 * eyes: a cut word is harder to read than none. nothing is cut for a screen reader, and if
 * the spoken text vanished with the visible label the tile would be nameless to a blind
 * person.
 *
 * and that meets exactly: whoever hides the labels usually does so because they see badly.
 * the same setting must not take the name away from them.
 *
 * checked in the source and **not** reproducible on the device: on this phone every label
 * fitted on 04.09.2026, so none is ever cut. hence a rule rather than an observation.
 */
class SpokenLabelTest {

    private val tile = Quelltext.withoutComments("org/biglau/ui/BigTile.kt")

    @Test
    fun `the spoken text does not know about visibility`() {
        val from = tile.indexOf("val spoken =")
        assertTrue("the spoken text is no longer built", from > 0)
        val body = tile.substring(from, minOf(tile.length, from + 400))
        listOf("showLabel", "LocalHideCutLabels", "LabelPosition.HIDDEN").forEach { visibility ->
            assertTrue(
                "the spoken text hangs on $visibility - then a setting made for eyes takes " +
                    "the tile's name from the screen reader.",
                visibility !in body,
            )
        }
    }

    /**
     * and it is built in **one** place, so no tile is forgotten.
     *
     * the rule first stood on the name `val gesprochen` and fell over when `BigRow` got a
     * variable of the same name for something else entirely. it was counting the wrong thing
     * anyway: `TileSpeech.describe` stands in two places, and rightly so - the home screen
     * adds the tile's state (signal, battery), the tile itself the badge at the corner.
     *
     * what the rule is really about is the **badge**: no caller may have to append it, or one
     * of them forgets. so whoever passes `badge` is counted.
     */
    @Test
    fun `only the tile itself builds it`() {
        val places = Quelltext.files()
            .filter { file ->
                val text = file.readText()
                val from = text.indexOf("TileSpeech.describe(")
                from >= 0 && "badge =" in text.substring(from, minOf(text.length, from + 300))
            }
            .map { it.name }
        assertEquals(
            "the badge at the corner is appended in more than one place. then there is a " +
                "tile that forgets it.",
            listOf("BigTile.kt"),
            places,
        )
    }

    /** the other way round the visibility does hang on both settings. */
    @Test
    fun `the visibility hangs on both settings`() {
        val from = tile.indexOf("val showLabel =")
        assertTrue("showLabel no longer exists", from > 0)
        val body = tile.substring(from, minOf(tile.length, from + 200))
        assertTrue(
            "the label no longer goes by both settings: $body",
            "LabelPosition.HIDDEN" in body && "LocalHideCutLabels" in body,
        )
    }
}
