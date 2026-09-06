package dev.kicker.hometiles.res

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * no dashes, no middle dots in the screen texts.
 *
 * the long dash and the middle dot are the mark of machine-written text. the user said so on
 * 04.09.2026, and it holds on its own: a sentence needing a dash is usually two sentences not
 * yet separated. read apart they are easier, and whoever writes large has little room.
 *
 * the replacement is a comma, a colon, a full stop or a second line. counted were 51 long
 * dashes, 2 short ones and 16 middle dots.
 *
 * the hyphen stays: "SMS-App" is one word, not an aside.
 */
class TextDashesTest {

    // the characters themselves are the check object.
    private val forbidden = mapOf(
        '—' to "long dash",
        '–' to "short dash",
        '·' to "middle dot",
    )

    @Test
    fun `no text carries a dash or a middle dot`() {
        val hits = Quelltext.allTexts().flatMap { file ->
            val content = file.readText()
            Regex("""<(?:string|item)[^>]*>(.*?)</(?:string|item)>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(content)
                .flatMap { place ->
                    forbidden.entries
                        .filter { (character, _) -> character in place.groupValues[1] }
                        .map { (_, name) ->
                            "${file.parentFile.name}: $name in " +
                                place.groupValues[1].take(60)
                        }
                }
        }
        assertEquals(
            "the replacement is a comma, a colon, a full stop or a second line",
            emptyList<String>(),
            hits,
        )
    }

    @Test
    fun `the rule would find a dash`() {
        // counter-check: otherwise a wrong pattern would wave everything through.
        val sample = "<string name=\"sample\">Ein Satz — mit Strich</string>"
        val found = Regex("""<string[^>]*>(.*?)</string>""").find(sample)
        assertEquals(true, found != null && '—' in found.groupValues[1])
    }
}
