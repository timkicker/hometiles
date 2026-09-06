package dev.kicker.hometiles

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * nothing personal goes into the repository.
 *
 * on 06.09.2026, one command before making this repository public, three real mobile
 * numbers from the owner's address book were still sitting in it - in the working log, in a
 * source comment, and in 815 commits behind them. they were found by looking, by hand, by
 * luck. looking by hand is not a check.
 *
 * the search itself lives in `tools/opsec.py`, in one place: the patterns, the allow-list
 * and the reasons. this rule runs it, so the same thing is never written twice and drifts.
 * the tool also scans the whole git history - that part is too slow for here and belongs in
 * the workflow before a release, because pushing publishes every commit at once.
 */
class OpsecTest {

    private fun run(vararg args: String): Pair<Int, String> {
        val process = ProcessBuilder(listOf("python3", "tools/opsec.py") + args)
            .directory(File(".."))
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        return process.waitFor() to output
    }

    /**
     * no skipping when python is missing.
     *
     * a rule that quietly passes where it cannot run is the exact shape of the fault it is
     * here to prevent: green, and nothing checked.
     */
    @Test
    fun `the search itself is there and can run`() {
        assertTrue("tools/opsec.py is gone", File("../tools/opsec.py").isFile)
        val (code, output) = run("probe")
        assertEquals("python3 is missing or the tool is broken:\n$output", 0, code)
    }

    /**
     * the counter-check: does the search still find what it searches for?
     *
     * `tools/opsec.py probe` holds one dirty sentence per kind and reports which kinds it
     * finds. a pattern broken by an edit would otherwise turn this rule green by finding
     * nothing anywhere.
     */
    @Test
    fun `every kind of finding is still found`() {
        val (_, output) = run("probe")
        assertTrue(
            "a pattern no longer finds its own sample:\n$output",
            "NOT FOUND" !in output,
        )
    }

    @Test
    fun `no personal data in the tracked files`() {
        val (code, output) = run("tree")
        assertEquals(
            "the search found something in a tracked file. either it goes, or it goes into " +
                "ERLAUBT in tools/opsec.py with a reason:\n$output",
            0,
            code,
        )
    }
}
