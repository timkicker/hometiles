package org.biglau.phone

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * what a speed dial does stands where one sets it up.
 *
 * the sentence existed, but only on the keypad and only **once one was assigned**. whoever
 * set up the first one learned afterwards what they had let themselves in for.
 *
 * and it is not just any information: a long press on a speed dial is the **only** place
 * where BigLau dials without asking. `PLAN.md` 3.1, principle 5 demands the confirmation
 * everywhere else, and `CallConfirmTest` holds it in the call log. whether the speed dial
 * itself should ask stands in `STATUS.md` as the user's decision; until then at least what
 * happens has to be written there.
 */
class SpeedDialHintTest {

    private val dialer = Quelltext.withoutComments("org/biglau/phone/DialerActivity.kt")

    @Test
    fun `the assignment says what the long press will do`() {
        // AssignList stands at the end of the file, so there is no next function as a
        // boundary. cutting to the end is right here.
        val list = Quelltext.cut(dialer, "private fun AssignList(")
        assertTrue(
            "the assignment does not name the sentence. one picks a contact and only then " +
                "learns that a long press calls them without asking.",
            "R.string.dialer_speeddial_hint_call" in list,
        )
    }

    /** two versions of the same sentence drift apart. */
    @Test
    fun `both places take the same sentence`() {
        val places = Regex("R\\.string\\.dialer_speeddial_hint_call")
            .findAll(dialer).count()
        assertEquals(
            "the sentence does not stand in both places - keypad and assignment - or it " +
                "stands there in two versions.",
            2,
            places,
        )
    }
}
