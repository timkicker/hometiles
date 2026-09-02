package org.biglau.res

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

    private val strings = File("src/main/res/values/strings.xml")

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
        "a11y_signal_bars" to "Maß: gefüllte von vier Stufen, wie 2 × 3 Felder",
    )


    private fun mitZahl(): List<String> =
        Regex("""<string name="([^"]+)">([^<]*%\d\${'$'}d[^<]*)</string>""")
            .findAll(strings.readText())
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

    @Test
    fun `die Regel wuerde einen zaehlenden Text finden`() {
        // Gegenprobe: ohne sie winkte ein kaputter Suchausdruck alles durch.
        val probe = """<string name="test_zaehler">%1${'$'}d Einträge</string>"""
        val treffer = Regex("""<string name="([^"]+)">([^<]*%\d\${'$'}d[^<]*)</string>""").find(probe)
        assertTrue("ein zählender Text muss auffallen", treffer != null)
    }
}
