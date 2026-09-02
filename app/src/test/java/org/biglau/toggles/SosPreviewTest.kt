package org.biglau.toggles

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Probe darf unter keinen Umständen senden.
 *
 * Anlass ist ein Fehler von mir: um den Countdown-Bildschirm ansehen zu können, hatte ich am
 * Emulator eine Testnummer eingetragen und die SMS-Erlaubnis entzogen — und **die
 * Installation der neuen Fassung hat die Erlaubnis wieder erteilt** (`GRANTED_BY_ROLE`). Der
 * nächste Countdown lief durch und schickte die Nachricht an die Testnummer. Es hat nichts den
 * Emulator verlassen, aber verlassen konnte ich mich darauf nicht.
 *
 * Seither gibt es die Probe: derselbe Ablauf, am Ende **kein** `Sos.send`. Wer den Notruf
 * einrichtet, kann ihn damit zeigen, statt ihn zu beschreiben — und prüfen lässt sich der
 * Bildschirm damit auch, ohne dass irgendwo etwas ankommt.
 */
class SosPreviewTest {

    private val quelle = File("src/main/java/org/biglau/toggles/SosActivity.kt").readText()

    @Test
    fun `in der Probe wird nicht gesendet`() {
        val vorProbe = quelle.substringBefore("Sos.send(")
        assertTrue(
            "Vor dem Senden steht keine Abfrage auf die Probe",
            "if (probe)" in vorProbe,
        )
        assertTrue("Die Probe kehrt nicht zurueck, bevor gesendet wird", "return@LaunchedEffect" in vorProbe)
    }

    @Test
    fun `die Probe heisst auch so`() {
        // Sonst weiss niemand, der den Bildschirm zufaellig sieht, ob es gerade ernst ist.
        assertTrue("sos_preview_title" in quelle)
        assertTrue("sos_preview_done" in quelle)
    }

    @Test
    fun `die Probe laeuft auch ohne eingetragene Kontakte`() {
        // Sonst muesste man erst eine Nummer eintragen, um den Ablauf zu sehen - genau die
        // Reihenfolge, die den Fehler oben moeglich gemacht hat.
        assertTrue("if (!configured && !probe) return@LaunchedEffect" in quelle)
    }

    /**
     * Die Probe zeigt auch **was** hinausginge. Vorher liess sich der Text nur
     * herausfinden, indem man ihn abschickte — auf einem Weg, den man nicht ausprobieren
     * will. Wer den Notruf fuer jemanden einrichtet, soll ihn lesen koennen, bevor er im
     * Ernstfall bei jemand anderem ankommt.
     */
    @Test
    fun `die Probe zeigt den Text, der hinausginge`() {
        assertTrue("der Text wird nicht zusammengesetzt", "Sos.compose(" in quelle)
        assertTrue("der Text wird nicht angezeigt", "previewText" in quelle)
        assertTrue(
            "es steht nicht dabei, ob ein Standort drin ist",
            "sos_preview_text_location" in quelle,
        )
    }
}
