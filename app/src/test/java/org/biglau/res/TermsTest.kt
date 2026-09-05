package org.biglau.res

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the same thing is called the same within one language. PLAN.md 10.4.
 *
 * five languages came about in one evening, and the danger there is not the gross mistake -
 * `TranslationsTest` catches that - but **disagreement**: the loudspeaker is called one thing
 * here and another there, and whoever does not read the app in one go takes two names for two
 * things.
 *
 * three of the five languages I do not speak. what a tool can do is this: **compare two texts
 * that mean the same thing** and report where a language disagrees with itself. it cannot
 * judge whether a phrasing sounds good, and this rule does not pretend to. three of its first
 * four findings stood in the **english**, the language I know best - two words for one button,
 * two wordings for one action, and one language saying the same word for "more" and "next".
 */
class TermsTest {

    private fun texts(language: String): Map<String, String> = buildMap {
        Quelltext.resRoots.map { File(it, "$language/strings.xml") }.filter { it.isFile }
            .forEach { file ->
                Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
                    .findAll(file.readText())
                    .forEach { put(it.groupValues[1], it.groupValues[2].trim()) }
            }
    }

    private val base = texts("values")

    /**
     * where a language is finer than english, with the reason. these are not sloppiness but
     * languages making a distinction english does not; each stands with its reason so the next
     * one is not simply added.
     */
    private val mayDiffer = mapOf(
        "Home" to "the home screen and the house icon share one word in english. every other " +
            "language separates them, and it is right to.",
        "Time" to "the time of day and the group of time icons. german separates those, " +
            "english does not.",
        "none" to "grammatical gender: the word depends on what it refers to and cannot be " +
            "made uniform.",
    )

    @Test
    fun `the same english text, the same translation`() {
        assertTrue("no texts read, then this rule measures nothing", base.size > 100)
        val repeated = base.entries.groupBy({ it.value }, { it.key })
            .filterValues { it.size > 1 }
            .filterKeys { it !in mayDiffer }
        val findings = Quelltext.translations().flatMap { language ->
            val d = texts(language)
            repeated.mapNotNull { (english, keys) ->
                val values = keys.mapNotNull { d[it] }.toSet()
                if (values.size > 1) "$language: \"$english\" -> $values ($keys)" else null
            }
        }.sorted()
        assertEquals(
            "the same thing is called two things in one language here. either the " +
                "translation disagrees with itself, or the english text is ambiguous and " +
                "belongs made precise - then it is the text that changes, not the exception " +
                "list:\n" + findings.joinToString("\n"),
            emptyList<String>(),
            findings,
        )
    }

    /**
     * and the other direction, which found more: two different english texts merged into one
     * in a language. either the english says the same thing twice in different words, or the
     * translation lost a distinction. both belong looked at.
     */
    private val mayMerge = mapOf(
        ("values-de" to "Anrufliste") to
            "the screen and the heading of its settings. english says `Call log` and " +
            "`Call list`; in german both are the one word.",
    )

    @Test
    fun `different english texts stay different`() {
        val findings = Quelltext.translations().flatMap { language ->
            texts(language).entries.groupBy({ it.value }, { it.key })
                .filterValues { it.size > 1 }
                .filterNot { (text, _) -> (language to text) in mayMerge }
                .mapNotNull { (text, keys) ->
                    val english = keys.mapNotNull { base[it] }.toSet()
                    if (english.size > 1) "$language: \"$text\" <- $english" else null
                }
        }.sorted()
        assertEquals(
            "a language merges two different english texts into one here. either the english " +
                "says the same thing twice in different ways - then it belongs unified - or " +
                "the translation lost a distinction:\n" + findings.joinToString("\n"),
            emptyList<String>(),
            findings,
        )
    }
}
