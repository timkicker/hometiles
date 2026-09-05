package org.biglau.res

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * one form of address, not two.
 *
 * the german texts said both: 21 places formal, 12 informal, and one said both in a single
 * sentence, where the second word was no longer unambiguous.
 *
 * chosen is the formal one: it was the majority, and the app is set up for an older person
 * nobody else addresses informally in his language. where possible the best answer is to need
 * no address at all.
 *
 * this rule is a **decision, not a truth**. should the app address informally, the test turns
 * around and the other list stands here.
 */
class AddressFormTest {

    private val files = Quelltext.texts("values-de") + Quelltext.texts("values-de", "plurals.xml")

    /**
     * lower case too. the first version looked only for the capitalised form, arguing the
     * lower-case word hides inside other words - the word boundaries take care of that, and
     * seven texts stood there unnoticed for months.
     */
    private val informal = Regex(
        """\b([Dd]u|[Dd]ir|[Dd]ich|[Dd]ein|[Dd]eine|[Dd]einen|[Dd]einem|[Dd]einer|[Dd]eines)\b""",
    )

    /**
     * and the imperative, which needs no pronoun: three texts addressed informally without
     * containing a single one of the words above.
     *
     * **what this list can and cannot do:** it knows the verbs this app gives instructions
     * with. a new verb does not stand out here. that is a weakness and not an intention - it
     * is better than nothing and worse than a grammar.
     */
    private val imperative = Regex(
        """(Öffne|Tippe|Halte|Wähle|Schalte|Versuche|Drücke|Lege|Setze|Trage|Gib|Nimm|""" +
            """Schau|Prüfe|Wische|Starte|Warte|Lösche|Ändere|Speichere|Rufe|Sende|Ziehe|""" +
            """Klicke|Mache|Denke|Lass)""",
    )

    @Test
    fun `the german texts do not command in the informal form`() {
        val hits = mutableListOf<String>()
        files.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val content = Regex(""">([^<]+)<""").find(line)?.groupValues?.get(1) ?: return@forEachIndexed
                // **only at the start of a sentence.** several of these words are also nouns
                // standing mid-sentence; the first version reported them and nearly had me
                // change correct texts. an imperative opens.
                content.split(Regex("""(?<=[.!?—:])\s+""")).forEach { sentence ->
                    val first = sentence.trim().substringBefore(" ").trim(',', '.', ':', ';')
                    if (imperative.matches(first)) {
                        hits += "${file.name}:${index + 1}: $first ..."
                    }
                }
            }
        }
        assertTrue(
            "an instruction in the informal form stands here without using a pronoun, so " +
                "the rule below does not see it:\n" + hits.joinToString("\n"),
            hits.isEmpty(),
        )
    }

    @Test
    fun `the german texts use the formal address`() {
        val hits = mutableListOf<String>()
        files.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                if (informal.containsMatchIn(line)) {
                    hits += "${file.name}:${index + 1}: ${line.trim()}"
                }
            }
        }
        assertTrue(
            "informal here, formal elsewhere - one form of address, not two:\n" +
                hits.joinToString("\n"),
            hits.isEmpty(),
        )
    }
}
