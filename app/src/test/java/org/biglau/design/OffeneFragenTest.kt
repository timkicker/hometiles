package org.biglau.design

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Eine offene Frage, die längst beantwortet ist, kostet mehr als keine.
 *
 * `PLAN.md` 9 führte am 3.9.2026 zwei Fragen als offen, die es nicht mehr waren: die
 * SIM-Karte („steckt gerade keine im Gerät" — sie steckt seit dem 01.09. drin) und die Lizenz
 * („Vorschlag GPL-3.0-or-later. Einverstanden?" — die `LICENSE` liegt seit Tagen im
 * Wurzelverzeichnis, das README nennt sie). Wer so eine Liste liest, hält eine erledigte
 * Sache für eine Hausaufgabe und fragt noch einmal nach.
 *
 * Diese Regel prüft die Lizenzfrage, weil sie sich am Repository festmachen lässt: gibt es
 * eine `LICENSE`, darf die Frage nicht mehr als offen dastehen.
 */
class OffeneFragenTest {

    private val plan = File("../PLAN.md").readText()

    /**
     * Bis zur naechsten Ueberschrift, nicht bis zum Dateiende.
     *
     * Ohne Endmarke reichte der Ausschnitt bis ans Ende von `PLAN.md` und nahm alles mit, was
     * danach kommt. Solange in Abschnitt 10 keine nummerierte Liste stand, fiel das nicht
     * auf; am 04.09.2026 kam eine dazu (die Reihenfolge der Arbeit in 10.3.7), und die Regel
     * meldete Fragen als falsch nummeriert, die gar keine Fragen sind.
     *
     * Ein Ausschnitt ohne Ende ist kein Ausschnitt. Die Regel hat immer den ganzen Rest des
     * Dokuments gemessen und nur zufaellig nichts gefunden.
     */
    private val fragen = Quelltext.cut(plan, "## 9. Offene Fragen", "## 10.")

    @Test
    fun `die Lizenzfrage steht nicht mehr offen`() {
        val lizenz = File("../LICENSE")
        assertTrue("Es gibt keine LICENSE mehr - dann darf die Frage wieder offen sein.", lizenz.isFile)
        assertTrue(
            "Die LICENSE ist nicht die GPL - dann stimmt der Eintrag in PLAN.md 9 nicht mehr.",
            "GNU GENERAL PUBLIC LICENSE" in lizenz.readText().take(200),
        )
        val zeile = fragen.lineSequence().firstOrNull { it.contains("**Lizenz") || it.contains("~~Lizenz~~") }
            ?: throw AssertionError("Die Lizenzzeile fehlt in PLAN.md 9")
        assertTrue(
            "PLAN.md 9 fragt noch nach der Lizenz, obwohl die LICENSE im Repository liegt " +
                "und das README sie nennt: $zeile",
            zeile.startsWith("4. ~~Lizenz~~"),
        )
    }

    /** Und die Nummern laufen durch - zwei Fünfer waren es bis zum 3.9.2026. */
    @Test
    fun `die Fragen sind fortlaufend nummeriert`() {
        val nummern = Regex("""^(\d+)\. """, RegexOption.MULTILINE)
            .findAll(fragen).map { it.groupValues[1].toInt() }.toList()
        assertTrue("keine nummerierten Fragen gefunden", nummern.size >= 4)
        assertEquals(
            "Die Fragen in PLAN.md 9 sind nicht fortlaufend nummeriert - eine Nummer " +
                "zweimal zu vergeben macht das Verweisen darauf unmöglich.",
            (1..nummern.size).toList(),
            nummern,
        )
    }
}
