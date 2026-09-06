package dev.kicker.hometiles.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsThreadsTest {

    private var nextId = 1L
    private fun msg(
        thread: Long,
        at: Long,
        body: String = "Hello",
        address: String = "+436601234567",
        incoming: Boolean = true,
        read: Boolean = true,
    ) = SmsMessage(nextId++, thread, address, body, at, incoming, read)

    @Test
    fun `messages of one conversation are grouped`() {
        val threads = SmsThreads.group(listOf(msg(1, 100), msg(1, 200), msg(2, 150)))
        assertEquals(2, threads.size)
    }

    @Test
    fun `the newest conversation stands on top`() {
        val threads = SmsThreads.group(listOf(msg(1, 100), msg(2, 500)))
        assertEquals(2L, threads.first().threadId)
    }

    @Test
    fun `the last message decides preview and address`() {
        val threads = SmsThreads.group(
            listOf(msg(1, 100, body = "old"), msg(1, 300, body = "new")),
        )
        assertEquals("new", threads.first().lastMessage.body)
    }

    @Test
    fun `only incoming messages count as unread`() {
        // a message one wrote oneself is never unread - otherwise the tile blinks because one
        // has written something.
        val threads = SmsThreads.group(
            listOf(
                msg(1, 100, incoming = true, read = false),
                msg(1, 200, incoming = false, read = false),
            ),
        )
        assertEquals(1, threads.first().unreadCount)
        assertTrue(threads.first().hasUnread)
    }

    @Test
    fun `without an unread message it stays at zero`() {
        val threads = SmsThreads.group(listOf(msg(1, 100, read = true)))
        assertTrue(!threads.first().hasUnread)
    }

    @Test
    fun `every conversation counts its own unread ones`() {
        val threads = SmsThreads.group(
            listOf(
                msg(1, 100, incoming = true, read = false),
                msg(2, 100, incoming = true, read = false),
                msg(2, 200, incoming = true, read = false),
            ),
        ).associateBy { it.threadId }
        assertEquals(1, threads.getValue(1L).unreadCount)
        assertEquals(2, threads.getValue(2L).unreadCount)
    }

    @Test
    fun `the contact's name is taken over where there is one`() {
        val threads = SmsThreads.group(listOf(msg(1, 100))) { "Alex" }
        assertEquals("Alex", threads.first().title)
    }

    @Test
    fun `without a name the address stands as the title`() {
        val threads = SmsThreads.group(listOf(msg(1, 100)))
        // in blocks, as in the call log. the raw number stood here before, and the same
        // number looked different in the two lists - whoever compares then compares two
        // spellings instead of two numbers.
        assertEquals("+436 601 234 567", threads.first().title)
    }

    @Test
    fun `the preview is one line`() {
        val message = msg(1, 100, body = "first line\nsecond  line")
        assertEquals("first line second line", SmsThreads.preview(message))
    }

    @Test
    fun `long previews are cut`() {
        val long = "a".repeat(200)
        val preview = SmsThreads.preview(msg(1, 100, body = long))
        assertEquals(SmsThreads.PREVIEW_LENGTH, preview.length)
        assertTrue(preview.endsWith("…"))
    }

    @Test
    fun `a conversation reads from old to new`() {
        val messages = listOf(msg(1, 300), msg(1, 100), msg(1, 200), msg(2, 400))
        val conversation = SmsThreads.conversation(messages, 1L)
        assertEquals(listOf(100L, 200L, 300L), conversation.map { it.timestamp })
    }

    @Test
    fun `grouping goes by the provider's id, not by the number`() {
        // deliberately so, see SmsThreads: grouping by the number would throw conversations
        // together that the provider keeps apart.
        val threads = SmsThreads.group(
            listOf(msg(1, 100, address = "+43 660 123"), msg(2, 200, address = "+43660123")),
        )
        assertEquals(2, threads.size)
    }

    @Test
    fun `an empty list yields no conversations`() {
        assertTrue(SmsThreads.group(emptyList()).isEmpty())
    }

    // --- senders that are no number (02.09.2026) ---

    /**
     * banks, parcel services and sign-in codes come as a letter id. until here **nothing** of
     * it was left: `PhoneNumbers.clean` throws letters away, and an empty row stood in the
     * list - on exactly the messages one looks for most.
     */
    @Test
    fun `a letter id stands there as the title`() {
        val threads = SmsThreads.group(listOf(msg(1, 100, address = "ADAC")))
        assertEquals("ADAC", threads.first().title)
    }

    @Test
    fun `without a sender the replacement text stands there`() {
        val threads = SmsThreads.group(listOf(msg(1, 100, address = "")))
        assertEquals("", threads.first().title)
        assertEquals("Unknown", threads.first().titleOr("Unknown"))
    }

    @Test
    fun `with a sender the replacement text stays away`() {
        val threads = SmsThreads.group(listOf(msg(1, 100)))
        assertEquals("+436 601 234 567", threads.first().titleOr("Unknown"))
    }
}
