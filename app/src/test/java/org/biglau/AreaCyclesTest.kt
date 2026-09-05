package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * which areas of `:app` point at each other in a circle?
 *
 * the module cut from `PLAN.md` 2.1 hangs on exactly this: a module cannot depend on one
 * that depends on it again - as long as `sms` and `notify` need each other there is no
 * `feature:sms`. measured on 03.09.2026 there were four cycles; since then there is none,
 * and this rule keeps it so.
 *
 * only edges between areas **inside** `:app` count. an import whose target has long lain in
 * `core:*` is none - `org.biglau.phone.PhoneNumbers` looks like `:app` by its name and lies
 * in `core:system`.
 */
class AreaCyclesTest {

    /**
     * cycle -> why it still stands and what would dissolve it.
     *
     * empty since 03.09.2026. an entry here is always a debt, never a state.
     */
    private val known = emptyMap<Set<String>, String>()

    @Test
    fun `there are no new circles between the areas`() {
        assertEquals(
            "a new circle between two areas. each one prevents a feature:* cut (PLAN.md " +
                "2.1). either dissolve it, or put it into the list in AreaCyclesTest with a " +
                "reason.",
            known.keys,
            cycles(),
        )
    }

    private fun cycles(): Set<Set<String>> {
        val edges = Areas.edges()
        return edges.filter { (pair, _) -> edges.containsKey(pair.second to pair.first) }
            .keys.map { setOf(it.first, it.second) }.toSet()
    }
}
