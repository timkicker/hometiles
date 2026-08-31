package org.biglau.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactAvatarTest {

    @Test
    fun `zwei Namensteile ergeben zwei Initialen`() {
        assertEquals("AB", initialsOf("Anna Berger"))
    }

    @Test
    fun `bei mehr als zwei Teilen zaehlen erster und letzter`() {
        assertEquals("AZ", initialsOf("Anna Maria Zimmermann"))
    }

    @Test
    fun `ein einzelner Name ergibt eine Initiale`() {
        assertEquals("A", initialsOf("Anna"))
    }

    @Test
    fun `leerer Name ergibt ein Fragezeichen statt eines leeren Kastens`() {
        assertEquals("?", initialsOf(""))
        assertEquals("?", initialsOf("   "))
    }

    @Test
    fun `ueberfluessige Leerzeichen stoeren nicht`() {
        assertEquals("AB", initialsOf("  Anna   Berger  "))
    }

    @Test
    fun `Initialen sind immer gross`() {
        assertEquals("AB", initialsOf("anna berger"))
    }

    @Test
    fun `Satzzeichen zaehlen nicht als Initiale`() {
        // Aus dem echten Telefonbuch des Geraets - ergab vorher "?(".
        assertEquals("WS", initialsOf("? (Wien) (Sus)"))
        assertEquals("A", initialsOf("(Anna)"))
        assertEquals("MK", initialsOf("Müller - Klein"))
    }

    @Test
    fun `ein Name ganz ohne Buchstaben ergibt ein Fragezeichen`() {
        assertEquals("?", initialsOf("??? ---"))
    }

    @Test
    fun `Ziffern duerfen Initiale sein`() {
        assertEquals("1F", initialsOf("1. Feuerwehr"))
    }

    @Test
    fun `die Farbe haengt nur am Namen`() {
        // Sonst springt sie bei jedem Neuzeichnen und der Kontakt ist nicht wiedererkennbar.
        assertEquals(colorIndexFor("Anna Berger"), colorIndexFor("Anna Berger"))
        assertEquals(colorIndexFor("Anna Berger"), colorIndexFor("  anna berger "))
    }

    @Test
    fun `verschiedene Namen bekommen ueberwiegend verschiedene Farben`() {
        val names = listOf("Anna", "Bertha", "Carl", "Dora", "Emil", "Frieda")
        val buckets = names.map { colorIndexFor(it).mod(6) }.toSet()
        assertTrue("Nur ${buckets.size} verschiedene Farben fuer 6 Namen", buckets.size >= 4)
    }
}
