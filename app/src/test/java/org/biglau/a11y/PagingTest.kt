package org.biglau.a11y

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PagingTest {

    @Test
    fun `one page moves by a page minus one row`() {
        assertEquals(4, Paging.step(5))
        assertEquals(0 + 4, Paging.down(firstVisible = 0, visibleCount = 5, total = 100))
    }

    @Test
    fun `the overlap keeps rows from being skipped`() {
        // rows 0..4 are visible. after the press the list starts at 4 - row 4 is seen twice.
        // without the overlap row 5 would be the new start and row 4 never readable at rest.
        val next = Paging.down(firstVisible = 0, visibleCount = 5, total = 100)
        assertTrue("no gap", next <= 0 + 5)
    }

    @Test
    fun `at the end the last screen stays full`() {
        // 10 entries, 4 fit: the last sensible start is 6, not 9.
        assertEquals(6, Paging.lastStart(visibleCount = 4, total = 10))
        assertEquals(6, Paging.down(firstVisible = 5, visibleCount = 4, total = 10))
        assertEquals(6, Paging.down(firstVisible = 6, visibleCount = 4, total = 10))
    }

    @Test
    fun `at the start paging back does not go negative`() {
        assertEquals(0, Paging.up(firstVisible = 2, visibleCount = 8))
        assertEquals(0, Paging.up(firstVisible = 0, visibleCount = 8))
    }

    @Test
    fun `a short list knows no direction`() {
        assertFalse(Paging.canGoUp(0))
        assertFalse(Paging.canGoDown(firstVisible = 0, visibleCount = 8, total = 3))
    }

    @Test
    fun `a long list knows both directions in the middle`() {
        assertTrue(Paging.canGoUp(20))
        assertTrue(Paging.canGoDown(firstVisible = 20, visibleCount = 6, total = 338))
    }

    @Test
    fun `an empty list cannot be paged`() {
        assertEquals(0, Paging.lastStart(visibleCount = 6, total = 0))
        assertFalse(Paging.canGoDown(firstVisible = 0, visibleCount = 6, total = 0))
        assertEquals(0, Paging.down(firstVisible = 0, visibleCount = 6, total = 0))
    }

    @Test
    fun `a single visible row still pages on`() {
        // a very tall row fills the screen on its own. step would otherwise be 0 and the
        // button would do nothing - the classic dead button.
        assertEquals(1, Paging.step(1))
        assertEquals(1, Paging.down(firstVisible = 0, visibleCount = 1, total = 9))
    }

    @Test
    fun `unknown visibility does not block the button`() {
        // before the first measurement the list reports 0 visible rows.
        assertEquals(1, Paging.step(0))
        assertEquals(1, Paging.down(firstVisible = 0, visibleCount = 0, total = 50))
    }
}

class PagingVisibilityTest {

    // viewport 0..100, rows 40 tall: the third hangs out at the bottom.
    private val rows = listOf(
        Paging.Row(index = 3, offset = -10, size = 40),
        Paging.Row(index = 4, offset = 30, size = 40),
        Paging.Row(index = 5, offset = 70, size = 40),
    )

    @Test
    fun `cut rows do not count`() {
        val whole = Paging.fullyVisible(rows, viewportStart = 0, viewportEnd = 100)
        assertEquals(listOf(4), whole.map { it.index })
    }

    @Test
    fun `the jump starts at the first fully visible row`() {
        assertEquals(4, Paging.firstFullyVisibleIndex(rows, 0, 100, fallback = 3))
    }

    @Test
    fun `without a single whole row the known start stays`() {
        // one very tall row fills the window and juts out top and bottom.
        val huge = listOf(Paging.Row(index = 7, offset = -20, size = 300))
        assertEquals(7, Paging.firstFullyVisibleIndex(huge, 0, 100, fallback = 7))
        assertTrue(Paging.fullyVisible(huge, 0, 100).isEmpty())
    }

    @Test
    fun `paging jumps exactly to the last fully visible row`() {
        // fully visible: 4,5,6,7. afterwards 7 stands on top - one row of overlap.
        val whole = (4..7).map { Paging.Row(it, (it - 4) * 25, 25) }
        val first = Paging.firstFullyVisibleIndex(whole, 0, 100, fallback = 4)
        val count = Paging.fullyVisible(whole, 0, 100).size
        assertEquals(7, Paging.down(first, count, total = 338))
    }
}
