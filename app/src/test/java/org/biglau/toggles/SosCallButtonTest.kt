package org.biglau.toggles

import org.biglau.Quelltext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nach einem Notruf steht ein Anruf-Knopf da — und er wählt nicht von selbst.
 *
 * `PLAN.md` 4.8 verlangt beides: „Danach ein **Anruf-Knopf**, kein automatischer Anruf …
 * ein grosser Knopf, der die Wähltastatur mit der Nummer öffnet, ist einen Tipp entfernt und
 * löst nie von selbst aus."
 *
 * Bis zum 3.9.2026 endete der Bildschirm nach dem Senden mit „Schliessen" — und im
 * schlimmsten Fall mit dem Satz „Es konnte nichts gesendet werden". Das ist die Stelle, an
 * der ein Mensch am wenigsten überlegen kann, und sie bot nichts an.
 *
 * Zwei Zusicherungen, und die zweite ist die wichtigere:
 *
 * 1. Der Knopf ist da.
 * 2. Er ruft `Intents.dial` und **nicht** `Intents.call`. `dial` öffnet die Wähltastatur mit
 *    der Nummer; gewählt wird erst durch einen zweiten Tipp eines Menschen. Genau deshalb
 *    steht `SosActivity` auch nicht in der Liste von `HinausTest`.
 */
class SosCallButtonTest {

    private val quelle = Quelltext.ohneKommentare("org/biglau/toggles/SosActivity.kt")

    @Test
    fun `nach dem Senden steht ein Anruf-Knopf da`() {
        assertTrue(
            "kein Anruf-Knopf nach dem Notruf - PLAN.md 4.8 verlangt ihn",
            "R.string.sos_call_now" in quelle && "Intents.dial(" in quelle,
        )
    }

    @Test
    fun `der Notruf-Bildschirm waehlt nie selbst`() {
        assertFalse(
            "SosActivity ruft Intents.call - der Notruf darf nie von selbst wählen",
            "Intents.call(" in quelle,
        )
        assertFalse("ACTION_CALL im Notruf-Bildschirm", "ACTION_CALL" in quelle)
    }

    @Test
    fun `in der Probe steht der Knopf nicht`() {
        val stelle = quelle.indexOf("R.string.sos_call_now")
        val davor = quelle.substring(maxOf(0, stelle - 300), stelle)
        assertTrue(
            "der Knopf muss an !preview hängen - eine Probe hat niemanden angerufen und " +
                "soll auch nicht dazu einladen",
            "!preview" in davor,
        )
    }
}
