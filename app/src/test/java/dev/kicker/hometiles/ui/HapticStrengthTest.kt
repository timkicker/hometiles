package dev.kicker.hometiles.ui

import kotlinx.serialization.json.Json
import dev.kicker.hometiles.data.Behaviour
import dev.kicker.hometiles.data.HapticStrength
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * the haptics have three levels instead of two. the old field still stands in every
 * configuration written before this version - it must not be lost silently. whoever had the
 * haptics switched off does not want them suddenly back.
 */
class HapticStrengthTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `an old configuration with haptics on becomes light`() {
        val old = json.decodeFromString<Behaviour>("""{"hapticFeedback":true}""")
        assertEquals(HapticStrength.LIGHT, old.haptics)
    }

    @Test
    fun `an old configuration with haptics off stays off`() {
        val old = json.decodeFromString<Behaviour>("""{"hapticFeedback":false}""")
        assertEquals(HapticStrength.OFF, old.haptics)
    }

    @Test
    fun `the new field beats the old one`() {
        val mixed = json.decodeFromString<Behaviour>(
            """{"hapticFeedback":false,"hapticStrength":"STRONG"}"""
        )
        assertEquals(HapticStrength.STRONG, mixed.haptics)
    }

    // otherwise an export from here would mean the opposite in an older version.
    @Test
    fun `withHaptics keeps both fields equal`() {
        val off = Behaviour().withHaptics(HapticStrength.OFF)
        assertEquals(false, off.hapticFeedback)
        assertEquals(HapticStrength.OFF, off.haptics)

        val strong = off.withHaptics(HapticStrength.STRONG)
        assertEquals(true, strong.hapticFeedback)
        assertEquals(HapticStrength.STRONG, strong.haptics)
    }

    @Test
    fun `the levels run in a circle`() {
        var level = HapticStrength.OFF
        level = Haptics.next(level); assertEquals(HapticStrength.LIGHT, level)
        level = Haptics.next(level); assertEquals(HapticStrength.STRONG, level)
        level = Haptics.next(level); assertEquals(HapticStrength.OFF, level)
    }

    // without a field: light. a fresh installation should make itself felt.
    @Test
    fun `the default is light`() {
        assertEquals(HapticStrength.LIGHT, Behaviour().haptics)
    }
}
