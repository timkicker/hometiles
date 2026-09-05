package org.biglau.toggles

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Speichern sagt, dass es gespeichert hat — und was.
 *
 * Auf der SOS-Seite stehen die einzigen zwei Knöpfe der App, deren ganze Wirkung unsichtbar
 * ist: die Nummern und der Text der Nachricht. Alles andere in den Einstellungen ändert sich
 * vor den Augen, oder der Bildschirm schliesst sich.
 *
 * Am 04.09.2026 am Emulator: „Nummern speichern" angetippt, und der Bildschirm sah aus wie
 * vorher. Bei Nummern, die man einmal einträgt und hoffentlich nie braucht, ist „hat es
 * geklappt?" die einzige Frage, die zählt.
 *
 * Zwei Sachen fehlten, und beide sind Klassiker aus dieser Datei:
 *
 * * **Ohne Änderung ist es kein Knopf.** Er sah immer gleich aus und liess sich immer
 *   drücken, obwohl nichts zu speichern war — wie das Senden ohne Text und das Anrufen ohne
 *   Nummer.
 * * **Die Rückmeldung sagt, was wirklich passiert ist.** Ein „x" ins Feld, speichern: „Nummern
 *   gespeichert" wäre wahr geklungen und falsch gewesen — die rote Zeile darüber sagt ja
 *   gerade, dass nichts davon brauchbar ist. Jetzt zählt die Meldung: keine, eine, mehrere.
 */
class SosSpeichernTest {

    private val quelle = Quelltext.ohneKommentare("org/biglau/toggles/SosSettings.kt")

    @Test
    fun `ohne aenderung sind beide knoepfe still`() {
        listOf(
            "changed" to "die Nummern",
            "messageChanged" to "die Nachricht",
        ).forEach { (kennzeichen, was) ->
            assertTrue(
                "Der Knopf fuer $was traegt die Akzentfarbe auch ohne Aenderung",
                "if ($kennzeichen) palette.surfaceAccent else palette.surfaceDefault" in quelle,
            )
            assertTrue(
                "Der Knopf fuer $was laesst sich auch ohne Aenderung druecken",
                "onClick = if ($kennzeichen) {" in quelle,
            )
        }
    }

    @Test
    fun `die rueckmeldung unterscheidet den leeren fall`() {
        val speichern = Quelltext.ausschnitt(
            quelle,
            von = "val taken = SosNumbers.parse(numbersText)",
            bis = "\n                    }",
        )
        assertTrue(
            "Die Meldung nach dem Speichern zaehlt nicht nach, was uebernommen wurde:\n" +
                speichern,
            "taken.isEmpty()" in speichern && "sos_numbers_cleared" in speichern,
        )
        assertTrue(
            "Bei uebernommenen Nummern fehlt die Mehrzahlform",
            "sos_numbers_saved_n" in speichern,
        )
    }

    @Test
    fun `beide meldungen gibt es in beiden sprachen`() {
        listOf("values-de", "values").forEach { sprache ->
            val text = Quelltext.textWert("sos_numbers_cleared", sprache)
            assertTrue("sos_numbers_cleared fehlt in $sprache", text.isNotBlank())
        }
    }
}
