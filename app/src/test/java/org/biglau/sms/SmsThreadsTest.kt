package org.biglau.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsThreadsTest {

    private var nextId = 1L
    private fun msg(
        thread: Long,
        at: Long,
        body: String = "Hallo",
        address: String = "+436601234567",
        incoming: Boolean = true,
        read: Boolean = true,
    ) = SmsMessage(nextId++, thread, address, body, at, incoming, read)

    @Test
    fun `Nachrichten eines Gespraechs werden zusammengefasst`() {
        val threads = SmsThreads.group(listOf(msg(1, 100), msg(1, 200), msg(2, 150)))
        assertEquals(2, threads.size)
        assertEquals(2, threads.first { it.threadId == 1L }.messageCount)
    }

    @Test
    fun `das neueste Gespraech steht oben`() {
        val threads = SmsThreads.group(listOf(msg(1, 100), msg(2, 500)))
        assertEquals(2L, threads.first().threadId)
    }

    @Test
    fun `die letzte Nachricht bestimmt Vorschau und Adresse`() {
        val threads = SmsThreads.group(
            listOf(msg(1, 100, body = "alt"), msg(1, 300, body = "neu")),
        )
        assertEquals("neu", threads.first().lastMessage.body)
    }

    @Test
    fun `ungelesen zaehlen nur eingehende Nachrichten`() {
        // Eine selbst geschriebene Nachricht ist nie ungelesen - sonst blinkt die Kachel,
        // weil man selbst etwas geschrieben hat.
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
    fun `ohne ungelesene Nachricht bleibt es bei null`() {
        val threads = SmsThreads.group(listOf(msg(1, 100, read = true)))
        assertTrue(!threads.first().hasUnread)
        assertEquals(0, SmsThreads.totalUnread(threads))
    }

    @Test
    fun `die Summe der Ungelesenen geht ueber alle Gespraeche`() {
        val threads = SmsThreads.group(
            listOf(
                msg(1, 100, incoming = true, read = false),
                msg(2, 100, incoming = true, read = false),
                msg(2, 200, incoming = true, read = false),
            ),
        )
        assertEquals(3, SmsThreads.totalUnread(threads))
    }

    @Test
    fun `der Name des Kontakts wird uebernommen wenn es einen gibt`() {
        val threads = SmsThreads.group(listOf(msg(1, 100))) { "Oma" }
        assertEquals("Oma", threads.first().title)
    }

    @Test
    fun `ohne Namen steht die Adresse als Titel`() {
        val threads = SmsThreads.group(listOf(msg(1, 100)))
        assertEquals("+436601234567", threads.first().title)
    }

    @Test
    fun `die Vorschau ist einzeilig`() {
        val message = msg(1, 100, body = "Erste Zeile\nZweite  Zeile")
        assertEquals("Erste Zeile Zweite Zeile", SmsThreads.preview(message))
    }

    @Test
    fun `lange Vorschauen werden gekuerzt`() {
        val long = "a".repeat(200)
        val preview = SmsThreads.preview(msg(1, 100, body = long))
        assertEquals(SmsThreads.PREVIEW_LENGTH, preview.length)
        assertTrue(preview.endsWith("…"))
    }

    @Test
    fun `eine Unterhaltung liest sich von alt nach neu`() {
        val messages = listOf(msg(1, 300), msg(1, 100), msg(1, 200), msg(2, 400))
        val conversation = SmsThreads.conversation(messages, 1L)
        assertEquals(listOf(100L, 200L, 300L), conversation.map { it.timestamp })
    }

    @Test
    fun `Schreibweisen derselben Nummer gelten als gleich`() {
        assertTrue(SmsThreads.sameAddress("+43 660 123", "+43660123"))
        assertTrue(!SmsThreads.sameAddress("+43660123", "+43660124"))
    }

    @Test
    fun `eine leere Adresse gilt nie als gleich`() {
        // Sonst waeren alle unterdrueckten Absender eine Person - derselbe Fehler wie
        // damals bei der Anrufliste.
        assertTrue(!SmsThreads.sameAddress("", ""))
        assertTrue(!SmsThreads.sameAddress("", "+43660"))
    }

    @Test
    fun `eine leere Liste ergibt keine Gespraeche`() {
        assertTrue(SmsThreads.group(emptyList()).isEmpty())
    }
}
