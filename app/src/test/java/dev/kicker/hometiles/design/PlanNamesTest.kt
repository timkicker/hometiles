package dev.kicker.hometiles.design

import java.io.File
import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the plan names no class that does not exist.
 *
 * `PLAN.md` puts type names in backticks - 92 of them on 03.09.2026. three pointed at
 * nothing: `HomeViewModel` and `HomeUiState` (section 2.3), a whole architecture with its
 * reasoning that was never built, and `NotificationsListenerService`, one letter too many -
 * the android class is called `NotificationListenerService`. a typo in an api name sends the
 * reader to a search engine and from there nowhere.
 *
 * not every name has to be ours. those stand below, with a reason.
 */
class PlanNamesTest {

    /** name -> why it may stand in the plan although it does not appear in the source. */
    private val fromOutside = mapOf(
        "BigLau" to
            "the name the app carried until 06.09.2026. section 9.5 records the rename, so " +
            "it has to be nameable there while being gone from the source - which is the " +
            "point of that entry.",
        "BadgesProvider" to
            "the original launcher's solution for blinking tiles - its own provider with " +
            "its own permission for the sister apps. we deliberately do it differently.",
        "FastOutSlowIn" to
            "an easing curve from compose itself, named as the default for the motion when " +
            "changing screens.",
        "LazyGrid" to
            "stands there as an explicit refusal: the grid is a `Layout` of our own, because " +
            "spanning cells do not work in a LazyGrid.",
        "SPALTENxZEILEN" to
            "no type but a placeholder in the description of what the original offers as a " +
            "grid input. it stays german because the plan is german.",
        "SHA256SUMS" to
            "no type but the name of the checksum file the release workflow puts beside the " +
            "apk. the convention writes it in capitals, which is what makes it look like one.",
    )

    private val plan = File("../PLAN.md").readText()

    private fun named(): List<String> =
        Regex("""`([A-Z][A-Za-z0-9]{3,})`""").findAll(plan).map { it.groupValues[1] }
            .distinct().sorted().toList()

    @Test
    fun `every named type appears in the source`() {
        val everything = (Quelltext.files() + Quelltext.testFiles()).joinToString("\n") { it.readText() }
        val ghosts = named().filterNot { it in fromOutside }.filterNot { it in everything }
        assertEquals(
            "PLAN.md names something the source does not have. either it is out of date, a " +
                "typo, or it belongs to somebody else - then into the list in PlanNamesTest " +
                "with a reason.",
            emptyList<String>(),
            ghosts,
        )
    }

    @Test
    fun `every exception names its reason and stands in the plan too`() {
        fromOutside.forEach { (name, reason) ->
            assertTrue("$name: reason missing or too short", reason.length > 40)
            assertTrue("$name no longer stands in the plan - then the exception can go", name in plan)
        }
    }

    @Test
    fun `the rule finds any names at all`() {
        assertTrue("only ${named().size} names found - is it still looking?", named().size >= 50)
    }
}
