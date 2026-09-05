package org.biglau.a11y

import org.biglau.data.Accessibility
import org.biglau.data.PressMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the menu-key list shows all three, whatever is set.
 *
 * `PLAN.md` 10.3.4. that is the whole point of the list, and the only place where it has to
 * differ from [LongPress.decide]: whoever turns on reading-aloud gets only that from a long
 * press. with a finger that is right and changeable. with keys it is a dead end, because the
 * setting that undoes it sits behind the editor, and the editor is what one can no longer
 * reach.
 */
class TileMenuTest {

    @Test
    fun `the list shows all three`() {
        assertEquals(
            listOf(MenuItem.EDIT, MenuItem.SPEAK, MenuItem.SHOW_LARGE),
            TileMenu.items(),
        )
    }

    /** the core: whatever a long press drops depending on the settings, the list never does. */
    @Test
    fun `no setting takes an entry from the list`() {
        listOf(
            Accessibility(),
            Accessibility(speakOnLongPress = true),
            Accessibility(popupOnLongPress = true),
            Accessibility(speakOnLongPress = true, popupOnLongPress = true),
        ).forEach { setting ->
            listOf(PressMode.SHORT, PressMode.LONG).forEach { mode ->
                val onLongPress = LongPress.decide(setting, editMode = false, pressMode = mode)
                assertTrue(
                    "a long press drops something here, which is fine: $onLongPress",
                    onLongPress.size < 3,
                )
                assertEquals(
                    "the list must still show all three, or it is as narrow for keys as the " +
                        "long press is",
                    3,
                    TileMenu.items().size,
                )
            }
        }
    }

    @Test
    fun `the second action comes first`() {
        val with = TileMenu.items(hasSecondAction = true)
        assertEquals("it belongs to this one tile", MenuItem.SECOND_ACTION, with.first())
        assertEquals(4, with.size)
    }

    @Test
    fun `without a second action it is absent`() {
        assertTrue(MenuItem.SECOND_ACTION !in TileMenu.items(hasSecondAction = false))
    }
}
