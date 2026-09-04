package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jede Flaeche, die sich ueber den Startbildschirm legt, holt sich den Fokus.
 *
 * PLAN.md 10.3.5. Die Ueberlagerungen liegen im **selben Fenster** wie der Startbildschirm,
 * als Geschwister darueber. Beim Start eines Fensters sucht Compose von selbst ein erstes
 * Fokusziel; wenn eine Flaeche spaeter dazukommt, tut es das nicht. Der Fokus bleibt, wo er
 * war, oder er ist weg.
 *
 * Und weg heisst hier tot: `onPreviewKeyEvent` laeuft nur den Weg vom fokussierten Knoten
 * zur Wurzel. Ohne Fokus laeuft kein Tastenhandler an, also bewegt sich nichts, also kommt
 * der Fokus nie zurueck. Ein Bildschirm ohne Fokus ist an einem Tastentelefon dasselbe wie
 * ein eingefrorener.
 *
 * Am 04.09.2026 zweimal an einem Abend gefunden, beide Male von `tools/unerreichbar.py` und
 * beide Male vorher von Hand uebersehen:
 *
 * * Der offene Ordner: neun anklickbare Flaechen, **null** je fokussiert.
 * * Die grosse Kachelbeschriftung: eine Flaeche, **null** erreicht. Dort stand obendrein
 *   `Zum Schliessen tippen` - auf einem Bildschirm, der fuer den gebaut ist, der die
 *   Beschriftung sonst nicht lesen kann.
 *
 * Deshalb diese Regel, und sie **zaehlt**: jede Ueberlagerung in `MainActivity` muss hier
 * genannt sein, mitsamt der Stelle, an der sie den Fokus anfordert. Eine neue laesst sie
 * umfallen und stellt die Frage, bevor jemand sie am Geraet stellt.
 */
class UeberlagerungFokusTest {

    private val start = Quelltext.ohneKommentare("org/biglau/MainActivity.kt")

    /**
     * Die gesperrte App zeichnet `PinGate`, und das liegt in `:core:ui`. Die Marke steht
     * also nicht in `MainActivity` - gesucht wird in beiden Dateien.
     */
    private val sperre = Quelltext.ohneKommentare("org/biglau/ui/PinGate.kt")

    private val quellen get() = start + sperre

    /**
     * Was sich ueber den Startbildschirm legt, und woran man im Quelltext sieht, dass es den
     * Fokus holt.
     *
     * Die Liste steht neben `verdeckt` in `MainActivity`: dieselben Flaechen, dieselbe
     * Frage. `VerdecktTest` haelt fest, dass keine fehlt.
     */
    private val ueberlagerungen = mapOf(
        "ordner" to "aktiv = obenauf",
        "kachelMenue" to "anker.first().requestFocus()",
        "label" to "anker.requestFocus()",
        // In `verdeckt` heisst sie `asking`, im Quelltext `ContactChoice`.
        "asking" to "anker.first().requestFocus()",
        // Die Tastatur holt den Fokus selbst, siehe BigKeypad und TastaturfolgeTest.
        "wartend" to "holtFokus = true",
        "phoneStateAsked" to "anker.first().requestFocus()",
    )

    /**
     * Nichts mehr offen.
     *
     * Diese Liste hat am 04.09.2026 einen Abend lang etwas enthalten, und das war ihr
     * Zweck: sie hat die beiden Ueberlagerungen benannt, von denen noch niemand wusste, wie
     * sie sich mit Tasten verhalten. Beide waren unbedienbar, beide sind es nicht mehr.
     * Leer bleibt sie nur, solange keine neue dazukommt.
     */
    private val ungemessen = emptyList<String>()

    @Test
    fun `jede Ueberlagerung ist bedacht`() {
        val bedingung = Quelltext.ausschnitt(start, "val verdeckt = ", "Column(")
        val genannt = ueberlagerungen.keys + ungemessen
        val fehlend = genannt.filterNot { it in bedingung }
        assertTrue("Die Liste ist leer, dann misst diese Regel nichts", genannt.size >= 5)
        assertEquals(
            "Diese Namen stehen hier, aber nicht mehr in der Bedingung `verdeckt`. " +
                "Entweder heisst die Ueberlagerung jetzt anders, oder es gibt sie nicht " +
                "mehr - so oder so misst die Regel dann die falsche Sache: $fehlend",
            emptyList<String>(),
            fehlend,
        )
        // Und die Gegenrichtung: nichts in `verdeckt`, das hier fehlt.
        val inBedingung = Regex("""(\w+)(?:\.value)? != null|(\w+)\.value \|\|""")
            .findAll(bedingung)
            .map { it.groupValues.drop(1).first { wert -> wert.isNotEmpty() } }
            .toSet()
        assertEquals(
            "Diese Ueberlagerungen stehen in `verdeckt`, aber nicht in dieser Regel. Fuer " +
                "jede gehoert die Frage beantwortet, ob sie den Fokus holt.",
            emptyList<String>(),
            inBedingung.filterNot { it in genannt },
        )
    }

    @Test
    fun `wer gemessen ist, holt sich den Fokus`() {
        assertTrue("keine Ueberlagerung genannt", ueberlagerungen.isNotEmpty())
        ueberlagerungen.forEach { (name, marke) ->
            assertTrue(
                "Die Ueberlagerung `$name` fordert den Fokus nicht mehr an ($marke fehlt). " +
                    "Dann bleibt sie mit Tasten unbedienbar, und keine Taste holt sie zurueck.",
                marke in quellen,
            )
        }
    }

    /**
     * Wer den Fokus nimmt, uebernimmt auch den Ausweg.
     *
     * Die Kehrseite, teuer gelernt am 04.09.2026: die Kontaktwahl liess sich mit Tasten
     * nicht bedienen, und der einzige Ausweg war die Zurueck-Taste ueber den `BackHandler`
     * des Startbildschirms. Kaum forderte sie den Fokus an, kam die Taste dort nicht mehr
     * an - dreimal am Emulator nachgestellt: ohne Fokus schloss sie, mit Fokus blieb die
     * Frage stehen. Ein eigener `BackHandler` half auch nicht; die Taste wird schon im
     * Fokusbaum verbraucht und erreicht den Verteiler nie.
     *
     * Aus einer Ueberlagerung bedienbar zu machen und ihren Ausweg zu vergessen ist
     * schlimmer als beides zu lassen: vorher kam man wenigstens heraus.
     */
    @Test
    fun `die Kontaktwahl haelt ihren Ausweg selbst`() {
        val wahl = Quelltext.ausschnitt(start, "private fun ContactChoice(", "private fun FolderOverlay(")
        assertTrue(
            "Die Kontaktwahl behandelt die Zurueck-Taste nicht mehr selbst. Sobald etwas " +
                "darin den Fokus hat, kommt sie am BackHandler des Startbildschirms nicht " +
                "mehr an, und die Frage bleibt stehen.",
            Regex("""Key\.Back ->[\s\S]{0,80}onDismiss\(\)""").containsMatchIn(wahl),
        )
        assertTrue(
            "Sie fordert den Fokus nicht mehr an, dann ist sie mit Tasten nicht bedienbar.",
            "anker.first().requestFocus()" in wahl,
        )
        listOf("DirectionUp", "DirectionDown").forEach {
            assertTrue(
                "Key.$it fehlt: zwischen Anrufen und Schreiben kommt man dann nicht.",
                "Key.$it" in wahl,
            )
        }
    }

    /**
     * Die grosse Beschriftung hat genau eine Handlung, deshalb schliesst sie jede Taste - und
     * ihr Hinweis sagt das auch. `tap_to_close` ist der Text der Kontaktwahl und gilt dort
     * weiter; die beiden Bildschirme haben verschiedene Antworten auf einen Tastendruck und
     * duerfen sich deshalb keinen Satz teilen.
     */
    @Test
    fun `die grosse Beschriftung sagt, dass jede Taste schliesst`() {
        val popup = Quelltext.ausschnitt(start, "private fun LabelPopup(", "private fun ContactChoice(")
        assertTrue(
            "Die grosse Beschriftung nennt nicht mehr den Text, der die Taste erwaehnt.",
            "R.string.popup_close_any_key" in popup,
        )
        assertTrue(
            "Sie schliesst nicht mehr auf jeden Tastendruck.",
            Regex("""KeyEventType\.KeyDown[\s\S]{0,120}onDismiss\(\)""").containsMatchIn(popup),
        )
        assertTrue(
            "Die Kontaktwahl hat den Satz der Beschriftung uebernommen. Dort ist er falsch: " +
                "wer dort eine Taste drueckt, waehlt zwischen Anrufen und Schreiben.",
            "R.string.tap_to_close" !in popup,
        )
    }
}
