package org.biglau.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationTextTest {

    @Test
    fun `the choice runs from three quarters to double`() {
        assertEquals(0.75f, ConversationText.CHOICES.first())
        assertEquals(2.0f, ConversationText.CHOICES.last())
        assertTrue("a hundred percent is among them", 1.0f in ConversationText.CHOICES)
    }

    @Test
    fun `a nonsensical value is bounded`() {
        // a backup from a later version can bring a number that does not exist here - a
        // factor of twelve would leave one letter of the message.
        assertEquals(2.0f, ConversationText.scale(12f))
        assertEquals(0.75f, ConversationText.scale(0f))
        assertEquals(0.75f, ConversationText.scale(-3f))
    }

    @Test
    fun `valid values stay unchanged`() {
        ConversationText.CHOICES.forEach { assertEquals(it, ConversationText.scale(it)) }
    }
}
