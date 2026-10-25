package dev.kicker.hometiles.ui

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the blink border of a tile with something new stands out from the page too.
 *
 * it was the tile's white ink, 2 dp: in the light theme 1.04:1 against the page, so the tile
 * looked a little smaller rather than marked (emulator, 25.10.2026, found beside issue #2).
 * it now takes the same rings as the focus, each as thin as before. which colours those are
 * is held for every theme by FocusInkTest.
 */
class BlinkBorderTest {

    private val tile = Quelltext.withoutComments("dev/kicker/hometiles/ui/BigTile.kt")

    @Test
    fun `the blink border uses the rings that the page cannot swallow`() {
        val badge = Quelltext.cut(tile, "badgeCount > 0 -> Modifier.focusRings(", "border != null ->")
        assertTrue("the blink border does not use the tile rings: $badge", "inkRings" in badge)
        assertTrue("the blink border is as thick as the focus", "BADGE_BORDER_DP" in badge)
    }
}
