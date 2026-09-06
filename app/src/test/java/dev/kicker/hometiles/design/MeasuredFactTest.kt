package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a measured fact about the device carries its date.
 *
 * sentences like "on the user's phone `WRITE_CALL_LOG` is granted=false" are valuable: they
 * say why a detour is there, and they are measured rather than guessed. they have one
 * property one cannot see in them - **they age**.
 *
 * when HomeTiles got the dialer and the sms role, android granted several rights along with
 * them, `WRITE_CALL_LOG` among them. six comments were false in one evening, each written
 * down correctly and none of them true any more.
 *
 * the reason for each detour stayed valid - a way that hangs on no role is the better one.
 * what was wrong was not the decision but the tense: the present, for something measured.
 */
class MeasuredFactTest {

    // single-digit days and months too: the source has both 3.9.2026 and 03.09.2026, and
    // demanding two digits reported three places that carried their date all along.
    private val date = Regex("""\d{1,2}\.\d{1,2}\.\d{4}""")

    /**
     * sentences invoking the one device, in both languages.
     *
     * english belongs here since the source speaks it: a rule that knows only the german
     * wording would quietly stop measuring as the translation moves on - the same defect it
     * exists to catch.
     */
    private val invokes = listOf(
        "Telefon des Nutzers", "Gerät des Nutzers", "Geraet des Nutzers",
        "on the user's phone", "on the user's device", "on this phone", "on this device",
    )

    @Test
    fun `whoever cites the device names the date`() {
        val withoutDate = (Quelltext.files() + Quelltext.testFiles())
            // the rule itself talks about such sentences instead of making any.
            .filterNot { it.name == "MeasuredFactTest.kt" }
            .flatMap { file ->
                val lines = file.readLines()
                lines.withIndex()
                    .filter { (_, line) -> invokes.any { it in line } }
                    .filterNot { (i, _) ->
                        // the date may stand in the same paragraph, not only the same line -
                        // a paragraph wraps after five lines at the latest.
                        lines.subList(maxOf(0, i - 4), minOf(lines.size, i + 5))
                            .any { date.containsMatchIn(it) }
                    }
                    .map { (i, line) -> "${file.name}:${i + 1}: ${line.trim().take(80)}" }
            }
        assertEquals(
            "a fact about the user's device stands here without a date. such sentences age " +
                "quietly: six of them were false at once when HomeTiles got two roles.",
            emptyList<String>(),
            withoutDate,
        )
    }

    /** and the rule finds anything at all - otherwise it checks nothing. */
    @Test
    fun `such sentences really exist`() {
        val hits = (Quelltext.files() + Quelltext.testFiles())
            .count { file -> invokes.any { it in file.readText() } }
        assertTrue(
            "not a single place cites the device any more - does the rule still read what " +
                "it means?",
            hits >= 5,
        )
    }
}
