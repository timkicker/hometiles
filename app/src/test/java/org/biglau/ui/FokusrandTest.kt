package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Fokusrand muss dicker sein als der Blinkrand je wird.
 *
 * Am 04.09.2026 am Emulator gemessen, bevor eine Zeile Tastenbedienung gebaut war: der Fokus
 * bewegt sich schon heute mit dem D-Pad, weil Compose aus jedem `combinedClickable` von
 * selbst ein Fokusziel macht. Drei Druecke, und er stand auf der Apps-Kachel. Man sieht ihn
 * nur nicht.
 *
 * Und der eine Rand, den man sah, bedeutete etwas anderes: Nachrichten trug einen weissen
 * Rand, weil dort 17 Ungelesene warteten. Wer mit Tasten arbeitet, sieht genau eine
 * hervorgehobene Kachel und startet mit der Auswahltaste eine andere. Das ist der
 * schlimmste Fehlgriff, den ein Startbildschirm anbieten kann, und er entsteht nicht aus
 * einem Fehler, sondern daraus, dass zwei Dinge dasselbe Bild benutzen.
 *
 * Deshalb diese Regel: **der Fokusrand ist breiter als der Blinkrand an seinem dicksten
 * Punkt.** Nicht knapp breiter, sondern so, dass es auch der sieht, fuer den die App gebaut
 * ist. Die Zahl steht im Quelltext und wird hier gelesen, nicht abgeschrieben.
 *
 * Warum die Breite und nicht die Farbe: der Zustand einer Flaeche darf nicht allein in der
 * Farbe stehen, das ist in diesem Baum schon mehrfach schiefgegangen. Eine Breite sieht
 * auch, wer Farben schlecht unterscheidet.
 *
 * Die Information des Blinkens geht dabei nicht verloren. Die Zahl in der Ecke wird
 * unabhaengig vom Rand gezeichnet; eine fokussierte Kachel mit Neuem zeigt weiter ihre Zahl.
 */
class FokusrandTest {

    private val quelle = Quelltext.ohneKommentare("org/biglau/ui/BigTile.kt")

    private fun zahl(name: String): Float {
        val treffer = Regex("""$name\s*=\s*([0-9.]+)f""").find(quelle)
            ?: throw AssertionError(
                "Die Konstante $name steht nicht mehr in BigTile.kt. Ohne sie misst diese " +
                    "Regel nichts und bliebe gruen.",
            )
        return treffer.groupValues[1].toFloat()
    }

    @Test
    fun `der Fokusrand ist dicker als der Blinkrand`() {
        val blinkRand = zahl("BADGE_BORDER_DP")
        val fokus = zahl("FOCUS_BORDER_DP")
        assertTrue(
            "Der Fokusrand ($fokus dp) ist nicht dicker als der Blinkrand an seinem " +
                "dicksten Punkt ($blinkRand dp). Dann zeigen auf dem Startbildschirm " +
                "zwei gleich aussehende Raender auf zwei verschiedene Ziele.",
            fokus > blinkRand,
        )
    }

    @Test
    fun `der Unterschied ist auch aus einem Meter zu sehen`() {
        // Knapp dicker reicht nicht: auf drei Zoll ist ein Dpunkt Unterschied nichts.
        // Die Haelfte mehr ist die Schwelle, an der zwei Raender verschieden aussehen.
        val blinkRand = zahl("BADGE_BORDER_DP")
        val fokus = zahl("FOCUS_BORDER_DP")
        assertTrue(
            "Der Fokusrand ist nur $fokus dp gegen $blinkRand dp. Zu knapp, um ihn " +
                "im Vorbeisehen zu unterscheiden.",
            fokus >= blinkRand * 1.5f,
        )
    }

    @Test
    fun `der Fokus setzt den Rand, nicht nur die Farbe`() {
        assertTrue(
            "Die Randbreite kennt den Fokus nicht. Ein Zustand, der nur in der Farbe " +
                "steht, erreicht niemanden mit einer Farbschwaeche.",
            Regex("""borderWidth[\s\S]{0,400}focused""").containsMatchIn(quelle),
        )
    }
}

/**
 * Der Fokus bleibt in dem Screen, der ihn zeichnet.
 *
 * PLAN.md 10.3.5. `absorbTouches` schluckt jeden Tipp, der neben eine Kachel der
 * Ueberlagerung geht, damit er nicht auf den Startbildschirm darunter durchschlaegt. Der
 * Modifier ist reines `pointerInput` und hat fuer Tasten kein Gegenstueck.
 *
 * Getragen wird die Sperre von zwei Dingen im Rasterrahmen, und beide stehen hier:
 *
 * 1. Die Ziele kommen aus `ziele`, also aus den Zellen **dieses** Screens. Ein Anker fuer
 *    eine Kachel darunter existiert gar nicht, es gibt also nichts anzufordern.
 * 2. Jede der vier Richtungstasten wird verbraucht, auch wenn sich nichts bewegt. Sonst
 *    sucht Compose sich selbst ein Ziel, zur Not im Bildschirm darunter oder in der
 *    Kopfzeile.
 *
 * Am 04.09.2026 am Emulator nachgestellt: Ordner geoeffnet, neun Tastendruecke in alle
 * Richtungen, der Fokus blieb jedes Mal auf einer Flaeche des Ordners.
 */
class FokussperreTest {

    private val quelle = Quelltext.ohneKommentare("org/biglau/ui/HomeScreenView.kt")

    @Test
    fun `die Ziele kommen nur aus diesem Screen`() {
        assertTrue(
            "Der Rasterrahmen rechnet nicht mehr mit den Zielen dieses Screens. Dann kann " +
                "ein Anker aus einem anderen Screen gemeint sein.",
            "FocusOrder.neighbour(targets," in quelle,
        )
        assertTrue(
            "Die Anker werden nicht aus den Zielen gebaut.",
            Regex("""anchors\s*=\s*remember\(targets\)""").containsMatchIn(quelle),
        )
    }

    @Test
    fun `jede Richtungstaste wird verbraucht`() {
        val stelle = Quelltext.ausschnitt(quelle, ".onPreviewKeyEvent { key ->", "else -> false")
        listOf("DirectionLeft", "DirectionRight", "DirectionUp", "DirectionDown").forEach {
            assertTrue(
                "Key.$it wird nicht behandelt. Eine Richtungstaste, die durchrutscht, " +
                    "laesst Compose selbst ein Ziel suchen.",
                "Key.$it -> move(" in stelle,
            )
        }
        // Der Rueckgabewert von bewege entscheidet, ob die Taste verbraucht ist. Er steht
        // einmal am Ende und ist immer true, auch wenn kein Nachbar gefunden wurde.
        val bewegen = Quelltext.ausschnitt(quelle, "fun move(", ".onPreviewKeyEvent { key ->")
        assertTrue(
            "bewege gibt nicht immer true zurueck. Am Rand rutscht die Taste dann durch " +
                "und Compose sucht sich selbst ein Ziel: " + bewegen,
            "return false" !in bewegen && "return true" in bewegen,
        )
    }
}
