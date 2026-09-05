package org.biglau.sms

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein Knopf, der nichts tun kann, sieht auch nicht so aus.
 *
 * `send` weigert sich bei einer leeren Nachricht - `if (body.isBlank()) return`. Das ist
 * richtig; falsch war, dass man es nicht sah: der Senden-Knopf stand in voller Akzentfarbe
 * da, man tippte, und es geschah **schweigend nichts**. Am 04.09.2026 am Jelly 2 gesehen,
 * mit offener Tastatur und leerem Feld.
 *
 * Denselben Fall haelt `LaunchFailureTest` fuer die Kacheln fest, und `BigRow` sagt es in
 * seiner eigenen Beschreibung: eine Zeile mit leerer Handlung sieht aus wie ein Knopf,
 * schluckt den Tipp und wird als Schaltflaeche angesagt. Wer sich darauf verlaesst, tippt
 * und wartet auf etwas, das nie kommt.
 */
class LeererKnopfTest {

    private val quelle = Quelltext.withoutComments("org/biglau/sms/SmsActivity.kt")

    @Test
    fun `das Senden weigert sich weiter bei leerem Text`() {
        val senden = Quelltext.cut(quelle, "private fun send(", "\n    }")
        assertTrue(
            "send() nimmt eine leere Nachricht wieder an - dann darf der Knopf auch wieder " +
                "immer antippbar sein, und diese Regel weg.",
            "if (body.isBlank()) return" in senden,
        )
    }

    @Test
    fun `der Senden-Knopf ist ohne Text kein Knopf`() {
        val knopf = Quelltext.cut(quelle, "val knopf = @Composable {", "\n        }")
        assertTrue(
            "Der Senden-Knopf bleibt antippbar, obwohl das Senden bei leerem Text nichts " +
                "tut: $knopf",
            "onClick = if (draft.isBlank()) {" in knopf,
        )
        assertTrue(
            "Der Knopf sieht ohne Text weiter aus wie einer - volle Akzentfarbe, aber " +
                "ohne Wirkung.",
            "draft.isBlank() -> palette.surfaceDefault" in knopf,
        )
    }

    /**
     * Und dieselbe Zeile auf der Waehltastatur.
     *
     * `dial` weigert sich bei etwas, das keine Nummer ist. Der Anrufen-Knopf stand
     * trotzdem in voller Akzentfarbe da - auf dem Bildschirm, auf den man sich am meisten
     * verlaesst.
     */
    @Test
    fun `der Anrufen-Knopf ist ohne Nummer kein Knopf`() {
        val waehler = Quelltext.withoutComments("org/biglau/phone/DialerActivity.kt")
        val waehlen = Quelltext.cut(waehler, "private fun dial(", "\n    }")
        assertTrue(
            "dial() nimmt wieder alles an - dann darf der Knopf auch wieder immer " +
                "antippbar sein, und diese Regel weg.",
            "if (!PhoneNumbers.isDialable(number)) return" in waehlen,
        )
        val tastatur = Quelltext.cut(waehler, "private fun Keypad(", "\nprivate fun ")
        assertTrue(
            "Der Anrufen-Knopf bleibt antippbar, obwohl das Waehlen ohne Nummer nichts tut.",
            "onClick = if (waehlbar) onCall else null" in tastatur,
        )
        assertTrue(
            "Er sieht ohne Nummer weiter aus wie ein Knopf.",
            "if (waehlbar) palette.surfaceAccent else palette.surfaceDefault" in tastatur,
        )
    }
}
