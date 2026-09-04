package org.biglau.design

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keine Gedankenstriche in den eigenen Texten, nicht nur in denen der App.
 *
 * [org.biglau.res.StricheTest] haelt die Bildschirmtexte frei. Der Nutzer hat die Ansage am
 * 04.09.2026 aber fuer alles Geschriebene gemacht, und `PLAN.md` trug an dem Tag noch 141
 * lange Striche, 15 kurze und 29 Mittelpunkte. Wer eine Regel fuer die App aufstellt und sie
 * selbst nicht befolgt, hat keine Regel, sondern eine Meinung ueber andere.
 *
 * Alle vier Dateien sind durch und bleiben bei null, `STATUS.md` eingeschlossen.
 *
 * Die Chronik war der grosse Rest: 1470 Striche in 361 alten Eintraegen. Hier stand
 * deshalb eine Weile eine Sperrklinke mit genau dieser Zahl, mit dem Argument, das
 * Umschreiben lohne den Aufwand nicht und 146 der Striche stuenden ohnehin in Zitaten alter
 * Bildschirmtexte. Der Nutzer hat am 04.09.2026 entschieden: weg damit.
 *
 * Umgeschrieben hat es `tools/entstrichen.py`, nach Muster und nicht Zeichen gegen Zeichen:
 * Klammern fuer den doppelten Einschub, Komma vor einer Konjunktion, Doppelpunkt vor einer
 * Aufzaehlung oder einem Codeblock und in Ueberschriften, sonst ein Punkt. Nachgesehen
 * wurde an einer Stichprobe, nicht an jedem der 1470 Faelle; ein Teil liest sich als
 * Ellipse statt als ganzer Satz. Das ist ein Arbeitstagebuch, kein Bildschirmtext.
 */
class ProsaStricheTest {

    private val verboten = mapOf(
        '—' to "langer Gedankenstrich",
        '–' to "kurzer Gedankenstrich",
        '·' to "Mittelpunkt",
    )

    private fun datei(name: String) = File("../$name")

    @Test
    fun `Plan und Readme tragen keinen Strich`() {
        val treffer = listOf("PLAN.md", "README.md", "tools/README.md", "STATUS.md").flatMap { name ->
            val inhalt = datei(name).readText()
            verboten.entries.filter { (z, _) -> z in inhalt }.map { (z, wie) ->
                "$name: $wie, ${inhalt.count { it == z }}mal"
            }
        }
        assertEquals(
            "Ersatz ist ein Komma, ein Doppelpunkt, ein Punkt oder eine zweite Zeile. Ein " +
                "Zahlenbereich nimmt den Bindestrich",
            emptyList<String>(),
            treffer,
        )
    }

    @Test
    fun `die Regel wuerde einen Strich finden`() {
        // Gegenprobe: sonst haette ein leerer Zeichenvorrat alles durchgewunken.
        assertTrue(verboten.keys.any { it in "Ein Satz — mit Strich" })
    }
}
