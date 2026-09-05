package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the page button has a lower bound, not a fixed size.
 *
 * a fixed size **before** the caller's modifier limits the caller instead of being
 * overridable by it: the wizard asked for 72 dp and got 56 - measured on 04.09.2026, 77
 * instead of 99 pixels. and no width stood there at all, so the button took the icon's, of
 * which **40,7 dp** were left in the wizard's narrow row, starting at x=1 - under the 48 dp
 * minimum for a finger tap.
 *
 * giving the button 72 dp of width in the wizard was the first attempt: at 200 percent
 * system font "Weiter" beside it broke mid-word. the minimum is enough, more takes the
 * neighbouring row's space.
 */
class PageButtonTest {

    private val button = Quelltext.cut(
        Quelltext.withoutComments("org/biglau/ui/ScrollButtons.kt"),
        from = "private fun PageButton(",
        to = "\n}",
    )

    @Test
    fun `the caller comes before the lower bounds`() {
        val chain = Quelltext.cut(button, from = "Box(", to = "contentAlignment")
        val caller = chain.indexOf(".then(modifier)")
        assertTrue("PageButton does not pass the caller's modifier through", caller >= 0)
        val fixed = Regex("""\.(height|width|size)\(""").find(chain)
        assertTrue(
            "a fixed size stands before `.then(modifier)` - then the caller cannot " +
                "override it but is limited by it: " + (fixed?.value ?: ""),
            fixed == null || fixed.range.first > caller,
        )
    }

    @Test
    fun `the button is never narrower than a finger`() {
        assertTrue(
            "PageButton has no minimum width. without it it takes its icon's width, and a " +
                "narrow row presses it below - in the wizard it was 40,7 dp.",
            "widthIn(min = 48.dp)" in button,
        )
        assertTrue(
            "PageButton no longer has a minimum height - then a caller that says nothing " +
                "stands before a button the size of an icon.",
            "heightIn(min = 56.dp)" in button,
        )
    }
}
