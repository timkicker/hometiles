package org.biglau.tiles

import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "clear tile" appears only when there is something to clear.
 *
 * in a **fresh** tile's editor "clear tile" stood in the danger colour, right at the bottom
 * above "done" - the screen's most conspicuous button, and it did nothing. the same screen
 * already hides move, blink and the second action for empty tiles, with the comment that
 * offering a row which then says "not possible" is worse than leaving it out.
 */
class ClearableTest {

    @Test
    fun `a fresh tile has nothing to clear`() {
        assertFalse(TileEdits.clearable(Button()))
    }

    @Test
    fun `a filled tile does`() {
        assertTrue(TileEdits.clearable(Button(action = ButtonAction.GoToScreen("home"))))
    }

    /**
     * the case for comparing against the *whole* tile and not only the action: an empty tile
     * may carry a label of its own - the home screen then shows it instead of the invitation
     * to fill the tile. that is something to clear.
     */
    @Test
    fun `an empty tile with its own label has something to clear`() {
        assertTrue(TileEdits.clearable(Button(label = "Spaeter")))
    }

    @Test
    fun `a preset colour or an icon counts too`() {
        assertTrue(TileEdits.clearable(Button(colorHue = 210f)))
        assertTrue(TileEdits.clearable(Button(iconName = "star")))
        assertTrue(TileEdits.clearable(Button(colorIndex = 2)))
    }

    /** and the second action - it otherwise survives invisibly on an empty tile. */
    @Test
    fun `a second action counts too`() {
        assertTrue(TileEdits.clearable(Button(longPress = ButtonAction.GoToScreen("home"))))
    }
}
