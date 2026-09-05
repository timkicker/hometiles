package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the number of edges in the plan is counted, not remembered.
 *
 * `PLAN.md` 2.1 carries a table of the edges between the future `feature:*` modules. it is
 * the measure for the module split: as long as edges stand there, it does not work.
 *
 * measured on 04.09.2026 and found the table out of date - two of four rows named things that
 * no longer existed, one file having moved and one area having been renamed. nobody had
 * noticed, because a table does not fall over.
 *
 * now it does. the same exercise as with the icon count in [org.biglau.ui.IconCountTest]: a
 * number that carries an argument has to be right.
 */
class EdgeCountTest {

    /**
     * read without backticks: the plan sets names in `…`, the measurement delivers them bare.
     * a rule failing on the markup checks the markup.
     */
    private val plan = File("../PLAN.md").readText().replace("`", "")

    /**
     * `MainActivity` stays out of it. it is the home screen and calls every tile action, so it
     * necessarily knows every area - counting it would count the shell, not the split.
     */
    private fun edges() = Areas.edges(without = setOf("MainActivity.kt"))

    @Test
    fun `the plan names the number that is measured`() {
        // the number stands **only** in the plan, not here as well. writing it in both places
        // meant changing it twice an hour later. a number standing twice is one too many.
        //
        // the pattern is german because it reads PLAN.md, not source.
        val inPlan = Regex("""Übrig sind (\d+) Kanten""").find(plan)?.groupValues?.get(1)
        assertTrue(
            "PLAN.md no longer names the number of remaining edges - then the table stands " +
                "there without its measure.",
            inPlan != null,
        )
        assertEquals(
            "the number in the plan is no longer right. either an edge arrived (then it " +
                "belongs in the table with a verdict) or one is gone (then the module split " +
                "is nearer than the plan says).",
            inPlan!!.toInt(),
            edges().size,
        )
    }

    @Test
    fun `every measured edge stands in the table too`() {
        // the arrow is the one the plan's table uses.
        val missing = edges().keys
            .map { (from, to) -> "$from → $to" }
            .filterNot { it in plan }
        assertEquals(
            "this edge is measured but stands in no row of the table. an edge without a " +
                "verdict is one nobody decided about.",
            emptyList<String>(),
            missing,
        )
    }
}
