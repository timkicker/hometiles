package dev.kicker.hometiles.design

import java.io.File
import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * every explanation stands beside what it explains.
 *
 * in this project the comments carry the reasoning - why a confirmation is in two steps, why
 * a number stands there. a reason above the wrong function is worse than none: it is
 * believed. the compiler notices nothing of it, and reading catches it only if one knows
 * both places.
 */
class DocPlacementTest {

    private val sources: List<File> = Quelltext.files() + Quelltext.testFiles()

    @Test
    fun `no doc block stands on another`() {
        val stacked = mutableListOf<String>()
        sources.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                val next = lines.getOrNull(index + 1)?.trim() ?: return@forEachIndexed
                if (line.trim().endsWith("*/") && next.startsWith("/**")) {
                    stacked += "${file.name}:${index + 2}"
                }
            }
        }
        assertEquals(
            "an explanation stands above another one here - so above the wrong thing: " +
                "$stacked",
            emptyList<String>(),
            stacked,
        )
    }

    @Test
    fun `no doc block stands at the end of a block`() {
        // the other case: the function described is gone, the explanation stayed.
        val orphaned = mutableListOf<String>()
        sources.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                val next = lines.getOrNull(index + 1)?.trim() ?: return@forEachIndexed
                if (line.trim().endsWith("*/") && (next == "}" || next.isEmpty())) {
                    orphaned += "${file.name}:${index + 2}"
                }
            }
        }
        assertEquals(
            "this explanation describes nothing any more: $orphaned",
            emptyList<String>(),
            orphaned,
        )
    }

    @Test
    fun `the rule finds an invented stack`() {
        // counter-check on an invented file, so a broken comparison shows.
        val lines = listOf(" */", "/** second block */", "fun x() = 1")
        val hits = lines.filterIndexed { index, line ->
            line.trim().endsWith("*/") && (lines.getOrNull(index + 1)?.trim()?.startsWith("/**") == true)
        }
        assertTrue("a stack has to show", hits.isNotEmpty())
    }
}
