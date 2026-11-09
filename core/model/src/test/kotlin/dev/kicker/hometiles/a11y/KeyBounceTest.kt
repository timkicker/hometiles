package dev.kicker.hometiles.a11y

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyBounceTest {

    private val one = 8
    private val two = 9
    private val select = 23

    @Test
    fun `a second press of the same key at once is dropped with its release`() {
        val bounce = KeyBounce()
        assertTrue(bounce.accept(select, down = true, repeatCount = 0, eventTime = 1000))
        assertTrue(bounce.accept(select, down = false, repeatCount = 0, eventTime = 1010))
        assertFalse(bounce.accept(select, down = true, repeatCount = 0, eventTime = 1030))
        assertFalse(bounce.accept(select, down = false, repeatCount = 0, eventTime = 1040))
    }

    @Test
    fun `the same key pressed twice on purpose counts twice`() {
        val bounce = KeyBounce()
        val presses = listOf(1000L, 1000L + KeyBounce.WINDOW_MS).count { at ->
            bounce.accept(one, down = true, repeatCount = 0, eventTime = at).also {
                bounce.accept(one, down = false, repeatCount = 0, eventTime = at + 40)
            }
        }
        assertEquals(2, presses)
    }

    @Test
    fun `two different keys in quick order both count`() {
        val bounce = KeyBounce()
        assertTrue(bounce.accept(one, down = true, repeatCount = 0, eventTime = 1000))
        assertTrue(bounce.accept(one, down = false, repeatCount = 0, eventTime = 1010))
        assertTrue(bounce.accept(two, down = true, repeatCount = 0, eventTime = 1020))
    }

    @Test
    fun `a held key keeps repeating`() {
        val bounce = KeyBounce()
        assertTrue(bounce.accept(one, down = true, repeatCount = 0, eventTime = 1000))
        assertTrue(bounce.accept(one, down = true, repeatCount = 1, eventTime = 1050))
        assertTrue(bounce.accept(one, down = true, repeatCount = 2, eventTime = 1100))
        assertTrue(bounce.accept(one, down = false, repeatCount = 0, eventTime = 1120))
    }

    @Test
    fun `a dropped release does not swallow the next real one`() {
        val bounce = KeyBounce()
        bounce.accept(select, down = true, repeatCount = 0, eventTime = 1000)
        bounce.accept(select, down = false, repeatCount = 0, eventTime = 1010)
        bounce.accept(select, down = true, repeatCount = 0, eventTime = 1030)
        bounce.accept(select, down = false, repeatCount = 0, eventTime = 1040)
        assertTrue(bounce.accept(select, down = true, repeatCount = 0, eventTime = 2000))
        assertTrue(bounce.accept(select, down = false, repeatCount = 0, eventTime = 2010))
    }
}
