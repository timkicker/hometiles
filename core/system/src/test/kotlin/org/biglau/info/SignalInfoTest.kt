package org.biglau.info

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * the signal tile.
 *
 * four states, because they lead to different actions: no card, no network, weak reception,
 * reception in order. an empty bar for all three of the first cases does not say what to do.
 */
class SignalInfoTest {

    private fun reading(
        level: Int = 3,
        hasSim: Boolean = true,
        inService: Boolean = true,
        roaming: Boolean = false,
        networkType: String = "4G",
    ) = SignalReading(
        level = level,
        mayRead = true,
        hasSim = hasSim,
        inService = inService,
        roaming = roaming,
        networkType = networkType,
    )

    @Test
    fun `without a card everything else is beside the point`() {
        val without = reading(level = 4, hasSim = false)
        assertEquals(SignalInfo.State.NO_SIM, SignalInfo.stateOf(without))
        assertEquals(0, SignalInfo.bars(without))
        assertEquals("", SignalInfo.caption(without))
    }

    @Test
    fun `a card without a network is something else than no reception`() {
        val none = reading(level = 2, inService = false)
        assertEquals(SignalInfo.State.NO_SERVICE, SignalInfo.stateOf(none))
        assertEquals(0, SignalInfo.bars(none))
    }

    @Test
    fun `one bar or none means weak`() {
        assertEquals(SignalInfo.State.WEAK, SignalInfo.stateOf(reading(level = 0)))
        assertEquals(SignalInfo.State.WEAK, SignalInfo.stateOf(reading(level = 1)))
        assertEquals(SignalInfo.State.OK, SignalInfo.stateOf(reading(level = 2)))
    }

    @Test
    fun `unknown counts as empty, not as full`() {
        // getLevel returns -1 when the network reports nothing. a negative value as bars
        // would be a crash or a full bar - both wrong.
        assertEquals(0, SignalInfo.bars(reading(level = -1)))
    }

    @Test
    fun `there are no more than four bars`() {
        assertEquals(SignalInfo.MAX_LEVEL, SignalInfo.bars(reading(level = 9)))
    }

    @Test
    fun `roaming stands on the tile`() {
        // it costs money, and in the status bar of a three-inch device one misses it.
        assertEquals("R 4G", SignalInfo.caption(reading(roaming = true)))
        assertEquals("R", SignalInfo.caption(reading(roaming = true, networkType = "")))
        assertEquals("4G", SignalInfo.caption(reading(roaming = false)))
    }

    @Test
    fun `without a network no addition stands there`() {
        assertEquals("", SignalInfo.caption(reading(inService = false, roaming = true)))
    }

    @Test
    fun `spaces in the network type do not disturb`() {
        assertEquals("LTE", SignalInfo.caption(reading(networkType = "  LTE  ")))
    }
}

/**
 * "knows nothing" is something else than "no card".
 *
 * on the first run the tile reported no sim card while the card was in and the status bar
 * showed 4G - BigLau simply did not have the read permission. a display claiming a missing
 * card sends the user to open the case.
 */
class SignalPermissionTest {

    @Test
    fun `without read permission the tile says so`() {
        val without = SignalReading(
            level = -1,
            mayRead = false,
            hasSim = false,
            inService = false,
            roaming = false,
            networkType = "",
        )
        assertEquals(SignalInfo.State.NO_PERMISSION, SignalInfo.stateOf(without))
        assertEquals(0, SignalInfo.bars(without))
        assertEquals("", SignalInfo.caption(without))
    }

    @Test
    fun `the permission comes before everything else`() {
        // even if values came from somewhere: without the permission they cannot be trusted.
        val contradiction = SignalReading(
            level = 4,
            mayRead = false,
            hasSim = true,
            inService = true,
            roaming = false,
            networkType = "4G",
        )
        assertEquals(SignalInfo.State.NO_PERMISSION, SignalInfo.stateOf(contradiction))
    }

    @Test
    fun `with permission and without a card it stays at no card`() {
        val withoutCard = SignalReading(
            level = -1,
            mayRead = true,
            hasSim = false,
            inService = false,
            roaming = false,
            networkType = "",
        )
        assertEquals(SignalInfo.State.NO_SIM, SignalInfo.stateOf(withoutCard))
    }
}
