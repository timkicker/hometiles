package org.biglau.sms

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bei offener Tastatur bleibt Platz fuer das, was man dann braucht.
 *
 * Auf drei Zoll bleiben ueber der Tastatur rund 290 Bildpunkte. Bei 200 % Systemschrift
 * brauchen Ueberschrift (63), Eingabefeld (92) und Senden-Zeile (90) zusammen mehr - am
 * 04.09.2026 am Jelly 2 gemessen: der Senden-Knopf war zur Haelfte von der Tastatur
 * verdeckt, das Wort nicht mehr zu lesen. Wer die grosse Schrift eingestellt hat, ist genau
 * der, der sie braucht.
 *
 * Von den drei Zeilen ist die Ueberschrift die entbehrlichste: mit wem man schreibt, hat man
 * gerade selbst ausgewaehlt. Nach der Aenderung standen Tagesstempel, Feld und die ganze
 * Senden-Zeile ueber der Tastatur.
 *
 * Die Einstellung „Knopf ueber dem Text" hilft auch, aber sie ist eine Vorliebe und keine
 * Rettung - wer sie nicht kennt, sieht nur einen halben Knopf.
 */
class TastaturPlatzTest {

    private val quelle = Quelltext.ohneKommentare("org/biglau/sms/SmsActivity.kt")

    @Test
    fun `die Unterhaltung laesst die Ueberschrift weg, solange getippt wird`() {
        // Conversation ist die letzte Funktion der Datei - es gibt keine naechste als
        // Grenze, also bis zum Ende.
        val gespraech = Quelltext.ausschnitt(quelle, "private fun Conversation(")
        assertTrue(
            "Die Unterhaltung fragt nicht, ob die Tastatur offen ist - dann steht die " +
                "Ueberschrift auch dann da, wenn der Platz fuer den Senden-Knopf fehlt.",
            "WindowInsets.ime.getBottom(" in gespraech,
        )
        assertTrue(
            "Die Ueberschrift steht unbedingt da: $gespraech",
            "if (!tastaturOffen) BigHeading(title)" in gespraech,
        )
    }
}
