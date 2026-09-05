package org.biglau.ui

import org.biglau.data.Behaviour
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.4: a confirmation dialog instead of a short flash.
 *
 * a flash is gone after two seconds. whoever reads slowly knows afterwards only that
 * something flashed up - not what.
 */
class NoticeStyleTest {

    @Test
    fun `without the setting it stays with the flash`() {
        assertEquals(NoticeStyle.TOAST, Notice.styleFor(false))
    }

    @Test
    fun `with the setting the notice waits`() {
        assertEquals(NoticeStyle.DIALOG, Notice.styleFor(true))
    }

    // the default is the flash: a notice that demands a button every time is an imposition
    // for most. the choice belongs to whoever needs it.
    @Test
    fun `the default is the flash`() {
        assertEquals(false, Behaviour().confirmMessages)
        assertEquals(NoticeStyle.TOAST, Notice.styleFor(Behaviour().confirmMessages))
    }

    /**
     * `BigHeading` brings two things a notice needs: the heading size and the scale that
     * **shrinks before it hyphenates** - a notice may be long, and split mid-word it reads
     * like a fault.
     */
    @Test
    fun `the notice stands in heading size`() {
        val source = org.biglau.Quelltext.file("org/biglau/ui/NoticeActivity.kt").readText()
        assertEquals(true, "BigHeading(text)" in source)
    }
}
