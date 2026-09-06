package dev.kicker.hometiles.toggles

import dev.kicker.hometiles.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * why nothing went out belongs on the screen.
 *
 * played through with the sms permission withdrawn, so nothing could go out: after the
 * countdown the screen said that nothing could be sent - the same sentence a network fault
 * would have produced. on the screen that is the last one in an emergency that is too
 * little: a missing permission, nobody entered, and a network that did not play along are
 * three different things, and only the first is one the person in front of it can do
 * something about.
 *
 * the second half belongs to it: the permission is now asked for only when it really is
 * missing. before, the screen asked after **every** failure and so blamed something that was
 * not missing at all.
 */
class SosFailureTest {

    @Test
    fun `a missing permission gets a sentence of its own`() {
        assertEquals(R.string.sos_failed_permission, Sos.failureText(SosFailure.NO_PERMISSION))
    }

    @Test
    fun `without contacts the hint points at the contacts`() {
        assertEquals(R.string.sos_not_configured, Sos.failureText(SosFailure.NO_NUMBERS))
    }

    @Test
    fun `otherwise it stays at the general sentence`() {
        assertEquals(R.string.sos_failed, Sos.failureText(SosFailure.SEND_FAILED))
        assertEquals(R.string.sos_failed, Sos.failureText(SosFailure.NONE))
    }

    @Test
    fun `a successful send has no reason to fail`() {
        assertEquals(SosFailure.NONE, SosResult(sent = 2, failed = 0, hadLocation = true).failure)
    }

    @Test
    fun `without a sent message the reason is set`() {
        val result = SosResult(sent = 0, failed = 1, hadLocation = false)
        assertTrue(result.failure != SosFailure.NONE)
    }
}
