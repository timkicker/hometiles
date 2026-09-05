package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * the language and type rules of `PLAN.md` 3.7 that had none yet.
 *
 * they were all kept when looked at - the test holds them so it stays that way. an "oops" is
 * quickly written in, and whoever reads it later takes it for intention.
 */
class PlainLanguageTest {

    private val sources = Quelltext.files()

    /**
     * both languages **and** the plural forms. only `strings.xml` stood here, while the
     * plural texts appeared on screen just the same and were exempt from every rule here.
     */
    private val texts = Quelltext.allTexts()

    /**
     * no capitals for tile labels: capitals destroy the word shape poor readers navigate by.
     *
     * exception with a reason: the initials in the contact avatar and on the contact tile -
     * two letters with no word shape there would be to destroy.
     */
    @Test
    fun `no capitals in the display`() {
        val hits = sources.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                if ("uppercase()" in line && file.name != "ContactAvatar.kt") {
                    "${file.name}:${index + 1}"
                } else {
                    null
                }
            }
        }.toList()
        assertEquals(emptyList<String>(), hits)
    }

    /**
     * the hint on an empty tile stays short.
     *
     * it stands in a tile's label zone and is cut like any other label: on the 2 x 4 grid it
     * already read as a cut-off fragment - of all texts the one a new user has to read. at
     * four columns about ten or eleven characters fit; twelve is the bound at which it still
     * stands on every grid.
     */
    @Test
    fun `the hint on an empty tile stays short`() {
        // over the languages and not over the files found: `mapNotNull` would leave the rule
        // green if the text existed nowhere - it would have looked at nothing. `textValue`
        // falls over in that case.
        val tooLong = listOf("values", "values-de").mapNotNull { language ->
            val text = Quelltext.textValue("empty_tile_invite", language)
            if (text.length > 12) "$language: \"$text\" (${text.length})" else null
        }
        assertEquals(emptyList<String>(), tooLong)
    }

    /**
     * error messages say what happened and what to do. no "oops".
     *
     * the words of all five shipped languages, not only two: the rule reads every text file,
     * and knowing german and english alone it would have waved three languages through.
     */
    @Test
    fun `no embarrassed filler`() {
        val filler = listOf(
            "Ups", "Oops", "Sorry", "Entschuldig", "Hoppla",
            "Oups", "Desole", "Zut", "Vaya", "Lo sentimos", "Perdon", "Ops", "Scusa", "Spiacenti",
        )
        val hits = texts.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val word = filler.firstOrNull { it in line }
                if (word != null) "${file.name}:${index + 1} ($word)" else null
            }
        }
        assertEquals(emptyList<String>(), hits)
    }
}
