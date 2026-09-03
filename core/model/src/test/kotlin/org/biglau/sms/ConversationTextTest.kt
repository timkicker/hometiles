package org.biglau.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationTextTest {

    @Test
    fun `die Auswahl reicht von drei Vierteln bis doppelt`() {
        assertEquals(0.75f, ConversationText.CHOICES.first())
        assertEquals(2.0f, ConversationText.CHOICES.last())
        assertTrue("hundert Prozent ist dabei", 1.0f in ConversationText.CHOICES)
    }

    @Test
    fun `ein unsinniger Wert wird begrenzt`() {
        // Eine Sicherung aus einer spaeteren Fassung kann eine Zahl mitbringen, die es hier
        // nicht gibt - Faktor zwoelf liesse von der Nachricht einen Buchstaben uebrig.
        assertEquals(2.0f, ConversationText.scale(12f))
        assertEquals(0.75f, ConversationText.scale(0f))
        assertEquals(0.75f, ConversationText.scale(-3f))
    }

    @Test
    fun `gueltige Werte bleiben unveraendert`() {
        ConversationText.CHOICES.forEach { assertEquals(it, ConversationText.scale(it)) }
    }
}
