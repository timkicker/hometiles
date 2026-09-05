package org.biglau.settings

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine Warnung zur Sicherung steht vor dem Laden, nicht danach.
 *
 * Eine Sicherung aus einem **neueren** BigLau enthält Felder, die diese Fassung nicht kennt;
 * sie fallen beim Einlesen weg. `ConfigTransfer.isFromNewerVersion` erkennt das seit Tagen,
 * und der Satz dazu stand auch da — nur an der falschen Stelle: erst **nach** dem Laden,
 * unter der Bedingung `done && vonNeuerer`.
 *
 * Am 04.09.2026 am Emulator nachgestellt, mit einer Datei, die `"version": 99` trug: die
 * Rückfrage sagte nur „2 Bildschirme mit 12 Kacheln. Sie ersetzt Ihre jetzige Einrichtung."
 * Geladen — und **dann** kam „Die Datei stammt aus einem neueren BigLau, was diese Fassung
 * nicht kennt, blieb weg." Wer das danach liest, kann nichts mehr entscheiden: die alte
 * Belegung ist bereits ersetzt.
 *
 * Die Regel: der Bildschirm nennt die Herkunft **auch** im Zustand vor der Bestätigung.
 */
class WarnungVorDemLadenTest {

    private val importieren = Quelltext.ohneKommentare("org/biglau/settings/ImportActivity.kt")

    @Test
    fun `die herkunft steht vor der bestaetigung`() {
        val vorher = Regex("""!done && fromNewer|fromNewer && !done""").containsMatchIn(importieren)
        assertTrue(
            "ImportActivity nennt die neuere Herkunft nur nach dem Laden. Dann ist die " +
                "bestehende Einrichtung schon ersetzt, und die Auskunft kommt zu spaet.",
            vorher,
        )
    }

    @Test
    fun `der satz davor sagt was passieren wird und nicht was passiert ist`() {
        listOf("values-de", "values").forEach { sprache ->
            val text = Quelltext.textWert("transfer_confirm_newer", sprache)
            assertTrue(
                "Der Satz vor dem Laden steht in der Vergangenheit (Sprache \"$sprache\"): " +
                    text + "\nVor der Entscheidung gehoert die Zukunft: was *wird* wegfallen.",
                "bleibt" in text || "will be" in text,
            )
        }
    }
}
