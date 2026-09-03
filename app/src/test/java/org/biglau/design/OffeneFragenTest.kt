package org.biglau.design

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
    private val fragen = plan.substringAfter("## 9. Offene Fragen")

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
