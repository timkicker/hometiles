package org.biglau

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a startup time without its method is no measurement.
 *
 * two numbers in `PLAN.md` carried a wrong label, and it showed only on remeasuring:
 *
 * * `am start -W` measures no cold start for a **home app**. android pulls BigLau back up
 *   within a second after `force-stop`, so what gets measured is a process already standing.
 *   on the Jelly 2 that was 1189 ms too little. `-S` stops the app as part of the start.
 * * `cmd package compile -m speed` reports `Success` on a **debuggable** app and leaves the
 *   state at `quicken`. so the number was never "after full compilation".
 *
 * both are faults one cannot look at: the command runs through, the number looks plausible.
 * hence the condition under which such a number may stand in the plan - it has to bring its
 * method along.
 */
class StartupNumbersTest {

    private val COMMAND = "compile -m speed"

    private val plan = File("../PLAN.md").readText()
    private val status = File("../STATUS.md").readText()

    /** paragraphs: separated by empty lines. that is as far as a label reaches. */
    private fun paragraphs(text: String): List<String> = text.split(Regex("\n[ \t]*\n"))

    @Test
    fun `the plan measures only with an explicit stop`() {
        val withoutStop = paragraphs(plan)
            .filter { "am start -W" in it }
            .filterNot { "-S" in it }
        assertTrue(
            "`am start -W` stands in PLAN.md and `-S` does not appear in the same " +
                "paragraph: " + withoutStop.joinToString("\n---\n") +
                "\nBigLau is the home app. android pulls it back up at once after " +
                "`force-stop`, and `am start -W` then measures a running process - on the " +
                "Jelly 2 1189 ms too cheap.",
            withoutStop.isEmpty(),
        )
    }

    @Test
    fun `where a startup time stands the compilation state stands beside it`() {
        val number = Regex("\\*\\*[0-9]{3,4} ms\\*\\*")
        val bare = paragraphs(plan)
            .filter { number.containsMatchIn(it) && "start" in it.lowercase() }
            .filterNot { "run-from-apk" in it && "quicken" in it }
        assertTrue(
            "a startup time in PLAN.md does not name its compilation state: " +
                bare.joinToString("\n---\n") +
                "\nthe same app starts on the Jelly 2 in 3018 ms (`run-from-apk`) or in " +
                "688 ms (`quicken`). without the state the number is arbitrary.",
            bare.isEmpty(),
        )
    }

    @Test
    fun `nobody claims full compilation for the debug build`() {
        val lies = mutableListOf<String>()
        for (text in listOf(plan, status)) {
            var i = text.indexOf(COMMAND)
            while (i >= 0) {
                // a window instead of a paragraph: the correction stands sometimes on the
                // same line, sometimes in the heading above, sometimes in the paragraph
                // after.
                val around = text.substring(
                    maxOf(0, i - 400),
                    minOf(text.length, i + 800),
                )
                if ("quicken" !in around) lies += around
                i = text.indexOf(COMMAND, i + 1)
            }
        }
        assertTrue(
            "a paragraph names `cmd package compile -m speed` without saying `quicken`: " +
                lies.joinToString("\n---\n") +
                "\non a debuggable app the command reports `Success` and changes nothing; " +
                "the state stays `quicken`. whoever leaves that out notes a compilation " +
                "that never happened.",
            lies.isEmpty(),
        )
    }
}
