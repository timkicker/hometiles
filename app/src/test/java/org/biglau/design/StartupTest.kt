package org.biglau.design

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Startweg trägt nichts, was warten kann.
 *
 * Am Jelly 2 gemessen: vom Antippen bis zum sichtbaren Startbildschirm **zwei Sekunden**
 * (Fehlersuchfassung). Der Anteil der App daran war klein, aber messbar — allein das erste
 * Einlesen der Einrichtung kostete **186 ms**, und zwar nicht wegen der Datei (zwölf
 * Kilobyte), sondern wegen des ersten Benutzens des Umwandlers.
 *
 * Auf dem Startfaden lag das vor allem anderen. Daneben läuft es jetzt mit, während Android
 * die Activity hochzieht — gefahrlos, weil `ConfigStore.get` gegen zwei gleichzeitige Aufrufe
 * gesichert ist: kommt die Oberfläche früher, wartet sie genauso lange wie vorher. Gemessen
 * hat es rund **80 bis 130 ms** gebracht.
 */
class StartupTest {

    /** Ohne Kommentare: eine Erklaerung darf die Regel nennen, ohne sie zu brechen. */
    private val app = Quelltext.datei("org/biglau/BigLauApp.kt")
        .readLines()
        .filterNot { it.trimStart().startsWith("//") || it.trimStart().startsWith("*") }
        .joinToString("\n")

    @Test
    fun `die Einrichtung wird nicht auf dem Startfaden eingelesen`() {
        val vorDemFaden = app.substringBefore("Thread {")
        assertTrue("kein eigener Faden im Start", "Thread {" in app)
        assertTrue(
            "ConfigStore wird noch auf dem Startfaden gebaut",
            "ConfigStore.get" !in vorDemFaden,
        )
    }

    @Test
    fun `der Absturzschreiber bleibt vorne`() {
        // Er kostet zwei Millisekunden und muss stehen, bevor irgendetwas abstuerzen kann -
        // sonst hat der Notmodus beim naechsten Start nichts anzuzeigen.
        val vorDemFaden = app.substringBefore("Thread {")
        assertTrue("CrashRecorder fehlt am Anfang", "CrashRecorder.get" in vorDemFaden)
    }
}
