package org.biglau.ui

import kotlinx.serialization.json.Json
import org.biglau.data.Behaviour
import org.biglau.data.HapticStrength
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Die Haptik hat drei Stufen statt zwei. Die alte Angabe steht noch in jeder
 * Konfiguration, die vor dieser Fassung geschrieben wurde - sie darf nicht stumm
 * verlorengehen. Wer die Haptik ausgeschaltet hatte, will sie nicht plötzlich zurück.
 */
class HapticStrengthTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `alte konfiguration mit haptik an wird leicht`() {
        val alt = json.decodeFromString<Behaviour>("""{"hapticFeedback":true}""")
        assertEquals(HapticStrength.LIGHT, alt.haptics)
    }

    @Test
    fun `alte konfiguration mit haptik aus bleibt aus`() {
        val alt = json.decodeFromString<Behaviour>("""{"hapticFeedback":false}""")
        assertEquals(HapticStrength.OFF, alt.haptics)
    }

    @Test
    fun `die neue angabe schlaegt die alte`() {
        val gemischt = json.decodeFromString<Behaviour>(
            """{"hapticFeedback":false,"hapticStrength":"STRONG"}"""
        )
        assertEquals(HapticStrength.STRONG, gemischt.haptics)
    }

    // Sonst bedeutete ein Export von hier in einer aelteren Fassung das Gegenteil.
    @Test
    fun `withHaptics haelt beide felder gleich`() {
        val aus = Behaviour().withHaptics(HapticStrength.OFF)
        assertEquals(false, aus.hapticFeedback)
        assertEquals(HapticStrength.OFF, aus.haptics)

        val kraeftig = aus.withHaptics(HapticStrength.STRONG)
        assertEquals(true, kraeftig.hapticFeedback)
        assertEquals(HapticStrength.STRONG, kraeftig.haptics)
    }

    @Test
    fun `die stufen laufen im kreis`() {
        var stufe = HapticStrength.OFF
        stufe = Haptics.next(stufe); assertEquals(HapticStrength.LIGHT, stufe)
        stufe = Haptics.next(stufe); assertEquals(HapticStrength.STRONG, stufe)
        stufe = Haptics.next(stufe); assertEquals(HapticStrength.OFF, stufe)
    }

    // Ohne Angabe gilt: leicht. Eine frische Installation soll sich melden.
    @Test
    fun `die vorgabe ist leicht`() {
        assertEquals(HapticStrength.LIGHT, Behaviour().haptics)
    }
}
