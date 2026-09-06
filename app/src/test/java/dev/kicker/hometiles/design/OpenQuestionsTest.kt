package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * an open question long since answered costs more than none.
 *
 * `PLAN.md` 9 listed two as open that were not: the sim card (it had been in the phone for
 * days) and the licence (the `LICENSE` lies in the root and the readme names it). whoever
 * reads such a list takes a finished thing for homework and asks again.
 *
 * this rule checks the licence question, because it can be pinned to the repository: if a
 * `LICENSE` exists, the question must not stand there as open.
 */
class OpenQuestionsTest {

    private val plan = File("../PLAN.md").readText()

    /**
     * up to the next heading, not to the end of the file.
     *
     * without an end mark the cut ran to the end of `PLAN.md` and took everything after it
     * along. that went unnoticed while section 10 held no numbered list; as soon as one
     * arrived, the rule reported questions as wrongly numbered that are no questions at all.
     *
     * a cut without an end is no cut. the rule had always measured the whole rest of the
     * document and only by chance found nothing.
     *
     * the marks are german because they cut in PLAN.md, which stays german.
     */
    private val questions = Quelltext.cut(plan, "## 9. Offene Fragen", "## 10.")

    @Test
    fun `the licence question no longer stands open`() {
        val licence = File("../LICENSE")
        assertTrue("there is no LICENSE any more - then the question may be open again.", licence.isFile)
        assertTrue(
            "the LICENSE is not the gpl - then the entry in PLAN.md 9 is no longer right.",
            "GNU GENERAL PUBLIC LICENSE" in licence.readText().take(200),
        )
        val line = questions.lineSequence().firstOrNull { it.contains("**Lizenz") || it.contains("~~Lizenz~~") }
            ?: throw AssertionError("the licence line is gone from PLAN.md 9")
        assertTrue(
            "PLAN.md 9 still asks about the licence although the LICENSE lies in the " +
                "repository and the readme names it: $line",
            line.startsWith("4. ~~Lizenz~~"),
        )
    }

    /** and the numbers run through - there were two fives once. */
    @Test
    fun `the questions are numbered consecutively`() {
        val numbers = Regex("""^(\d+)\. """, RegexOption.MULTILINE)
            .findAll(questions).map { it.groupValues[1].toInt() }.toList()
        assertTrue("no numbered questions found", numbers.size >= 4)
        assertEquals(
            "the questions in PLAN.md 9 are not numbered consecutively - giving a number " +
                "twice makes referring to it impossible.",
            (1..numbers.size).toList(),
            numbers,
        )
    }
}
