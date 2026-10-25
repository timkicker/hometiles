package dev.kicker.hometiles.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** issue #2: lists for a small flip phone, on where the phone has keys, off on touch phones. */
class KeyFirstTest {

    @Test
    fun `automatic is the default and follows the keys`() {
        assertEquals(KeyFirst.AUTO, Behaviour().keyFirst)
        assertTrue(KeyFirst.AUTO.isOn(phoneHasKeys = true))
        assertFalse(KeyFirst.AUTO.isOn(phoneHasKeys = false))
    }

    @Test
    fun `on and off hold whatever the phone has`() {
        assertTrue(KeyFirst.ON.isOn(phoneHasKeys = false))
        assertFalse(KeyFirst.OFF.isOn(phoneHasKeys = true))
    }

    // a config from before the setting has none and must read as automatic.
    @Test
    fun `an older config reads as automatic`() {
        val old = configJson.decodeFromString(Behaviour.serializer(), """{ "pressMode": "SHORT" }""")
        assertEquals(KeyFirst.AUTO, old.keyFirst)
    }
}
