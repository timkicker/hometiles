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
    }

    @Test
    fun `jedes Gespraech zaehlt seine eigenen Ungelesenen`() {
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
    fun `der Name des Kontakts wird uebernommen wenn es einen gibt`() {
        val threads = SmsThreads.group(listOf(msg(1, 100))) { "Oma" }
        assertEquals("Oma", threads.first().title)
    }

    @Test
    fun `ohne Namen steht die Adresse als Titel`() {
        val threads = SmsThreads.group(listOf(msg(1, 100)))
        // In Bloecken, wie in der Anrufliste. Vorher stand hier die rohe Nummer, und
        // dieselbe Nummer sah in den beiden Listen verschieden aus - wer vergleicht,
        // vergleicht dann zwei Schreibweisen statt zweier Nummern.
        assertEquals("+436 601 234 567", threads.first().title)
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
    fun `gruppiert wird nach der Kennung des Anbieters, nicht nach der Nummer`() {
        // Bewusst so, siehe SmsThreads: nach der Nummer zu gruppieren wuerfe Gespraeche
        // zusammen, die der Anbieter getrennt fuehrt. Eine eigene Nummernvergleichs-
        // funktion gab es dafuer einmal - sie hat nie jemand aufgerufen.
        val threads = SmsThreads.group(
            listOf(msg(1, 100, address = "+43 660 123"), msg(2, 200, address = "+43660123")),
        )
        assertEquals(2, threads.size)
    }

    @Test
    fun `eine leere Liste ergibt keine Gespraeche`() {
        assertTrue(SmsThreads.group(emptyList()).isEmpty())
    }

    // --- Absender, die keine Nummer sind (02.09.2026) ---

    /**
     * Banken, Paketdienste und Anmeldecodes kommen als Buchstabenkennung. Bis hierher
     * blieb davon **nichts** uebrig: `PhoneNumbers.clean` wirft Buchstaben weg, und in der
     * Liste stand eine leere Zeile - bei genau den Nachrichten, die man am ehesten sucht.
     */
    @Test
    fun `eine Buchstabenkennung steht als Titel da`() {
        val threads = SmsThreads.group(listOf(msg(1, 100, address = "ADAC")))
        assertEquals("ADAC", threads.first().title)
    }

    @Test
    fun `ohne Absender steht der Ersatztext da`() {
        val threads = SmsThreads.group(listOf(msg(1, 100, address = "")))
        assertEquals("", threads.first().title)
        assertEquals("Unbekannt", threads.first().titleOr("Unbekannt"))
    }

    @Test
    fun `mit Absender bleibt der Ersatztext weg`() {
        val threads = SmsThreads.group(listOf(msg(1, 100)))
        assertEquals("+436 601 234 567", threads.first().titleOr("Unbekannt"))
    }
}
