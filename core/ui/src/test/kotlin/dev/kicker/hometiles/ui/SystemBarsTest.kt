package dev.kicker.hometiles.ui

import dev.kicker.hometiles.data.Appearance
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.2 "Statusleiste: sichtbar / Vollbild", quoted in the plan's german.
 *
 * here the two bars together come to about forty of 605 dp - seven percent the tiles do not
 * get. on a large phone that would be ornament.
 */
class SystemBarsTest {

    @Test
    fun `the default shows the bars`() {
        assertEquals(false, Appearance().fullScreen)
        assertEquals(SystemBars.Behaviour.VISIBLE, SystemBars.behaviourFor(false))
    }

    /**
     * the case that matters: hiding yes, locking away no.
     *
     * whoever takes the bars away and locks the pull-down with them locks the notifications
     * away - and whoever does not know one can swipe for them never gets at them again. a few
     * dp are not worth that, so there is no state at all that hides them hard.
     */
    @Test
    fun `hidden always means swipe-to-show`() {
        assertEquals(SystemBars.Behaviour.HIDDEN_SWIPE_SHOWS, SystemBars.behaviourFor(true))
        assertEquals(2, SystemBars.Behaviour.entries.size)
    }
}
