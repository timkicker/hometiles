package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Blätterknopf hat eine Untergrenze, keine feste Grösse.
 *
 * Zwei Dinge gingen am 04.09.2026 am Emulator schief, und beide hingen an derselben Zeile —
 * `Modifier.height(56.dp).then(modifier)`:
 *
 * 1. Der Kommentar daneben sagte „Hoehe zuerst, damit ein Aufrufer sie ueberschreiben
 *    kann". Es ist umgekehrt: eine feste Grösse **vor** dem Modifier des Aufrufers begrenzt
 *    ihn. Der Assistent bat um 72 dp und bekam 56 — nachgemessen, 77 statt 99 Bildpunkte.
 * 2. Eine Breite stand gar nicht da. Der Knopf holte sie sich vom Symbol, und in der engen
 *    Zeile des Assistenten blieben davon **40,7 dp** übrig, angefangen bei x=1 — also
 *    ausserhalb der Spaltenfüllung, unter dem Mindestmass von 48 dp für einen Fingertipp.
 *
 * `heightIn`/`widthIn` sind Untergrenzen und tun, was der alte Kommentar versprach: wer
 * nichts sagt, bekommt 56 dp hoch; wer etwas sagt, bekommt es; schmaler als 48 dp wird es
 * nie.
 *
 * Der erste Anlauf gab dem Knopf im Assistenten 72 dp Breite — und bei 200 % Systemschrift
 * brach „Weiter" daneben mitten im Wort um („Weite/r"). Am Gerät gesehen, beide Male: das
 * Mindestmass reicht, mehr nimmt der Nachbarzeile den Platz.
 */
class BlaetterknopfTest {

    private val knopf = Quelltext.ausschnitt(
        Quelltext.ohneKommentare("org/biglau/ui/ScrollButtons.kt"),
        von = "private fun PageButton(",
        bis = "\n}",
    )

    @Test
    fun `der aufrufer kommt vor den untergrenzen`() {
        val kette = Quelltext.ausschnitt(knopf, von = "Box(", bis = "contentAlignment")
        val aufrufer = kette.indexOf(".then(modifier)")
        assertTrue("PageButton reicht den Modifier des Aufrufers nicht durch", aufrufer >= 0)
        val fest = Regex("""\.(height|width|size)\(""").find(kette)
        assertTrue(
            "Vor `.then(modifier)` steht eine feste Groesse - dann kann der Aufrufer sie " +
                "nicht mehr ueberschreiben, sondern wird von ihr begrenzt: " +
                (fest?.value ?: ""),
            fest == null || fest.range.first > aufrufer,
        )
    }

    @Test
    fun `der knopf ist nie schmaler als ein finger`() {
        assertTrue(
            "PageButton hat keine Mindestbreite. Ohne sie nimmt er die Breite seines " +
                "Symbols, und eine enge Zeile drueckt ihn darunter - im Assistenten waren " +
                "es 40,7 dp.",
            "widthIn(min = 48.dp)" in knopf,
        )
        assertTrue(
            "PageButton hat keine Mindesthoehe mehr - dann steht ein Aufrufer ohne Angabe " +
                "vor einem Knopf in Symbolgroesse.",
            "heightIn(min = 56.dp)" in knopf,
        )
    }
}
