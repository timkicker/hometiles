package org.biglau.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Die Adresse einer Webseiten-Kachel.
 *
 * Auf drei Zoll tippt niemand freiwillig „https://". Die Eingabe wird deshalb ergänzt statt
 * abgelehnt - eine Kachel, die „ungültige Adresse" sagt, weil das Vorwort fehlt, ist
 * schlechter als eine, die es hinzufügt.
 */
class LinkTargetTest {

    @Test
    fun `ohne Vorwort wird https ergaenzt`() {
        assertEquals("https://orf.at", LinkTarget.normalise("orf.at"))
        assertEquals("https://www.wien.gv.at", LinkTarget.normalise("www.wien.gv.at"))
    }

    @Test
    fun `ein vorhandenes Vorwort bleibt stehen`() {
        assertEquals("http://alte-seite.at", LinkTarget.normalise("http://alte-seite.at"))
        assertEquals("https://orf.at", LinkTarget.normalise("https://orf.at"))
    }

    @Test
    fun `andere Schemata bleiben unangetastet`() {
        // tel: und mailto: haben ihren Sinn; ein vorangestelltes https waere Unsinn.
        assertEquals("mailto:hallo@example.at", LinkTarget.normalise("mailto:hallo@example.at"))
        assertEquals("geo:47.0,11.4", LinkTarget.normalise("geo:47.0,11.4"))
    }

    @Test
    fun `Leerraum am Rand stoert nicht`() {
        assertEquals("https://orf.at", LinkTarget.normalise("  orf.at  "))
    }

    @Test
    fun `ein Leerzeichen mittendrin heisst keine Adresse`() {
        assertNull(LinkTarget.normalise("das ist keine adresse"))
    }

    @Test
    fun `leer bleibt leer`() {
        assertNull(LinkTarget.normalise(""))
        assertNull(LinkTarget.normalise("   "))
    }

    @Test
    fun `etwas ohne Punkt ist kein Rechnername`() {
        assertNull(LinkTarget.normalise("orf"))
    }

    @Test
    fun `der Rechnername steht auf der Kachel`() {
        assertEquals("orf.at", LinkTarget.labelFor("https://orf.at/news/wetter"))
        assertEquals("wien.gv.at", LinkTarget.labelFor("https://www.wien.gv.at/"))
    }

    @Test
    fun `Pfad, Abfrage und Anker gehoeren nicht zum Namen`() {
        assertEquals("example.at", LinkTarget.hostOf("https://example.at/pfad?a=1#hier"))
    }

    @Test
    fun `ein Port gehoert auch nicht dazu`() {
        assertEquals("example.at", LinkTarget.hostOf("https://example.at:8443/x"))
    }

    @Test
    fun `ohne Rechnernamen steht die Adresse selbst da`() {
        assertEquals("mailto:hallo@example.at", LinkTarget.labelFor("mailto:hallo@example.at"))
    }
}
