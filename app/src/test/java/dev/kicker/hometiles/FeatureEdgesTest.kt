package dev.kicker.hometiles

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * no area knows another one - except the settings tree.
 *
 * `PLAN.md` 2.1 wants the app cut into `feature:*` modules, and the rule there is that a
 * `feature:*` never imports another. of 58 violations at the first count, 45 were none but
 * building blocks still lying in a screen's package; after they moved to `core:ui` and
 * `core:model`, **13** were left and all start from the settings tree.
 *
 * that tree is the one open design question (see `STATUS.md`): either `feature:settings` may
 * know every area, or every area brings its own settings page. this rule does **not** decide
 * it. it only holds what applies without a decision: everything else is free and shall stay so.
 *
 * what falls over here is usually not a fault but a new part in the wrong place. shared things
 * belong in `core:*` - the way there has never cost a single import line, because the package
 * stays and only the module changes.
 */
class FeatureEdgesTest {

    /** area in `:app` to the module it belongs to by PLAN.md 2.1. */
    private val belongsTo = mapOf(
        "tiles" to "home", "apps" to "home", "widgets" to "home", "shortcuts" to "home",
        "ui" to "home",
        "phone" to "phone", "contacts" to "phone",
        "sms" to "sms", "notify" to "sms",
        "settings" to "settings", "wizard" to "settings",
        "toggles" to "sos", "actions" to "sos", "safety" to "sos",
        // a11y, search, security, web and info cut across and belong to no module.
    )

    @Test
    fun `only the settings tree knows other areas`() {
        // MainActivity is the shell and may know every screen.
        val edges = Areas.edges(without = setOf("MainActivity.kt"))
        val violations = edges.entries.flatMap { (pair, places) ->
            val from = belongsTo[pair.first]
            val to = belongsTo[pair.second]
            if (from == null || to == null || from == to || from == "settings") {
                emptyList()
            } else {
                places.map { "$from -> $to  $it" }
            }
        }.sorted()

        assertEquals(
            "an area knows another one that later becomes a module of its own. shared things " +
                "belong in core:* - keep the package, change the module, and no import line " +
                "changes.",
            emptyList<String>(),
            violations,
        )
    }

    /**
     * and the settings tree does not grow unnoticed. the number is not a target but a note:
     * raising it makes the open question more expensive.
     */
    @Test
    fun `the settings tree's edges stay counted`() {
        val edges = Areas.edges(without = setOf("MainActivity.kt"))
        val fromSettings = edges.entries
            .filter { (pair, _) -> belongsTo[pair.first] == "settings" && belongsTo[pair.second] != null && belongsTo[pair.second] != "settings" }
            .flatMap { it.value }
            .map { Quelltext.cut(it, ": ") }
            .toSortedSet()

        assertEquals(
            "the settings tree knows more or fewer areas than counted. fewer is good - then " +
                "shorten this list. more makes the open question in STATUS.md more expensive.",
            // down from three to two: a repository moved to `core:system` with its service.
            // what is left are exactly the two settings pages - the design question itself.
            sortedSetOf("MessagesSettingsList", "SosSettings"),
            fromSettings,
        )
    }
}
