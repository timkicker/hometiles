package org.biglau.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.biglau.apps.LaunchableApp
import org.junit.Test

class TextSearchTest {

    private fun app(label: String) = LaunchableApp(label, "pkg.${label.lowercase()}", "pkg.Main")

    private val apps = listOf(
        app("AnkiDroid"),
        app("Brave"),
        app("Calculator"),
        app("Google Maps"),
        app("Müller App"),
        app("Öffi"),
        app("WhatsApp"),
        app("Photo Editor"),
    )

    @Test
    fun `leere Anfrage laesst die Liste unveraendert`() {
        assertEquals(apps, TextSearch.filter(apps, "") { it.label })
        assertEquals(apps, TextSearch.filter(apps, "   ") { it.label })
    }

    @Test
    fun `Anfang des Namens rangiert vor Vorkommen in der Mitte`() {
        // "app" steckt in "WhatsApp" mittendrin, "Müller App" hat es am Wortanfang.
        val result = TextSearch.filter(apps, "app") { it.label }
        assertEquals(listOf("Müller App", "WhatsApp"), result.map { it.label })
    }

    @Test
    fun `Suche findet auch mitten im Wort`() {
        // Genau das kann eine reine Praefixsuche nicht - und deshalb gibt es sie hier.
        assertEquals(listOf("Photo Editor"), TextSearch.filter(apps, "dito") { it.label }.map { it.label })
    }

    @Test
    fun `Gross- und Kleinschreibung spielt keine Rolle`() {
        assertEquals(
            TextSearch.filter(apps, "BRAVE") { it.label }.map { it.label },
            TextSearch.filter(apps, "brave") { it.label }.map { it.label },
        )
    }

    @Test
    fun `Umlaute werden gefunden ohne Umlaute zu tippen`() {
        assertEquals(listOf("Müller App"), TextSearch.filter(apps, "muller") { it.label }.map { it.label })
        assertEquals(listOf("Öffi"), TextSearch.filter(apps, "offi") { it.label }.map { it.label })
    }

    @Test
    fun `Eszett wird wie Doppel-s behandelt`() {
        val list = listOf(app("Straße"))
        assertEquals(1, TextSearch.filter(list, "strasse") { it.label }.size)
    }

    @Test
    fun `mehrere Wortteile muessen alle zutreffen`() {
        assertEquals(listOf("Google Maps"), TextSearch.filter(apps, "goog map") { it.label }.map { it.label })
        assertTrue(TextSearch.filter(apps, "goog zzz") { it.label }.isEmpty())
    }

    @Test
    fun `ohne Treffer bleibt die Liste leer`() {
        assertTrue(TextSearch.filter(apps, "xyzzy") { it.label }.isEmpty())
    }

    @Test
    fun `Rangfolge unterscheidet Anfang Wortanfang und Mitte`() {
        assertEquals(0, TextSearch.rank("Google Maps", "goo"))
        assertEquals(1, TextSearch.rank("Google Maps", "map"))
        assertEquals(2, TextSearch.rank("WhatsApp", "atsa"))
        assertNull(TextSearch.rank("WhatsApp", "zzz"))
    }

    @Test
    fun `Trennzeichen gelten als Wortgrenze`() {
        assertEquals(1, TextSearch.rank("Firefox-Klar", "klar"))
        assertEquals(1, TextSearch.rank("org.mein_dienst", "dienst"))
    }

    @Test
    fun `bei gleichem Rang wird alphabetisch sortiert`() {
        val list = listOf(app("Zebra Tool"), app("Alpha Tool"), app("Mittel Tool"))
        assertEquals(
            listOf("Alpha Tool", "Mittel Tool", "Zebra Tool"),
            TextSearch.filter(list, "tool") { it.label }.map { it.label },
        )
    }

    @Test
    fun `die Reihenfolge bleibt zwischen zwei Tastendruecken stabil`() {
        // Sonst springt die Liste unter dem Finger weg - auf drei Zoll besonders aergerlich.
        val once = TextSearch.filter(apps, "a") { it.label }.map { it.label }
        val twice = TextSearch.filter(apps.shuffled(), "a") { it.label }.map { it.label }
        assertEquals(once, twice)
    }
}
