package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Zifferntastatur fuehrt den Fokus selbst, statt ihn suchen zu lassen.
 *
 * PLAN.md 10.3.5. Als Bildschirm einer Activity braucht sie das nicht: das Fenster bekommt
 * den Fokus, Compose sucht das erste Ziel und findet danach von selbst weiter. Als
 * **Ueberlagerung** ueber dem Startbildschirm nicht - dort liegt darunter noch ein
 * Bildschirm, und die Suche nach dem naechsten Ziel laeuft dorthin.
 *
 * Am 04.09.2026 gemessen, gesperrte App angetippt: elf anklickbare Flaechen, **null**
 * erreicht. Die ganze Tastatur war unerreichbar, und damit jede gesperrte App. Wer die
 * App-Sperre einschaltet und mit Tasten arbeitet, kam an keine davon mehr heran.
 *
 * Ein `moveFocus` in die Richtung der Taste war der erste Versuch und half nicht: am Rand
 * der Tastatur verliess es die Ueberlagerung, und danach war der Fokus ganz weg. Dieselbe
 * Regel wie im Rasterrahmen und beim Streifen darunter: **kein blindes moveFocus, sondern
 * benannte Anker.**
 *
 * Nachgemessen: gesperrte App 11 von 11, PIN-Sperre vor dem Editor unveraendert 11 von 11 -
 * der Umbau hat den Fall, der schon stimmte, nicht verschlechtert.
 */
class TastaturfolgeTest {

    private val tastatur = Quelltext.ohneKommentare("org/biglau/ui/BigKeypad.kt")
    private val sperre = Quelltext.ohneKommentare("org/biglau/ui/PinGate.kt")

    @Test
    fun `jede Taste hat einen Anker`() {
        assertTrue(
            "Die Tastatur legt keine Anker mehr an. Ohne sie kann sie den Fokus nicht " +
                "fuehren, und unter einer Ueberlagerung laeuft er weg.",
            Regex("""anchors = remember\([\s\S]{0,80}FocusRequester\(\)""").containsMatchIn(tastatur),
        )
        assertTrue(
            "Die Anker haengen nicht mehr an den Tasten.",
            "focusRequester(anchors[rowIndex][columnIndex])" in tastatur,
        )
    }

    @Test
    fun `jede Richtungstaste wird verbraucht`() {
        val weg = Quelltext.ausschnitt(tastatur, ".onPreviewKeyEvent { event ->", "verticalArrangement")
        listOf("DirectionUp", "DirectionDown", "DirectionLeft", "DirectionRight").forEach {
            assertTrue(
                "Key.$it wird nicht behandelt. Eine Richtungstaste, die durchrutscht, " +
                    "laesst Compose selbst suchen - und unter einer Ueberlagerung findet " +
                    "es eine Kachel, die niemand sieht.",
                "Key.$it ->" in weg,
            )
        }
        assertTrue(
            "Nicht jede PadDirection gibt true zurueck. Am Rand rutscht die Taste dann durch.",
            weg.split("true").size >= 5,
        )
    }

    /**
     * Kein blindes `moveFocus` - dieselbe Regel wie im Rasterrahmen. Sie steht hier ein
     * zweites Mal, weil sie hier ein zweites Mal gebrochen wurde.
     */
    @Test
    fun `gesucht wird nicht`() {
        assertTrue(
            "In der Tastatur steht wieder ein moveFocus. Das sucht sich selbst ein Ziel, " +
                "und unter einer Ueberlagerung ist das naechste eine Kachel, die niemand " +
                "sieht - am 04.09.2026 gemessen, nach zehnmal hoch war der Fokus weg.",
            "moveFocus" !in tastatur,
        )
    }

    @Test
    fun `unter der letzten Reihe steht Fertig`() {
        assertTrue(
            "Die Tastatur kennt keinen Weg nach unten hinaus. Die Fertig-Zeile darunter " +
                "waere dann mit Tasten unerreichbar.",
            "below?.let" in tastatur,
        )
        assertTrue(
            "Die PIN-Eingabe reicht den Anker ihrer Fertig-Zeile nicht mehr durch.",
            "below = doneAnchor" in sperre && "focusRequester(doneAnchor)" in sperre,
        )
        assertTrue(
            "Die PIN-Eingabe laesst die Tastatur den Fokus nicht mehr holen.",
            "takesFocus = true" in sperre,
        )
    }
}
