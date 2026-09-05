package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a branch whose two ways do the same thing is a forgotten intention.
 *
 * the call screen had `if (view.speakerOn) CallAction.SPEAKER else CallAction.SPEAKER`: a
 * button that switched the speaker on and never off again. the compiler says nothing, and no
 * test fell for it - both branches did deliver the expected result.
 */
class DeadBranchTest {

    private val pattern = Regex("""\bif\s*\(.+?\)\s+(.+?)\s+else\s+(.+)""")

    private fun sources(): List<File> =
        Quelltext.files()

    /** trailing commas and closing brackets are not part of the branch. */
    private fun clean(branch: String): String = branch.trim().trimEnd(',', ')')

    @Test
    fun `no branch with two equal ways`() {
        val equal = mutableListOf<String>()
        sources().forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val hit = pattern.find(line) ?: return@forEachIndexed
                val left = clean(hit.groupValues[1])
                val right = clean(hit.groupValues[2])
                // an "if" on the right is a chain, not a doubled way.
                if (right.startsWith("if")) return@forEachIndexed
                if (left.isNotEmpty() && left == right) {
                    equal += "${file.name}:${index + 1}: ${line.trim()}"
                }
            }
        }
        assertTrue(
            "both branches do the same - half an intention is missing:\n" +
                equal.joinToString("\n"),
            equal.isEmpty(),
        )
    }
}
