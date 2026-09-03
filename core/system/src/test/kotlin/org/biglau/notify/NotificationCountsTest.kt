package org.biglau.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCountsTest {

    private fun row(
        pkg: String,
        clearable: Boolean = true,
        ongoing: Boolean = false,
        groupSummary: Boolean = false,
    ) = NotificationRow(pkg, clearable, ongoing, groupSummary)

    @Test
    fun `normale Nachrichten werden pro Paket gezaehlt`() {
        val counts = NotificationCounts.summarise(
            listOf(row("chat"), row("chat"), row("mail")),
        )
        assertEquals(mapOf("chat" to 2, "mail" to 1), counts)
    }

    @Test
    fun `laufende Anzeigen zaehlen nicht`() {
        // Musikwiedergabe und USB-Debugging liegen dauerhaft an. Wuerden sie zaehlen,
        // blinkte die Kachel fuer immer - und der Nutzer lernt, sie zu ignorieren.
        val counts = NotificationCounts.summarise(
            listOf(row("player", ongoing = true), row("player")),
        )
        assertEquals(mapOf("player" to 1), counts)
    }

    @Test
    fun `nicht wegwischbare Anzeigen zaehlen nicht`() {
        assertTrue(NotificationCounts.summarise(listOf(row("system", clearable = false))).isEmpty())
    }

    @Test
    fun `Gruppenzusammenfassungen zaehlen nicht doppelt`() {
        // Viele Apps melden drei Nachrichten plus eine Zusammenfassung. Ohne diese Regel
        // stuende auf der Kachel eine Vier.
        val counts = NotificationCounts.summarise(
            listOf(row("chat"), row("chat"), row("chat"), row("chat", groupSummary = true)),
        )
        assertEquals(mapOf("chat" to 3), counts)
    }

    @Test
    fun `eine leere Liste ergibt keine Zaehler`() {
        assertTrue(NotificationCounts.summarise(emptyList()).isEmpty())
    }

    @Test
    fun `ein Paket ohne zaehlbare Anzeige taucht gar nicht auf`() {
        // Wichtig: nicht mit Null eintragen, sonst blinkt die Kachel bei jeder Aenderung kurz.
        val counts = NotificationCounts.summarise(listOf(row("player", ongoing = true)))
        assertTrue(!counts.containsKey("player"))
    }

    @Test
    fun `die Anzeige stapelt ab zehn`() {
        assertNull(NotificationCounts.badgeText(0))
        assertNull(NotificationCounts.badgeText(-1))
        assertEquals("1", NotificationCounts.badgeText(1))
        assertEquals("9", NotificationCounts.badgeText(9))
        assertEquals("9+", NotificationCounts.badgeText(10))
        assertEquals("9+", NotificationCounts.badgeText(250))
    }

    @Test
    fun `die Einzelpruefung deckt sich mit der Zusammenfassung`() {
        val rows = listOf(
            row("a"),
            row("b", ongoing = true),
            row("c", clearable = false),
            row("d", groupSummary = true),
        )
        val expected = rows.filter { NotificationCounts.counts(it) }.map { it.packageName }.toSet()
        assertEquals(expected, NotificationCounts.summarise(rows).keys)
    }
}
