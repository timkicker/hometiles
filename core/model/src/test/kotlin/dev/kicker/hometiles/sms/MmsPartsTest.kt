package dev.kicker.hometiles.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * putting a picture message back together.
 *
 * the case that shapes this: a picture with no words at all. the parts hold an image and a
 * layout description, no text - and a row that shows the layout would put markup in the list,
 * while an empty row looks like a fault.
 */
class MmsPartsTest {

    private fun text(body: String) = MmsParts.Part("text/plain", body)
    private val picture = MmsParts.Part("image/jpeg")
    private val layout = MmsParts.Part("application/smil", "<smil><head></head></smil>")

    @Test
    fun `the text parts make the body`() {
        assertEquals("Hello there", MmsParts.body(listOf(text("Hello"), text("there"))))
    }

    @Test
    fun `the layout part does not become the body`() {
        assertFalse(MmsParts.isText(layout))
        assertEquals("", MmsParts.body(listOf(layout, picture)))
    }

    @Test
    fun `empty parts fall away instead of leaving gaps`() {
        assertEquals("Hello", MmsParts.body(listOf(text("Hello"), text("   "), text(""))))
    }

    @Test
    fun `a picture is recognised by its type`() {
        assertTrue(MmsParts.hasImage(listOf(layout, picture)))
        assertTrue(MmsParts.hasImage(listOf(MmsParts.Part("IMAGE/PNG"))))
        assertFalse(MmsParts.hasImage(listOf(layout, text("Hello"))))
    }

    @Test
    fun `a picture without words says picture`() {
        assertEquals("Picture", MmsParts.preview(listOf(layout, picture), "Picture"))
    }

    @Test
    fun `with text the text stands there`() {
        assertEquals(
            "Look at this",
            MmsParts.preview(listOf(layout, picture, text("Look at this")), "Picture"),
        )
    }

    @Test
    fun `without anything to show the row stays empty`() {
        assertEquals("", MmsParts.preview(listOf(layout), "Picture"))
        assertEquals("", MmsParts.preview(emptyList(), "Picture"))
    }
}
