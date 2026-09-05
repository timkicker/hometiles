package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * after the first message the new conversation is a real one.
 *
 * on 03.09.2026 the first message to a number went out, the field emptied, a short notice
 * came - and the screen stayed **empty**. the message was written and stood in the list, but
 * this screen still hung on the number instead of the conversation that existed by then.
 *
 * whoever has just sent something and then stands before an empty screen sends it again. an
 * sms costs money and confuses the recipient.
 *
 * the second half of the same place: the "new message to ..." row stayed after the
 * conversation existed - two entries for the same person, one with a name and one with a
 * number.
 */
class NewThreadTest {

    private val source = Quelltext.withoutComments("org/biglau/sms/SmsActivity.kt")

    @Test
    fun `after sending the screen moves into the conversation`() {
        // what is asked is not whether the name stands somewhere but whether it is set
        // **while sending**. the first version of this rule stayed green when exactly that
        // assignment was removed.
        val whileSending = source.indexOf("send(newNumber")
        assertTrue("sending into a new conversation no longer exists", whileSending > 0)
        val block = source.substring(whileSending, minOf(source.length, whileSending + 260))
        assertTrue(
            "sending into a new conversation does not remember the number - then the " +
                "screen stays empty although the message is out: " + block,
            "justSentTo" in block,
        )
        val from = source.indexOf("LaunchedEffect(threads, justSentTo)")
        assertTrue(
            "nobody resolves the remembered number into a conversation.",
            from > 0,
        )
        val body = source.substring(from, minOf(source.length, from + 500))
        assertTrue(
            "the effect does not set the conversation: $body",
            "openThread =" in body,
        )
    }

    @Test
    fun `the row for a new message goes away with the conversation`() {
        val from = source.indexOf("prefilled =")
        assertTrue("the row no longer exists", from > 0)
        val body = source.substring(from, minOf(source.length, from + 300))
        assertTrue(
            "the row hangs on nothing - then it stands there even when the conversation " +
                "has long existed, and the same person appears twice: once with a name, " +
                "once with a number.",
            "threads.none" in body || "takeIf" in body,
        )
    }
}
