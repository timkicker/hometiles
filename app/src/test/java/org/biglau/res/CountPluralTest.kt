package org.biglau.res

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein Text, der etwas zählt, ist ein Plural.
 *
 * Anlass: „1 Einträge gelöscht" in der Anrufliste, gefunden am Bildschirm. Zwei Runden
 * später stand „Costs 1 text messages" unter dem Nachrichtenfeld — derselbe Fehler an einer
 * Stelle, die niemand nachgezählt hatte. Beim zweiten Mal von Hand zu finden ist einmal zu
 * oft.
 *
 * Nicht jede Zahl zählt etwas: Maße („2 × 3 Felder"), Positionen („Schritt 2 von 5") und
 * Namen („Screen 3") tragen keine Mehrzahl. Die stehen unten mit Grund — eine Ausnahme ohne
 * Grund ist bloß ein leiser gestellter Fehler.
 */
class CountPluralTest {

    /**
     * **Alle** englischen Textdateien, nicht die erste.
     *
     * Bis zum 3.9.2026 stand hier `.first()`. Solange es nur `:app` gab, war das dasselbe;
     * seit `core:ui` eigene Texte hat, war es das nicht mehr — und ein zählender Text in
     * einem anderen Modul wäre stillschweigend ungeprüft geblieben. Genau der Grund, aus dem
     * es `Quelltext` gibt.
     *
     * Und seit dem 4.9.2026 **beide Sprachen**. Bis dahin las die Regel nur `values`, also
     * Englisch — dabei war der Anlass ein deutscher Satz („1 Einträge gelöscht"). Ein Text,
     * der nur auf Deutsch zählt, wäre nie aufgefallen. Heute sind alle zwölf deutschen
     * Zähltexte Ausnahmen mit Grund; die Regel findet also nichts Neues, aber sie sieht ab
     * jetzt dorthin, wo der Fehler herkam.
     */
    private val strings = Quelltext.texte("values") + Quelltext.texte("values-de")

    /**
     * Zahlen, die nichts zählen.
     *
     * Maße und Positionen: „2 × 3 Felder" hat kein Singular-Gegenstück, „Platz 1 von 3"
     * ebenso wenig, und „Screen 1" ist ein Name.
     */
    private val zaehltNicht = mapOf(
        "resize_current" to "Maß: Spalten × Zeilen",
        "screen_grid" to "Maß: Spalten × Zeilen",
        "screen_is_home" to "Maß: Spalten × Zeilen",
        "widget_needs" to "Maß: Spalten × Zeilen",
        "widget_fixed" to "Maß: Spalten × Zeilen",
        "widget_no_room" to "Maß: Spalten × Zeilen",
        "screen_default_name" to "Name: Screen 1, Screen 2",
        "swipe_order_position" to "Position: Platz 2 von 5",
        "wizard_position" to "Position: Schritt 2 von 5",
        "sos_numbers_hint" to "Obergrenze, nie eins: bis zu 5 Menschen",
        "move_spot" to "Position im Raster: Zeile 2, Spalte 1",
        "editor_where" to "Position im Raster, mit Screen davor: Start, Zeile 4, Spalte 1",
        "move_which" to "Position im Raster, mit der Kachel davor: Kontakte, Zeile 2, Spalte 1",
        "a11y_signal_bars" to "Maß: gefüllte von vier Stufen, wie 2 × 3 Felder",
    )


    private fun mitZahl(): List<String> =
        Regex("""<string name="([^"]+)">([^<]*%\d\${'$'}d[^<]*)</string>""")
            .findAll(strings.joinToString("\n") { it.readText() })
            .map { it.groupValues[1] }
            .toList()

    @Test
    fun `jeder zaehlende Text ist ein Plural`() {
        val ungeprueft = mitZahl().filterNot { it in zaehltNicht }
        assertEquals(
            "Diese Texte zählen etwas und stehen trotzdem als einzelner String da — bei " +
                "der Zahl 1 lesen sie sich falsch: $ungeprueft",
            emptyList<String>(),
            ungeprueft,
        )
    }

    @Test
    fun `jede Ausnahme gibt es wirklich und nennt ihren Grund`() {
        val vorhanden = mitZahl().toSet()
        zaehltNicht.forEach { (name, grund) ->
            assertTrue("$name gibt es nicht mehr — Ausnahme streichen", name in vorhanden)
            assertTrue("$name braucht einen Grund", grund.length > 10)
        }
    }

    /**
     * Ein Plural, dessen Einzahl und Mehrzahl derselbe Satz sind, ist keiner.
     *
     * Die Form stimmt dann - `one` und `other` sind da, `TranslationsTest` ist zufrieden -,
     * und am Bildschirm steht trotzdem „1 Einträge". Genau der Fehler, gegen den diese
     * Datei geschrieben wurde, nur eine Ebene tiefer versteckt.
     *
     * Die Regel unterstellt dabei, dass jede Sprache das gezählte Wort beugt. Das stimmt
     * meistens und war am 04.09.2026 beim Italienischen zum ersten Mal knapp: `app` ist dort
     * unveränderlich, `1 app` und `5 app` sind beide richtig. Ausgewichen wurde über
     * `applicazione` und `applicazioni`, also ohne die Regel anzufassen. Kommt eine Sprache
     * dazu, in der sich das nicht umgehen lässt, gehört hier eine Ausnahme mit Grund hin und
     * keine Aufweichung.
     */
    @Test
    fun `kein Plural sagt zweimal dasselbe`() {
        val gleich = Quelltext.alleTexte()
            .filter { it.name == "plurals.xml" }
            .flatMap { datei ->
                Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
                    .findAll(datei.readText())
                    .mapNotNull { treffer ->
                        val formen = Regex("""<item quantity="([^"]+)">(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
                            .findAll(treffer.groupValues[2])
                            .associate { it.groupValues[1] to it.groupValues[2].trim() }
                        val einzahl = formen["one"]
                        val mehrzahl = formen["other"]
                        if (einzahl != null && einzahl == mehrzahl) {
                            datei.parentFile.name + "/" + treffer.groupValues[1] + ": " + einzahl
                        } else {
                            null
                        }
                    }
            }
        assertEquals(
            "Einzahl und Mehrzahl sind hier derselbe Satz - dann ist die Mehrzahlform nur " +
                "Form und der Text liest sich bei 1 weiter falsch",
            emptyList<String>(),
            gleich,
        )
    }

    @Test
    fun `die Regel wuerde einen zaehlenden Text finden`() {
        // Gegenprobe: ohne sie winkte ein kaputter Suchausdruck alles durch.
        val probe = """<string name="test_zaehler">%1${'$'}d Einträge</string>"""
        val treffer = Regex("""<string name="([^"]+)">([^<]*%\d\${'$'}d[^<]*)</string>""").find(probe)
        assertTrue("ein zählender Text muss auffallen", treffer != null)

        // Und dieselbe Gegenprobe fuer den doppelten Plural.
        val doppelt = """<plurals name="test"><item quantity="one">%1${'$'}d Eintraege</item><item quantity="other">%1${'$'}d Eintraege</item></plurals>"""
        val formen = Regex("""<item quantity="([^"]+)">(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(doppelt)
            .associate { it.groupValues[1] to it.groupValues[2].trim() }
        assertEquals("die Gegenprobe muss zwei gleiche Formen sehen", formen["one"], formen["other"])
    }
}
