package org.biglau.tiles

import org.biglau.data.LauncherConfig
import org.biglau.data.Screen
import org.biglau.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * next screen and previous screen.
 *
 * both sat in the else branch of the tile handling and reported "coming soon" while the plan
 * promises them in 4.3. an else branch hides such a thing, a complete when does not.
 */
class ScreenOrderTest {

    private fun screen(id: String) = Screen(id = id, name = id)

    private val three = LauncherConfig(
        screens = listOf(screen("a"), screen("b"), screen("c")),
        homeScreenId = "a",
    )

    @Test
    fun `forward goes in order`() {
        assertEquals("b", ScreenOrder.next(three, "a"))
        assertEquals("c", ScreenOrder.next(three, "b"))
    }

    @Test
    fun `the row is a ring`() {
        // otherwise the tile on the last screen would do nothing - a dead button.
        assertEquals("a", ScreenOrder.next(three, "c"))
        assertEquals("c", ScreenOrder.previous(three, "a"))
    }

    @Test
    fun `back goes backwards`() {
        assertEquals("a", ScreenOrder.previous(three, "b"))
    }

    @Test
    fun `with a single screen there is nothing to page`() {
        val one = LauncherConfig(screens = listOf(screen("a")), homeScreenId = "a")
        assertNull(ScreenOrder.next(one, "a"))
        assertNull(ScreenOrder.previous(one, "a"))
    }

    @Test
    fun `folders do not lie in the row`() {
        // whoever taps next expects the next screen, not the contents of a folder.
        val withFolder = LauncherConfig(
            screens = listOf(screen("a"), Screen(id = "f", name = "F", kind = ScreenKind.FOLDER), screen("b")),
            homeScreenId = "a",
        )
        assertEquals(listOf("a", "b"), ScreenOrder.ordered(withFolder).map { it.id })
        assertEquals("b", ScreenOrder.next(withFolder, "a"))
    }

    @Test
    fun `an own order is respected`() {
        val own = three.copy(swipeOrder = listOf("c", "a", "b"))
        assertEquals(listOf("c", "a", "b"), ScreenOrder.ordered(own).map { it.id })
        assertEquals("a", ScreenOrder.next(own, "c"))
    }

    @Test
    fun `a screen outside the own order does not fall away`() {
        // otherwise it would never be reachable through next - the same trap as a screen
        // without a jump tile.
        val own = three.copy(swipeOrder = listOf("c"))
        assertEquals(listOf("c", "a", "b"), ScreenOrder.ordered(own).map { it.id })
    }

    @Test
    fun `an unknown screen leads nowhere`() {
        assertNull(ScreenOrder.next(three, "doesnotexist"))
    }
}
