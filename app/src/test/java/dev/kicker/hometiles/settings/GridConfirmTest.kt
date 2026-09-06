package dev.kicker.hometiles.settings

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a smaller grid costs tiles - both ways there ask first.
 *
 * the grid can be changed in **two** ways: through the presets at the top and through the
 * free lists below them. both lead into the same thing, and both have to ask the same
 * question: name the number of tiles that will disappear, and change only on the second tap.
 * two ways that drift apart is the case that has come up twice already.
 *
 * counted on 04.09.2026 with eight tiles on a 2x4 grid: the list named six, four and two
 * lost tiles for the smaller presets and called 2x4 the current one. the numbers agree with
 * `config.json`.
 */
class GridConfirmTest {

    private val settings = Quelltext.withoutComments("dev/kicker/hometiles/settings/SettingsActivity.kt")

    @Test
    fun `both ways count the tiles that disappear`() {
        val places = settings.split("ScreenEdits.dropped(").size - 1
        assertTrue(
            "not every way to the grid counts what gets lost - then one of the two costs " +
                "tiles without saying so.",
            places >= 2,
        )
    }

    @Test
    fun `both ways change only on the second tap`() {
        // the preset list and `GridChoiceRow` - two places, one behaviour.
        val armed = Regex("""armed ->""").findAll(settings).count()
        assertTrue(
            "only one of the two grid lists asks before a loss: $armed places with an " +
                "armed row.",
            armed >= 2,
        )
        assertEquals(
            "a grid change without a loss should not ask - a confirmation that comes even " +
                "when nothing happens soon goes unread.",
            2,
            settings.split("loses == 0 ->").size - 1,
        )
    }
}
