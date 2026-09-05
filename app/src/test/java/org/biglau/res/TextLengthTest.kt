package org.biglau.res

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * no text longer than 160 characters.
 *
 * the texts were too long: the longest had 240 characters and stood on a screen that does not
 * scroll. at 135 percent type size 160 characters are already seven lines; what goes beyond
 * that nobody reads, and where there is no scrolling it falls out at the bottom silently.
 *
 * while shortening, the same fault happened twice, and both times a rule caught it: **the
 * where** is the first thing to go. "give the role back" instead of "give it back in the
 * settings". the sentence gets shorter and the way out unusable. so this rule stands beside
 * [org.biglau.tiles.NoRoomTest] and [org.biglau.sms.MmsHintTest], which check exactly
 * that: shortening is allowed to whoever leaves the way standing.
 *
 * 160 is the longest text still standing after the pass, not a wished-for number. the bound
 * is a ratchet: it holds what has been reached. lowering it means shortening first.
 *
 * one number for both languages, not two: german is regularly longer than english, so a
 * shared bound binds where it is harder.
 *
 * what this rule does **not** measure is lines. one german phrasing was ten characters
 * shorter than another and needed one line **more** on the emulator: a 24-letter word fitted
 * into no started line and left half of the one before it empty. counted shorter, set longer.
 * so a look at the device stays part of shortening; the number here only finds the texts
 * where it is worth taking.
 */
class TextLengthTest {

    private val bound = 160

    @Test
    fun `no text is longer than the bound`() {
        val tooLong = Quelltext.allTexts().flatMap { file ->
            Regex("""<(?:string|item)[^>]*?(?:name="([^"]*)")?[^>]*>(.*?)</(?:string|item)>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(file.readText())
                .map { it.groupValues[1] to it.groupValues[2] }
                .filter { (_, text) -> text.length > bound }
                .map { (name, text) ->
                    "${file.parentFile.name}/$name: ${text.length} characters"
                }
        }
        assertEquals(
            "too long. shorten, but leave the where standing: a sentence without the way is " +
                "not a shorter sentence but a useless one",
            emptyList<String>(),
            tooLong,
        )
    }

    @Test
    fun `the rule would find a text that is too long`() {
        // counter-check: a wrong pattern would otherwise wave everything through.
        val probe = "<string name=\"probe\">" + "x".repeat(bound + 1) + "</string>"
        val found = Regex("""<string[^>]*>(.*?)</string>""").find(probe)
        assertEquals(true, found != null && found.groupValues[1].length > bound)
    }
}
