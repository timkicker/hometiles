package org.biglau.tiles

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wer ein Widget wählt, erfährt vorher, dass der lange Druck danach woanders hinführt.
 *
 * Auf jeder anderen Kachel öffnet ein langer Druck den Kachel-Editor. Auf einer
 * Widget-Kachel nicht: das Widget bekommt die Berührung zuerst. Am 04.09.2026 am Emulator
 * nachgestellt — Analoguhr auf eine Kachel gelegt, lang gedrückt, und die Weckerapp ging
 * auf. Der übliche Weg zum Editor ist für diese eine Kachel zu.
 *
 * Es gibt einen anderen (Einstellungen → „Kacheln ändern", im Bearbeitungsmodus reicht ein
 * kurzer Tipp — auch am Emulator nachgeprüft, er führt auf die richtige Kachel). Nur weiss
 * das niemand, der es nicht schon weiss. Der Satz steht deshalb **vor** der Wahl, in der
 * Widget-Liste, und nicht als Trost hinterher.
 */
class WidgetLangdruckTest {

    @Test
    fun `die widget-liste warnt vor dem langen druck`() {
        val liste = Quelltext.cut(
            Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt"),
            from = "private fun WidgetPicker(",
            to = "\n}",
        )
        assertTrue(
            "Die Widget-Auswahl sagt nicht, dass der lange Druck danach das Widget " +
                "oeffnet statt des Editors:\n$liste",
            "widget_long_press_hint" in liste,
        )
    }

    @Test
    fun `der hinweis nennt den anderen weg`() {
        listOf("values-de", "values").forEach { sprache ->
            val text = Quelltext.textValue("widget_long_press_hint", sprache)
            assertTrue(
                "Der Hinweis nennt keinen Ausweg (Sprache \"$sprache\"): $text",
                "Kacheln ändern" in text || "Change the tiles" in text,
            )
        }
    }
}
