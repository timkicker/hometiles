package org.biglau.toggles

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * where it says there are no emergency contacts, the way there stands too.
 *
 * the sos screen says at two places that no contacts are stored: before it starts, and after
 * the **rehearsal**. the first had long offered a button into the sos settings - the way
 * there instead of directions, as the comment beside it says. the second did not, and that is
 * the place where somebody is setting the sos up and trying it out.
 *
 * so the check is: **both** branches that show the sentence offer the jump.
 */
class SosSetupReachableTest {

    private val source = Quelltext.withoutComments("org/biglau/toggles/SosActivity.kt")

    @Test
    fun `every notice about missing contacts carries the jump into the settings`() {
        val notices = Regex("""R\.string\.sos_not_configured""").findAll(source).count()
        // the row stands once as a function and is called twice; the calls are counted, not
        // the definition.
        val ways = Regex("""(?<!fun )SosSetupRow\(\)""").findAll(source).count()
        assertEquals(
            "there are $notices notices about missing emergency contacts but $ways ways into " +
                "the settings. whoever reads the sentence should not only read it.",
            notices,
            ways,
        )
    }
}
