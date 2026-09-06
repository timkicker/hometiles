package dev.kicker.hometiles.tiles

import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.data.ButtonAction
import dev.kicker.hometiles.data.Cell
import dev.kicker.hometiles.data.LauncherConfig
import dev.kicker.hometiles.data.Screen
import dev.kicker.hometiles.data.ScreenKind
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.1: the swipe chain and in which order it runs.
 *
 * `swipeOrder` stood in the model from the start and was read by [ScreenOrder] - but written
 * by no screen ever. it stayed empty, the order was always the order of creation. a promise
 * that did nothing.
 */
class SwipeChainTest {

    private fun screen(id: String, kind: ScreenKind = ScreenKind.SCREEN) =
        Screen(id = id, name = id.uppercase(), cols = 2, rows = 3, kind = kind)

    private val config = LauncherConfig(
        screens = listOf(
            screen("a"),
            screen("b"),
            screen("mehr", ScreenKind.FOLDER),
            screen("c"),
        ),
        homeScreenId = "a",
    )

    @Test
    fun `without an order of its own the creation order holds`() {
        assertEquals(listOf("a", "b", "c"), SwipeChain.explicit(config))
    }

    // folders belong to their tile, not to the chain: whoever swipes on expects the next
    // screen, not the contents of a folder.
    @Test
    fun `folders never enter the chain`() {
        val moved = SwipeChain.moveDown(config, "a")
        assertEquals(listOf("b", "a", "c"), moved.swipeOrder)
    }

    @Test
    fun `up and down are reversible`() {
        val down = SwipeChain.moveDown(config, "a")
        val upAgain = SwipeChain.moveUp(down, "a")
        assertEquals(listOf("a", "b", "c"), SwipeChain.explicit(upAgain))
    }

    /**
     * nothing happens at the edge, and without wrapping: whoever moves the topmost screen up
     * does not expect it to come out at the bottom. the ring holds for swiping, not sorting.
     */
    @Test
    fun `nothing moves at the edge`() {
        assertEquals(config, SwipeChain.moveUp(config, "a"))
        assertEquals(config, SwipeChain.moveDown(config, "c"))
    }

    @Test
    fun `an unknown screen changes nothing`() {
        assertEquals(config, SwipeChain.moveUp(config, "gibtsnicht"))
    }

    // the order has to take effect, not only be stored.
    @Test
    fun `the new order holds for next and previous`() {
        val moved = SwipeChain.moveDown(config, "a")
        assertEquals(listOf("b", "a", "c"), ScreenOrder.ordered(moved).map { it.id })
        assertEquals("a", ScreenOrder.next(moved, "b"))
        assertEquals("b", ScreenOrder.previous(moved, "a"))
    }

    @Test
    fun `the position is reported correctly`() {
        assertEquals(0, SwipeChain.position(config, "a"))
        assertEquals(2, SwipeChain.position(config, "c"))
        assertEquals(-1, SwipeChain.position(config, "mehr"))
    }

    // deleting keeps the order tidy, or it would point at a screen that is gone and the next
    // swipe would fall into nothing.
    @Test
    fun `a deleted screen drops out of the order`() {
        val ordered = SwipeChain.moveDown(config, "a")
        val withoutB = ScreenEdits.delete(ordered, "b")
        assertEquals(false, withoutB.swipeOrder.contains("b"))
        assertEquals(listOf("a", "c"), ScreenOrder.ordered(withoutB).map { it.id })
    }
}

/**
 * PLAN.md 4.1: which screens are reachable by swiping.
 *
 * a screen may leave the chain only if it can still be reached another way. otherwise taking
 * it out would be the quickest way to make a screen unfindable - it would stand in the
 * configuration and no way would lead there.
 */
class SwipeMembershipTest {

    private fun screen(id: String, cells: List<Cell> = emptyList()) =
        Screen(id = id, name = id.uppercase(), cols = 2, rows = 3, cells = cells)

    private val jump = Cell(0, 0, button = Button(action = ButtonAction.GoToScreen("b")))

    private val config = LauncherConfig(
        screens = listOf(screen("a", listOf(jump)), screen("b"), screen("c")),
        homeScreenId = "a",
    )

    @Test
    fun `with a jump tile it may leave`() {
        assertEquals(true, SwipeChain.mayLeave(config, "b"))
        val without = SwipeChain.exclude(config, "b")
        assertEquals(setOf("b"), without.swipeExcluded)
        assertEquals(listOf("a", "c"), ScreenOrder.ordered(without).map { it.id })
    }

    @Test
    fun `without a way back it stays in`() {
        assertEquals(false, SwipeChain.mayLeave(config, "c"))
        assertEquals(config, SwipeChain.exclude(config, "c"))
    }

    // the home screen is always reachable - the back gesture leads to it.
    @Test
    fun `the home screen may always leave`() {
        assertEquals(true, SwipeChain.mayLeave(config, "a"))
    }

    @Test
    fun `bringing it back always works`() {
        val without = SwipeChain.exclude(config, "b")
        assertEquals(config.swipeExcluded, SwipeChain.include(without, "b").swipeExcluded)
        assertEquals(listOf("a", "b", "c"), ScreenOrder.ordered(SwipeChain.include(without, "b")).map { it.id })
    }

    // an exception list instead of a membership list: a screen created later is in by itself
    // rather than quietly missing.
    @Test
    fun `a new screen is in the chain by itself`() {
        val without = SwipeChain.exclude(config, "b")
        val withNew = without.copy(screens = without.screens + screen("d"))
        assertEquals(listOf("a", "c", "d"), ScreenOrder.ordered(withNew).map { it.id })
    }

    /**
     * and the warning that no way leads here has to count swiping, or it warns about screens
     * one reaches with a hand movement - and a warning that is wrong is no longer taken
     * seriously where it is right either.
     */
    @Test
    fun `with swiping switched on the chain counts as a way`() {
        val withoutSwiping = config
        assertEquals(listOf("c"), ScreenEdits.unreachable(withoutSwiping).map { it.id })

        val withSwiping = config.copy(
            behaviour = config.behaviour.copy(swipeBetweenScreens = true),
        )
        assertEquals(emptyList<String>(), ScreenEdits.unreachable(withSwiping).map { it.id })
    }

    @Test
    fun `taken out of the chain the swiping no longer counts`() {
        val withSwiping = config.copy(
            behaviour = config.behaviour.copy(swipeBetweenScreens = true),
        )
        // "b" has a jump tile, so it may leave - and stays reachable.
        val withoutB = SwipeChain.exclude(withSwiping, "b")
        assertEquals(emptyList<String>(), ScreenEdits.unreachable(withoutB).map { it.id })
    }
}
