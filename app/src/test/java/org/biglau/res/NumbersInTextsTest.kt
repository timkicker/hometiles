package org.biglau.res

import org.biglau.Quelltext
import org.biglau.phone.SpeedDial
import org.biglau.security.Pin
import org.biglau.ui.EMERGENCY_HOLD_MILLIS
import org.biglau.ui.theme.FreeTileColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a number in a text is a promise.
 *
 * four texts name a number that also stands somewhere in the source. none of them hung on its
 * origin - whoever sets `HUE_COUNT` to 18 changes the colours and leaves the sentence
 * standing. written down on 04.09.2026 while the colour choice was gone through.
 *
 * same exercise as [org.biglau.ui.IconCountTest] and [org.biglau.EdgeCountTest]. both
 * languages are checked: a number that only travels in german is the same trap at half reach.
 */
class NumbersInTextsTest {

    private fun text(name: String, language: String): String =
        Quelltext.texts(language)
            .firstNotNullOfOrNull { file ->
                Regex("""<string name="$name">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
                    .find(file.readText())?.groupValues?.get(1)
            }
            ?: throw AssertionError("$language: $name missing")

    private fun bothLanguages(name: String, number: String, forWhat: String) {
        listOf("values", "values-de").forEach { language ->
            val value = text(name, language)
            assertTrue(
                "$language/$name no longer names $number ($forWhat): $value",
                number in value,
            )
        }
    }

    @Test
    fun `the number of free hues is right`() {
        bothLanguages(
            "editor_color_free_hint",
            FreeTileColor.HUE_COUNT.toString(),
            "FreeTileColor.HUE_COUNT",
        )
    }

    @Test
    fun `the hold time of the emergency exit is right`() {
        val seconds = (EMERGENCY_HOLD_MILLIS / 1000).toString()
        listOf("security_explainer", "security_forgot", "editor_locked_hint").forEach {
            bothLanguages(it, seconds, "EMERGENCY_HOLD_MILLIS")
        }
    }

    @Test
    fun `the length of the pin is right`() {
        listOf("security_new_pin", "security_pin_rules").forEach { name ->
            bothLanguages(name, Pin.MIN_LENGTH.toString(), "Pin.MIN_LENGTH")
            bothLanguages(name, Pin.MAX_LENGTH.toString(), "Pin.MAX_LENGTH")
        }
    }

    @Test
    fun `the assignable speed dial keys are right`() {
        val first = SpeedDial.ASSIGNABLE.first().toString()
        val last = SpeedDial.ASSIGNABLE.last().toString()
        bothLanguages("dialer_speeddial_hint_assign", first, "SpeedDial.ASSIGNABLE.first")
        bothLanguages("dialer_speeddial_hint_assign", last, "SpeedDial.ASSIGNABLE.last")
    }

    /** and the rule finds anything at all - otherwise it checks air. */
    @Test
    fun `there really are four different numbers`() {
        assertEquals(
            "one of the four sources is gone - then the rule needs pulling along.",
            listOf(24, 30, 4, 8),
            listOf(
                FreeTileColor.HUE_COUNT,
                (EMERGENCY_HOLD_MILLIS / 1000).toInt(),
                Pin.MIN_LENGTH,
                Pin.MAX_LENGTH,
            ),
        )
    }
}
