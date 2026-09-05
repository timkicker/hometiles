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
    private val start = Quelltext.file("org/biglau/BigLauApp.kt")
        .readLines()
        .filterNot { Quelltext.isCommentLine(it) }
        .joinToString("\n")

    @Test
    fun `der start haengt die systemteile der rufnummern ein`() {
        assertTrue(
            "BigLauApp ruft SystemNumbers.install nicht mehr. Ohne den Aufruf schreibt " +
                "BigLau Rufnummern in blossen Dreierblöcken - ohne Fehler, ohne Absturz.",
            "SystemNumbers.install" in start,
        )
    }

    /**
     * Der Wecker für die wiederholte Erinnerung hängt an `ELAPSED_REALTIME_WAKEUP` und ist
     * nach einem Neustart des Telefons weg. Einen `BOOT_COMPLETED`-Empfänger gibt es
     * bewusst nicht — er kostete eine weitere Berechtigung, und BigLau **ist** der
     * Startbildschirm: es läuft nach jedem Neustart ohnehin.
     *
     * Ohne diese Zeile erinnerte eine Nachricht, die vor dem Neustart ungelesen war, nie
     * wieder — lautlos, und genau das verspricht die Einstellung.
     */
    @Test
    fun `der start stellt den wecker fuer die erinnerung wieder`() {
        assertTrue(
            "BigLauApp stellt den Erinnerungs-Wecker nicht mehr - nach einem Neustart " +
                "erinnert dann nichts mehr an eine ungelesene Nachricht.",
            "MessageReminderReceiver.schedule" in start,
        )
        assertTrue(
            "ohne die Prüfung auf SmsReminder.active stellt BigLau bei jedem Start einen " +
                "Wecker, den niemand bestellt hat",
            "SmsReminder.active" in start,
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
