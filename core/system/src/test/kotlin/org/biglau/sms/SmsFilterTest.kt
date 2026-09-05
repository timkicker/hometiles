package org.biglau.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the message filter (PLAN.md 4.7).
 *
 * it **hides**, it does not block - BigLau does not hold the sms role. that stands in the
 * KDoc, in the settings and here, so nobody takes the boundary for tighter than it is.
 */
class SmsFilterTest {

    private var id = 0L
    private fun msg(
        body: String = "Hello",
        address: String = "+436641234567",
        incoming: Boolean = true,
    ) = SmsMessage(id++, 1L, address, body, 1000L, incoming, true)

    @Test
    fun `without a filter everything stays`() {
        val list = listOf(msg(), msg("second"))
        assertEquals(list, SmsFilter.apply(list, emptyList(), emptyList()))
    }

    @Test
    fun `a filtered number vanishes from the list`() {
        val list = listOf(msg(address = "+436641234567"), msg(address = "+436809999999"))
        val left = SmsFilter.apply(list, listOf("0664 1234567"), emptyList())
        assertEquals(1, left.size)
        assertEquals("+436809999999", left.first().address)
    }

    @Test
    fun `a word works mid-text and regardless of case`() {
        // the german words stay: the umlaut pair is the check object for the case folding.
        assertTrue(SmsFilter.hidden(msg("Sie haben GEWONNEN!!!"), emptyList(), listOf("gewonnen")))
        assertTrue(SmsFilter.hidden(msg("herzlichen glückwunsch"), emptyList(), listOf("Glückwunsch")))
        assertFalse(SmsFilter.hidden(msg("see you soon"), emptyList(), listOf("gewonnen")))
    }

    @Test
    fun `one's own messages are never hidden`() {
        // what one wrote oneself is not hidden from oneself - not even when the filtered word
        // stands in it.
        assertFalse(
            SmsFilter.hidden(msg("ich habe gewonnen", incoming = false), emptyList(), listOf("gewonnen")),
        )
    }

    @Test
    fun `a number too short does not filter everything away`() {
        // the same fault as with call blocking: "123" must not hit every message that happens
        // to end that way.
        val list = listOf(msg(address = "+436641234567"))
        assertEquals(list, SmsFilter.apply(list, listOf("123"), emptyList()))
    }

    @Test
    fun `the word list is split and de-duplicated`() {
        assertEquals(listOf("Gewinn", "Werbung"), SmsFilter.parseWords("Gewinn, gewinn; Werbung"))
        assertEquals("Gewinn, Werbung", SmsFilter.formatWords(listOf("Gewinn", "Werbung")))
    }

    @Test
    fun `an empty word filters nothing`() {
        // otherwise the whole list would disappear, because every text contains the empty
        // string.
        assertFalse(SmsFilter.hidden(msg("Hello"), emptyList(), listOf("", "   ")))
    }
}
