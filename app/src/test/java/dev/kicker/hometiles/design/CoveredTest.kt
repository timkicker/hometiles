package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * what is covered does not exist for the screen reader either.
 *
 * the home screen and its overlays - an open folder, the large tile label, the pin gate, the
 * contact choice, the signal explainer - lie over each other as siblings. the folder covers
 * everything, and the tiles below still stood in the accessibility view: fifty nodes in the
 * dump, twenty-nine after the fix and only the folder itself.
 *
 * whoever has it read aloud walked through tiles they cannot see and started an app with a
 * double tap that is not standing there at all.
 */
class CoveredTest {

    private val home = Quelltext.withoutComments("dev/kicker/hometiles/MainActivity.kt")

    @Test
    fun `the home screen vanishes while something lies over it`() {
        assertTrue(
            "the home screen is not taken out of the accessibility view while an overlay is " +
                "open.",
            "clearAndSetSemantics {}" in home,
        )
    }

    /**
     * and **every** overlay counts. the fault comes back as soon as someone adds a new one
     * and forgets the condition, so it is counted here: everything shown conditionally after
     * the home screen has to stand in the condition.
     */
    @Test
    fun `every overlay stands in the condition`() {
        val condition = Quelltext.cut(home, "val covered = ", "Column(")
        // `then(swipe)` belongs to the home screen and only to it - its swipe gesture between
        // screens. an earlier mark broke when the call it named grew to two lines; an
        // expression that **can** only occur in this one place holds longer.
        val after = Quelltext.cut(home, "then(swipe)", "private fun")
        // two spellings, not one. with only the first, the menu key's list arrived as
        // `tileMenu.value?.let { ... }`, was not counted and was missing from the condition:
        // with the list open, three tiles still stood in the node dump. a rule that knows one
        // of two forms stays green and checks half.
        val forms = listOf(
            Regex("""\n {16}if \((\w+(?:\.\w+)*) != null\) \{"""),
            Regex("""\n {16}(\w+(?:\.\w+)*)\?\.let \{"""),
        )
        val missing = forms
            .flatMap { form -> form.findAll(after).map { it.groupValues[1] } }
            .map { it.removeSuffix(".value") }
            .filterNot { it in condition }
            .distinct()
            .toList()
        assertEquals(
            "these overlays are missing from the condition - while they are open the home " +
                "screen below stays in the screen reader: $missing",
            emptyList<String>(),
            missing,
        )
    }
}
