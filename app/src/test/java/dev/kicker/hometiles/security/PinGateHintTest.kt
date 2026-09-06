package dev.kicker.hometiles.security

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the lock carries the way out, not the reasoning.
 *
 * the long explaining text took three lines on the pin entry and was cut off **exactly where
 * the way out stood**. whoever has forgotten the pin therefore misses precisely the sentence
 * they need.
 *
 * on the settings page the long text stays; there is room there, and that is where one reads
 * it before setting a pin.
 */
class PinGateHintTest {

    private val settings =
        Quelltext.file("dev/kicker/hometiles/settings/SettingsActivity.kt").readText()

    @Test
    fun `the lock shows the short sentence`() {
        val place = Quelltext.cut(settings, "Page.GATE -> PinGate(", "wrongText")
        assertTrue("the short sentence is missing: $place", "security_forgot" in place)
        assertTrue("the long text stands on the lock again: $place", "security_explainer" !in place)
    }

    @Test
    fun `the short sentence names the thirty seconds`() {
        listOf("values", "values-de").forEach { language ->
            val text = Quelltext.textValue("security_forgot", language)
            assertTrue("$language: without the duration the sentence is no use", "30" in text)
            assertTrue("$language: too long for the lock (${text.length})", text.length <= 60)
        }
    }
}
