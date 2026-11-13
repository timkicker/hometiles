package dev.kicker.hometiles.phone

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a set speed dial can be found again from the keypad (issue #10).
 *
 * on the keypad a long press on a set key dials, so nothing there leads to editing it. the
 * way out is a settings page (issue #2), but its row only said "call list" and nobody looked
 * for speed dial behind it. now the hint line above the keypad opens that page and the row
 * names speed dial.
 */
class SpeedDialWayOutTest {

    private val dialer = Quelltext.withoutComments("dev/kicker/hometiles/phone/DialerActivity.kt")
    private val languages = listOf("values", "values-de", "values-fr", "values-es", "values-it")

    private fun text(directory: String, name: String): String {
        val pattern = Regex("""<string name="$name">([^<]*)</string>""")
        return Quelltext.texts(directory).firstNotNullOfOrNull { pattern.find(it.readText())?.groupValues?.get(1) }
            ?: throw AssertionError("$directory has no $name")
    }

    @Test
    fun `the hint line opens the speed dial settings`() {
        assertTrue(
            "the hint line over the keypad is no way to the speed dial settings.",
            Regex("""\.clickable\(onClickLabel = changeLabel, onClick = onSpeedDialSettings\)""")
                .containsMatchIn(dialer),
        )
        val wiring = Quelltext.cut(dialer, "onSpeedDialSettings = {", "onDigit")
        assertTrue(
            "the hint line leads somewhere other than the page with the speed dial.",
            wiring.contains("SettingsLink.PAGE_CALL_TYPES"),
        )
        assertTrue(
            "the hint line opens the call page at the top; the speed dial is at its end.",
            wiring.contains("SettingsLink.SECTION_SPEED_DIAL"),
        )
    }

    @Test
    fun `the call page scrolls to the speed dial when asked`() {
        val settings = Quelltext.withoutComments("dev/kicker/hometiles/settings/SettingsActivity.kt")
        val page = Quelltext.cut(settings, "private fun CallTypesList(", "private fun ")
        assertTrue("the speed dial heading has no key to scroll to.", "item(key = SPEED_DIAL_KEY)" in page)
        assertTrue("the page never looks for the speed dial heading.", "it.key == SPEED_DIAL_KEY" in page)
        assertTrue(
            "the settings never read the section the keypad asks for.",
            "SettingsLink.EXTRA_SECTION" in settings && "toSpeedDial = speedDialRequest.value" in settings,
        )
    }

    @Test
    fun `the settings row says speed dial`() {
        val hidden = languages.filter { dir ->
            text(dir, "speeddial_heading").lowercase() !in text(dir, "settings_call_types").lowercase()
        }
        assertEquals("the row leading to the speed dial does not name it", emptyList<String>(), hidden)
    }
}
