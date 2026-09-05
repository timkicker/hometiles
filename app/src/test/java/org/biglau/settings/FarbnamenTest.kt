package org.biglau.settings

import org.biglau.Quelltext
import org.biglau.data.ThemeName
import org.biglau.ui.theme.ScreenBackground
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fünf Farben, fünf Namen — und die Namen kommen auch an.
 *
 * Die Auswahl des Screen-Hintergrunds zeigt jede Farbe in voller Breite; die Zeile heisst
 * überall „Diese Farbe". Für das Auge ist das die bessere Auskunft, und so steht es auch im
 * Quelltext begründet. Wer die Farbe **nicht** sieht, hatte bis zum 04.09.2026 fünf gleiche
 * Angebote vor sich — am Emulator im Knotenabzug nachgesehen, fünfmal derselbe Text ohne
 * jede Beschreibung.
 *
 * Gesprochen wird jetzt der Name der Farbe. Zwei Dinge müssen dafür stimmen: es muss für
 * jede angebotene Farbe einen Namen geben, und die Namen müssen sich unterscheiden — eine
 * Liste mit fünfmal „Farbe" wäre dasselbe Problem mit mehr Aufwand.
 */
class FarbnamenTest {

    private val schluessel = listOf(
        "screen_background_blue",
        "screen_background_violet",
        "screen_background_green",
        "screen_background_red",
        "screen_background_ochre",
    )

    @Test
    fun `fuer jede angebotene farbe gibt es einen namen`() {
        val farben = ScreenBackground.choicesFor(ThemeName.DARK, false)
        assertEquals(
            "So viele Farben werden angeboten, so viele Namen muss es geben",
            farben.size,
            HINTERGRUND_NAMEN.size,
        )
        assertEquals(
            "Die Liste im Quelltext und die Schluessel dieser Regel muessen zusammenpassen",
            schluessel.size,
            HINTERGRUND_NAMEN.size,
        )
    }

    @Test
    fun `jeder name steht in beiden sprachen und ist einmalig`() {
        listOf("values-de", "values").forEach { sprache ->
            val namen = schluessel.map { Quelltext.textValue(it, sprache) }
            namen.forEach { name ->
                assertTrue("Ein Farbname ist leer (Sprache \"$sprache\")", name.isNotBlank())
            }
            assertEquals(
                "Zwei Farben heissen gleich (Sprache \"$sprache\"): $namen",
                namen.size,
                namen.toSet().size,
            )
        }
    }

    @Test
    fun `die farbzeile reicht den gesprochenen namen weiter`() {
        val liste = Quelltext.cut(
            Quelltext.withoutComments("org/biglau/settings/SettingsActivity.kt"),
            from = "itemsIndexed(backgroundColours)",
            to = "\n        }",
        )
        assertTrue(
            "Die Farbzeilen sagen ihren Namen nicht: $liste",
            "labelSpeech" in liste && "HINTERGRUND_NAMEN" in liste,
        )
    }
}
