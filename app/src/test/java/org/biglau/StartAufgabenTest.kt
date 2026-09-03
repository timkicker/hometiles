package org.biglau

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Was beim Start eingehängt werden muss, wird beim Start eingehängt.
 *
 * `PhoneNumbers` rechnet nur und liegt deshalb im reinen Kotlin-Modul. Zwei Auskünfte kann
 * es dort nicht selbst holen — die Schreibweise einer Nummer und das Land der SIM —, und
 * beide hängt `SystemNumbers.install` beim Start ein.
 *
 * Wer den Aufruf entfernt, bekommt **keinen** Fehler: BigLau schreibt Rufnummern dann in
 * blossen Dreierbloecken weiter. Genau so ist der Fehler entstanden, den der Nutzer am
 * 2.9.2026 an seinen eigenen Kontakten gesehen hat — „+436 804 …", wobei Österreich „+43"
 * ist. Ein stiller Rückschritt, den niemand meldet.
 */
class StartAufgabenTest {

    /**
     * Ohne Kommentarzeilen gelesen.
     *
     * Beim Gegenprobieren fiel die erste Fassung dieser Regel selbst herein: den Aufruf
     * auszukommentieren liess sie grün, denn die Zeichenkette stand ja noch da. Eine Regel,
     * die einen auskommentierten Aufruf für einen Aufruf hält, prüft nichts.
     */
    private val start = Quelltext.datei("org/biglau/BigLauApp.kt")
        .readLines()
        .filterNot { it.trim().startsWith("//") }
        .joinToString("\n")

    @Test
    fun `der start haengt die systemteile der rufnummern ein`() {
        assertTrue(
            "BigLauApp ruft SystemNumbers.install nicht mehr. Ohne den Aufruf schreibt " +
                "BigLau Rufnummern in blossen Dreierblöcken - ohne Fehler, ohne Absturz.",
            "SystemNumbers.install" in start,
        )
    }

    @Test
    fun `der start fasst das telefon nicht selbst an`() {
        val framework = listOf("TelephonyManager", "PhoneNumberUtils")
            .filter { it in start }
        assertTrue(
            "BigLauApp fasst Android-Telefonie direkt an: $framework. Das gehört nach " +
                "core:system (PLAN.md 2.1), sonst steht der Systemzugriff wieder überall.",
            framework.isEmpty(),
        )
    }
}
