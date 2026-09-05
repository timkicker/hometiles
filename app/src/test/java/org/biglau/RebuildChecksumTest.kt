package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the checksum in the README names the commit it belongs to.
 *
 * a checksum without a commit is no assurance but a number: it holds for one state of the
 * source, and the next commit makes it uncheckable without it *looking* wrong.
 *
 * this rule does not check the number itself - that costs two full rebuilds,
 * `tools/nachbauen.sh` - but that it is written down so it can be recomputed.
 */
class RebuildChecksumTest {

    private val readme = File("../README.md").readText()

    @Test
    fun `a commit stands beside the checksum`() {
        val sum = Regex("""\b[0-9a-f]{64}\b""").find(readme)
        assertTrue("no sha-256 checksum found in the README", sum != null)

        val around = readme.substring(
            (sum!!.range.first - 400).coerceAtLeast(0),
            (sum.range.last + 400).coerceAtMost(readme.length),
        )
        // the word is capitalised in the README, and the rule wants it that way: a lower
        // case "commit" appears in prose too, and then the rule would find the wrong one.
        val commit = Regex("""Commit\s+`([0-9a-f]{7,40})`""").find(around)
        assertTrue(
            "the checksum in the README names no commit. without it nobody can recompute " +
                "it, and it ages silently.",
            commit != null,
        )
    }

    /**
     * a typo in seven characters catches nobody's eye, and whoever notices notices while
     * recomputing - exactly when they wanted to distrust the number.
     *
     * without `.git` - in a source archive as F-Droid builds it - nothing is checked. a rule
     * that falls there says nothing about the source.
     */
    @Test
    fun `the named commit exists`() {
        if (!File("../.git").exists()) return
        val commit = Regex("""Commit\s+`([0-9a-f]{7,40})`""").find(readme)?.groupValues?.get(1)
        assertTrue("no commit in the README - see the rule above", commit != null)
        val found = runCatching {
            ProcessBuilder("git", "cat-file", "-e", "$commit^{commit}")
                .directory(File(".."))
                .start()
                .waitFor()
        }.getOrNull()
        assertEquals(
            "the README names commit $commit for the checksum, but it does not exist in " +
                "this directory. the number is then not recomputable.",
            0,
            found,
        )
    }

    @Test
    fun `the tool for recomputing exists`() {
        val tool = File("../tools/nachbauen.sh")
        assertTrue("tools/nachbauen.sh is missing, the README names it", tool.isFile)
        assertEquals(true, tool.canExecute())
        assertTrue("the README does not name the tool", "tools/nachbauen.sh" in readme)
    }
}
