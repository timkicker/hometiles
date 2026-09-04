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
        category: String? = null,
        mediaStyle: Boolean = false,
    ) = NotificationRow(pkg, clearable, ongoing, groupSummary, 0, category, mediaStyle)

    /**
     * Die pausierte Wiedergabe: der Fall, der am 04.09.2026 am Jelly 2 blinkte.
     *
     * Waehrend Spotify spielt, ist die Anzeige `ongoing` und faellt schon durch die alte
     * Regel. Pausiert ist sie es nicht mehr, laesst sich wegwischen und sah damit aus wie
     * eine wartende Nachricht. Sie ist aber dieselbe Anzeige und meldet nichts Neues.
     */
    @Test
    fun `eine pausierte Wiedergabe zaehlt nicht`() {
        val pausiert = row("musik", clearable = true, ongoing = false, mediaStyle = true)
        assertTrue("die Vorlage allein muss reichen", !NotificationCounts.counts(pausiert))
        assertEquals(
            emptyMap<String, Int>(),
            NotificationCounts.summarise(listOf(pausiert)),
        )
    }

    @Test
    fun `Anzeigen ueber etwas Laufendes zaehlen nicht`() {
        // Was die App selbst als Kategorie angibt. Alles hier ist eine Anzeige ueber etwas,
        // das laeuft oder gilt, und keine Nachricht, auf die jemand antworten wuerde.
        listOf("transport", "service", "progress", "navigation", "call", "alarm", "sys").forEach {
            assertTrue(
                "Kategorie $it darf nicht blinken",
                !NotificationCounts.counts(row("app", category = it)),
            )
        }
    }

    @Test
    fun `eine Nachricht zaehlt weiterhin`() {
        // Die Gegenprobe: ohne sie koennte die Liste zu weit werden und alles wegfiltern.
        listOf(null, "msg", "email", "social", "event", "reminder").forEach {
            assertTrue(
                "Kategorie $it ist eine Nachricht und muss zaehlen",
                NotificationCounts.counts(row("app", category = it)),
            )
        }
    }

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
