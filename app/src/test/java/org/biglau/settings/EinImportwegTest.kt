package org.biglau.settings

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine Sicherung wird an **einer** Stelle eingelesen.
 *
 * Es gibt zwei Wege zu derselben Sache: die Datei von aussen antippen, und in den
 * Einstellungen "Aus einer Datei laden". Der erste ging ueber `ImportActivity` - dort steht,
 * was in der Datei ist ("Diese Sicherung enthaelt 3 Screens mit 14 Kacheln"), und erst dann
 * wird gefragt. Der zweite las selbst und ersetzte die ganze Einrichtung, sobald eine Datei
 * gewaehlt war.
 *
 * Zwei Antworten auf dieselbe Frage, und der haeufigere Weg war der unvorsichtigere. Dazu
 * hiess dort eine Datei, die sich gar nicht **oeffnen** liess, "Das ist keine
 * BigLau-Sicherung" - drueben gibt es dafuer zwei verschiedene Saetze.
 */
class EinImportwegTest {

    private val einstellungen = Quelltext.withoutComments("org/biglau/settings/SettingsActivity.kt")

    @Test
    fun `nur eine Stelle ersetzt die Einrichtung aus einer Datei`() {
        val stellen = Quelltext.files()
            // Beide Schreibweisen: `ImportActivity` reicht die Funktion als Referenz
            // weiter (`ConfigTransfer::import`), die erste Fassung dieser Regel suchte nur
            // den Aufruf mit Klammer - und fand deshalb gar nichts.
            .filter { datei ->
                val text = datei.readText()
                "ConfigTransfer.import(" in text || "ConfigTransfer::import" in text
            }
            .map { it.name }
            .sorted()
        assertEquals(
            "Mehr als eine Stelle liest eine Sicherung ein. Dann gibt es zwei Antworten " +
                "auf die Frage, ob vorher gefragt wird - und sie laufen auseinander.",
            listOf("ImportActivity.kt"),
            stellen,
        )
    }

    @Test
    fun `die Einstellungen reichen die Datei weiter`() {
        assertTrue(
            "Die Einstellungen oeffnen den Dateidialog, geben die Datei aber nicht an " +
                "ImportActivity weiter - dann fehlt die Rueckfrage, die dort steht.",
            "ImportActivity::class.java" in einstellungen,
        )
    }
}
