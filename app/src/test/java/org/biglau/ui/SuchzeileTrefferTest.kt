package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein Tipp irgendwo in die Suchzeile setzt die Schreibmarke.
 *
 * Die gezeichnete Zeile ist 64 dp hoch, das Eingabefeld darin nur 48 - und sobald die
 * Trefferzahl darunter steht, sogar 38. Am 04.09.2026 am Jelly 2 gemessen: ein Tipp auf die
 * oberen vierzehn Bildpunkte der Zeile tat gar nichts (`mInputShown` blieb `false`), obwohl
 * dort ein Feld gezeichnet ist.
 *
 * Das ist der stille Fehlgriff: es sieht aus wie ein Feld, man tippt darauf, und nichts
 * geschieht. Die Hand, fuer die BigLau gebaut ist, trifft den Rand regelmaessig.
 */
class SuchzeileTrefferTest {

    private val feld = Quelltext.withoutComments("org/biglau/ui/BigSearchField.kt")

    @Test
    fun `die ganze Zeile nimmt den Tipp an`() {
        assertTrue(
            "Die Suchzeile reicht den Tipp nicht an das Feld weiter - dann tut ein Tipp " +
                "auf ihren Rand nichts.",
            "detectTapGestures { caret.requestFocus() }" in feld,
        )
        assertTrue(
            "Das Eingabefeld nimmt die Schreibmarke nicht entgegen.",
            "focusRequester(caret)" in feld,
        )
    }

    /**
     * Und die Zeile bleibt ein Feld, kein Knopf.
     *
     * `clickable` wuerde sie fuer die Vorlesefunktion zu einer Schaltflaeche machen -
     * `BigRow` sagt in seiner eigenen Beschreibung, warum das schlecht ist.
     */
    @Test
    fun `die Zeile wird dadurch nicht zur Schaltflaeche`() {
        val zeile = Quelltext.cut(feld, "Row(", "Icon(")
        assertTrue(
            "Die Suchzeile ist anklickbar geworden - dann sagt die Vorlesefunktion " +
                "\"Schaltflaeche\", obwohl dort ein Eingabefeld steht: $zeile",
            ".clickable" !in zeile,
        )
    }
}
