package dev.kicker.hometiles.a11y

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenOffHomeTest {

    @Test
    fun `screen off goes home only when switched on`() {
        assertTrue(ScreenOffHome.goHome(enabled = true, audioMode = 0))
        assertFalse(ScreenOffHome.goHome(enabled = false, audioMode = 0))
    }

    @Test
    fun `never while it rings or a call runs`() {
        // 1 ringtone, 2 in call, 3 in communication, 4 call screening.
        (1..4).forEach { mode -> assertFalse(ScreenOffHome.goHome(enabled = true, audioMode = mode)) }
    }
}
