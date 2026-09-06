package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * a rule cutting in the source falls over when the mark is gone, instead of staying green.
 *
 * `substringAfter("X")` returns **the whole text** when `X` is missing, and `substringBefore`
 * too. the rule then no longer cuts out the section it means but keeps everything - and finds
 * its keyword somewhere else. it happened twice in one night: `SelectionAnnouncementTest` took
 * the tint picker along (the end mark lay behind the section), and `ForeignIntentTest` would
 * have searched the whole rest of the file after a renamed variable. both were green and
 * checked nothing.
 *
 * `Quelltext.cut` throws in that case. eighty cut sites are switched over; this rule holds the
 * last three where the old form is right.
 */
class LoudCutsTest {

    /**
     * where `substringAfter`/`substringBefore` may stay, and why.
     *
     * `DeadTileTest` passes the fallback `""` - with the mark missing nothing comes back
     * instead of everything, and the rule falls over by itself. `SearchKeyLockTest` cuts a
     * line it has checked beforehand contains the mark. `AddressFormTest` cuts a sentence and
     * not source: there "no space, take the whole word" is exactly right.
     */
    private val allowed = mapOf(
        "DeadTileTest.kt" to "passes a fallback",
        "SearchKeyLockTest.kt" to "checks the mark beforehand",
        "AddressFormTest.kt" to "cuts a sentence, not source",
    )

    @Test
    fun `rules cut at a mark that has to exist`() {
        val silent = Quelltext.testFiles()
            .filterNot { it.name in allowed }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { number, line ->
                    val text = line.trim()
                    if (Quelltext.isCommentLine(line)) {
                        null
                    } else if (Regex("""\.substring(After|Before)\(""").containsMatchIn(line)) {
                        "${file.name}:${number + 1}: $text"
                    } else {
                        null
                    }
                }
            }
        assertEquals(
            "something is cut here at a mark that will one day be gone - then the cut " +
                "delivers the whole text and the rule stays green without checking anything. " +
                "use Quelltext.cut; that one falls over:\n" + silent.joinToString("\n"),
            emptyList<String>(),
            silent,
        )
    }

    /**
     * and a text is looked for in **all** modules, not only the first.
     *
     * `Quelltext.texts(language).first()` is `:app` and nothing else. texts have moved to
     * another module three times, and five rules would have grasped at nothing afterwards.
     * `Quelltext.textValue` looks everywhere and falls over when the text exists nowhere.
     *
     * both spellings: the helper was called `texte` until it was renamed, and the rule kept
     * looking for the old name - green, and blind. the same defect it exists to catch.
     */
    @Test
    fun `a text is looked for in all modules`() {
        val appOnly = Quelltext.testFiles().flatMap { file ->
            file.readLines().mapIndexedNotNull { number, line ->
                // comments do not count: a rule reported its own reasoning as a violation
                // three times in one night.
                val text = line.trim()
                if (Quelltext.isCommentLine(line)) {
                    null
                } else if (Regex("""text[es]\([^)]*\)\s*\.first\(\)""").containsMatchIn(line)) {
                    "${file.name}:${number + 1}: $text"
                } else {
                    null
                }
            }
        }
        assertEquals(
            "a text is looked for in :app only here. if it moves to another module the rule " +
                "grasps at nothing. use Quelltext.textValue:\n" + appOnly.joinToString("\n"),
            emptyList<String>(),
            appOnly,
        )
    }
}
