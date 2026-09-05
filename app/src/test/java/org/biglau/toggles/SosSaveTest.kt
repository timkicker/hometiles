package org.biglau.toggles

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * saving says that it has saved - and what.
 *
 * the sos page holds the app's only two buttons whose whole effect is invisible: the numbers
 * and the message text. everything else in the settings changes before one's eyes, or the
 * screen closes. on 04.09.2026 "save numbers" was tapped and the screen looked as before.
 *
 * two things were missing:
 *
 * * without a change it is no button. it looked the same and could always be pressed
 *   although there was nothing to save.
 * * the feedback says what really happened. an "x" in the field would have made "numbers
 *   saved" sound true and be false - the red line above says precisely that none of it is
 *   usable. the message now counts: none, one, several.
 */
class SosSaveTest {

    private val source = Quelltext.withoutComments("org/biglau/toggles/SosSettings.kt")

    @Test
    fun `without a change both buttons stay quiet`() {
        listOf(
            "changed" to "the numbers",
            "messageChanged" to "the message",
        ).forEach { (flag, what) ->
            assertTrue(
                "the button for $what carries the accent colour even without a change",
                "if ($flag) palette.surfaceAccent else palette.surfaceDefault" in source,
            )
            assertTrue(
                "the button for $what can be pressed even without a change",
                "onClick = if ($flag) {" in source,
            )
        }
    }

    @Test
    fun `the feedback tells the empty case apart`() {
        val saving = Quelltext.cut(
            source,
            from = "val taken = SosNumbers.parse(numbersText)",
            to = "\n                    }",
        )
        assertTrue(
            "the message after saving does not count what was taken:\n" + saving,
            "taken.isEmpty()" in saving && "sos_numbers_cleared" in saving,
        )
        assertTrue(
            "the plural form is missing for taken numbers",
            "sos_numbers_saved_n" in saving,
        )
    }

    @Test
    fun `both messages exist in both languages`() {
        listOf("values-de", "values").forEach { language ->
            val text = Quelltext.textValue("sos_numbers_cleared", language)
            assertTrue("sos_numbers_cleared is missing in $language", text.isNotBlank())
        }
    }
}
