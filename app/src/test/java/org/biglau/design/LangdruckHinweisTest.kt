package org.biglau.design

import org.biglau.Quelltext
import org.biglau.a11y.LongPress
import org.biglau.a11y.LongPressAction
import org.biglau.data.Accessibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wer den Langdruck belegt, verliert damit den Weg zum Editor - und erfaehrt es.
 *
 * `LongPress.decide` gibt einer Zweitbelegung den Vorrang vor allem anderen, mit Grund:
 * „Eine Zweitbelegung ist eine Entscheidung fuer genau diese Kachel und geht deshalb den
 * allgemeinen Vorgaben vor." Daraus folgt aber etwas, das im Editor niemand sagte: fuer
 * **diese** Kachel oeffnet der Langdruck den Editor nicht mehr.
 *
 * Dieselbe Sackgasse kennt die App laengst - wenn eine **Einstellung** den Langdruck nimmt,
 * steht `a11y_editor_moved` da. Nimmt ihn die Kachel selbst, stand bis zum 04.09.2026
 * nichts.
 *
 * Und es trifft nicht selten: wer eine Zweitbelegung setzt, will die Kachel bearbeiten -
 * genau das, was danach nicht mehr auf dem gewohnten Weg geht.
 */
class LangdruckHinweisTest {

    private val editor = Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt")

    /** Erst der Grund: eine Zweitbelegung nimmt dem Langdruck den Editor wirklich weg. */
    @Test
    fun `eine Zweitbelegung geht dem Editor vor`() {
        assertEquals(
            "Eine Zweitbelegung oeffnet wieder den Editor - dann ist der Hinweis falsch " +
                "und gehoert weg.",
            listOf(LongPressAction.SECOND_ACTION),
            LongPress.decide(Accessibility(), editMode = false, hasSecondAction = true),
        )
        assertEquals(
            "Ohne Zweitbelegung soll der Langdruck weiter zum Editor fuehren.",
            listOf(LongPressAction.EDIT),
            LongPress.decide(Accessibility(), editMode = false, hasSecondAction = false),
        )
    }

    /** Und dann der Hinweis, genau dort, wo die Belegung steht. */
    @Test
    fun `der Editor sagt es, sobald eine Zweitbelegung da ist`() {
        val ab = editor.indexOf("if (button.longPress != null) {")
        assertTrue("Der Zweig fuer eine gesetzte Zweitbelegung ist weg", ab > 0)
        val zweig = editor.substring(ab, minOf(editor.length, ab + 900))
        assertTrue(
            "Der Editor sagt nicht, dass der Langdruck ihn nicht mehr oeffnet. Wer die " +
                "Kachel spaeter aendern will, haelt sie gedrueckt und loest die " +
                "Zweitbelegung aus - im schlimmsten Fall einen Anruf.",
            "R.string.editor_long_press_takes_editor" in zweig,
        )
    }

    /** Und er sagt es **nur** dann - sonst waere es eine Warnung ohne Anlass. */
    @Test
    fun `ohne Zweitbelegung steht der Hinweis nicht da`() {
        val stellen = Regex("""R\.string\.editor_long_press_takes_editor""")
            .findAll(editor).map { it.range.first }.toList()
        assertEquals(
            "Der Hinweis soll genau einmal dastehen - keinmal waere keine Auskunft, "  +
                "zweimal waere Laerm.",
            1,
            stellen.size,
        )
        val ab = editor.indexOf("if (button.longPress != null) {")
        assertTrue(
            "Der Hinweis steht ausserhalb des Zweiges fuer eine gesetzte Zweitbelegung - " +
                "dann warnt er auch, wenn es nichts zu warnen gibt.",
            stellen.first() > ab,
        )
    }
}
