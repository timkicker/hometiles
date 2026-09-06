package dev.kicker.hometiles.actions

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * in an sos the position of **now** counts, not yesterday's.
 *
 * the sos message took the last known position. on a phone lying in a pocket that is often
 * hours old - or there is none at all, because no app happened to ask for a long while. on
 * the emulator it was **always** empty (`last location=null`), and that is what the first
 * check of the map link failed on: `adb emu geo fix` alone does not fill it.
 *
 * the countdown is the window for it: it lasts some seconds anyway, in which the phone can
 * search. afterwards the request is unregistered - a receiver that keeps running costs
 * power.
 *
 * with the search during the countdown the fix arrived at once, and the coordinates in the
 * map link carried a **dot** as the decimal separator on a german interface. that had only
 * been computed before, never seen.
 */
class SosLocationTest {

    private val screen =
        Quelltext.withoutComments("dev/kicker/hometiles/toggles/SosActivity.kt")

    @Test
    fun `during the countdown it searches`() {
        assertTrue("no location in the sos screen", "SosLocation(" in screen)
        assertTrue("the search does not begin", "locator.start()" in screen)
    }

    @Test
    fun `only when the position is to be sent along at all`() {
        assertTrue(
            "it locates even when nobody wants the position",
            "if (sos.sendLocation) locator.start()" in screen,
        )
    }

    @Test
    fun `the search stops again`() {
        val cleanup = Quelltext.cut(screen, "onDispose {", "}")
        assertTrue("the location keeps running: $cleanup", "locator.stop()" in cleanup)
    }
}
