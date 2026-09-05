package org.biglau.widgets

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Satz, wenn ein Widget nicht passt, sagt was **ist** und nennt den Weg, den es gibt.
 *
 * Das Widget wird auf die Kachel geschrieben, bevor der Platz geprueft wird - und das ist
 * so gewollt: die Kennung ist da schon vergeben, unter Umstaenden hat der Nutzer im System
 * gerade eine Erlaubnis erteilt, und das wegzuwerfen waere schlimmer als eine zu kleine
 * Kachel. Der Satz sagte aber „Leeren Sie zuerst eine Nachbarkachel", als waere noch nichts
 * passiert; in Wirklichkeit liegt das Widget schon da und ist gestaucht.
 *
 * Und der Weg heraus steht eine Zeile weiter im selben Editor: „Groesse aendern". Die Regel
 * prueft den Zusammenhang, nicht den Wortlaut - wer die Zeile umbenennt, muss den Satz
 * mitnehmen.
 */
class WidgetPlatzTest {

    @Test
    fun `der Satz nennt die Zeile, die das Problem loest`() {
        listOf("values", "values-de").forEach { sprache ->
            val satz = Quelltext.textValue("widget_no_room", sprache)
            val zeile = Quelltext.textValue("editor_resize", sprache)
            assertTrue(
                "$sprache: der Satz nennt nicht die Zeile \"$zeile\", mit der man die " +
                    "Kachel groesser macht: $satz",
                zeile in satz,
            )
        }
    }

    @Test
    fun `der Satz behauptet nicht, es sei noch nichts geschehen`() {
        val editor = Quelltext.withoutComments("org/biglau/tiles/TileEditorActivity.kt")
        val stelle = Quelltext.cut(editor, "fun finishWidget(", "pendingWidget = null")
        assertTrue(
            "Die Kachel wird nicht mehr vor der Platzpruefung beschrieben - dann darf der " +
                "Satz wieder sagen, es sei noch nichts passiert, und diese Regel weg.",
            stelle.indexOf("write(next)") in 0 until stelle.indexOf("widget_no_room"),
        )
        listOf("values", "values-de").forEach { sprache ->
            val satz = Quelltext.textValue("widget_no_room", sprache)
            assertTrue(
                "$sprache: der Satz besteht aus einem Halbsatz - er muss sagen, was ist, " +
                    "und was zu tun bleibt: $satz",
                satz.count { it == '.' } >= 2,
            )
        }
    }
}
