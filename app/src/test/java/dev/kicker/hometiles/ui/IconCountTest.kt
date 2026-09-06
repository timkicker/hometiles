package dev.kicker.hometiles.ui

import java.io.File
import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the plan names the number of icons shipped - the real one.
 *
 * PLAN.md 1.1 rests a decision on it: the original has only about 11 built-in icons and loads
 * the rest from theme APKs; we ship everything so nobody has to install anything. until
 * 3.9.2026 it said about 120 there. built are 56.
 *
 * the number carries the argument, so it has to be right, and this rule counts it: the number
 * in the plan against the entries in [IconCatalogue.GROUPS].
 */
class IconCountTest {

    private val plan = File("../PLAN.md").readText()

    private fun catalogue(): List<String> {
        val source = Quelltext.file("dev/kicker/hometiles/ui/IconCatalogue.kt").readText()
        val from = Quelltext.cut(source, "val GROUPS")
        return Regex("""listOf\(([^)]*)\)""").findAll(from)
            .flatMap { Regex(""""([A-Za-z]+)"""").findAll(it.groupValues[1]) }
            .map { it.groupValues[1] }
            .toList()
    }

    @Test
    fun `the number in the plan matches the catalogue`() {
        // the pattern is german: it cuts into the german plan.
        val named = Regex("""Gebaut sind \*\*(\d+)\*\* in (\w+) Gruppen""").find(plan)
            ?: throw AssertionError("the icon line in PLAN.md 1.1 names no number any more")
        assertEquals(
            "PLAN.md names a different number than IconCatalogue.GROUPS yields. the number " +
                "carries an argument there (no theme APKs needed) - it has to be right.",
            catalogue().size,
            named.groupValues[1].toInt(),
        )
    }

    @Test
    fun `no icon stands twice in the catalogue`() {
        val all = catalogue()
        val duplicates = all.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.sorted()
        assertEquals(
            "an icon stands in two groups. whoever looks for it finds it twice and does not " +
                "know whether it is the same one.",
            emptyList<String>(),
            duplicates,
        )
    }

    @Test
    fun `there are clearly more than the eleven of the original`() {
        assertTrue(
            "only ${catalogue().size} icons - the argument from PLAN.md 1.1 (the original " +
                "has about 11 and loads more) does not carry then.",
            catalogue().size >= 40,
        )
    }
}
