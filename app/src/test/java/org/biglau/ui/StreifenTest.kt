package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Streifen unter dem Raster ist mit Tasten zu erreichen und wieder zu verlassen.
 *
 * PLAN.md 10.3.5. In einem Ordner und in der Liste der Menuetaste steht unter dem Raster
 * eine Zeile, die die Ueberlagerung schliesst. Sie ist ein Geschwister des Rasterrahmens,
 * nicht sein Kind, und der Rahmen verbraucht jede Richtungstaste - damit kam der Fokus nie
 * zu ihr. Am 04.09.2026 von `tools/unerreichbar.py` gemeldet: neun anklickbare Flaechen im
 * Ordner, acht erreicht, und die neunte war der Streifen.
 *
 * Ein sichtbarer Weg, den die Tasten nicht halten, ist schlimmer als keiner. Die
 * Zurueck-Taste schliesst zwar auch, aber sie steht nirgends angeschrieben.
 *
 * **Die Falle dabei, teuer bezahlt:** als der Streifen erreichbar war, verlor ein Druck nach
 * rechts den Fokus **ganz**. Compose sucht dann selbst und findet nichts, weil die Zeile die
 * volle Breite hat - und danach half keine Taste mehr, denn ohne Fokus laeuft kein
 * Tastenhandler an. Derselbe Fehler, der eine Stunde vorher im ganzen Ordner steckte, nur
 * eine Zeile kleiner. Deshalb pruefen die Regeln hier **beide Richtungen**: hin und zurueck,
 * und dass dazwischen nichts durchrutscht.
 */
class StreifenTest {

    private val rahmen = Quelltext.ohneKommentare("org/biglau/ui/HomeScreenView.kt")
    private val start = Quelltext.ohneKommentare("org/biglau/MainActivity.kt")

    /** Die Zeile, die die Ueberlagerung schliesst, mit ihren Modifiern. */
    private val streifen =
        Quelltext.ausschnitt(start, "stringResource(schliessen)", "onClick = onClose")

    @Test
    fun `unter der letzten Zeile fuehrt eine Taste zum Streifen`() {
        val bewegen = Quelltext.ausschnitt(rahmen, "fun move(", ".onPreviewKeyEvent { key ->")
        assertTrue(
            "Der Rasterrahmen schickt den Fokus am unteren Rand nicht mehr zum Streifen. " +
                "Dann steht dort eine Zeile, die kein Tastendruck erreicht: " + bewegen,
            "PadDirection.DOWN" in bewegen && "below" in bewegen,
        )
    }

    /**
     * Und zwar an einen **benannten** Anker.
     *
     * `moveFocus(Down)` nimmt den naechsten fokussierbaren Knoten. Unter der Ueberlagerung
     * liegen die Kacheln des Startbildschirms, an denselben Stellen wie das Raster darueber.
     * Der Fokus landete dann unsichtbar dort - genau das, was `FokussperreTest` verhindern
     * soll.
     */
    @Test
    fun `der Weg ist ein benannter Anker, keine blinde Suche`() {
        assertTrue(
            "Im Rasterrahmen steht wieder ein moveFocus. Das sucht sich selbst ein Ziel, " +
                "und unter einer Ueberlagerung ist das naechste eine Kachel, die niemand " +
                "sieht.",
            "moveFocus" !in rahmen,
        )
        assertTrue(
            "Der Streifen traegt keinen Anker mehr, also weiss der Rahmen nicht, wohin.",
            "focusRequester(belowAnchor)" in streifen,
        )
    }

    @Test
    fun `vom Streifen fuehrt eine Taste zurueck ins Raster`() {
        assertTrue(
            "Der Streifen schickt den Fokus nach oben nicht mehr ins Raster zurueck. " +
                "Dann ist er eine Sackgasse: " + streifen,
            Regex("""Key\.DirectionUp[\s\S]{0,120}backAnchor\.requestFocus""")
                .containsMatchIn(streifen),
        )
        assertTrue(
            "Der Rahmen bietet den Rueckweg nicht mehr an. Der Anker des Streifens zeigt " +
                "dann auf nichts.",
            "gridAnchor" in rahmen,
        )
    }

    /**
     * Die drei uebrigen Richtungen werden verbraucht und tun nichts.
     *
     * Das ist der Fall, der den Fokus verschwinden liess. Eine Richtungstaste, die auf dem
     * Streifen durchrutscht, laesst Compose selbst suchen; findet es nichts, ist der Fokus
     * weg, und ein Bildschirm ohne Fokus ist an einem Tastentelefon dasselbe wie ein
     * eingefrorener.
     */
    @Test
    fun `die uebrigen Richtungen rutschen nicht durch`() {
        listOf("DirectionDown", "DirectionLeft", "DirectionRight").forEach {
            assertTrue(
                "Key.$it wird auf dem Streifen nicht verbraucht. Ein Druck dorthin, und " +
                    "der Fokus ist weg: " + streifen,
                "Key.$it" in streifen,
            )
        }
        assertTrue(
            "Die verbrauchten Richtungen geben nicht mehr true zurueck.",
            Regex("""DirectionRight -> true""").containsMatchIn(streifen),
        )
    }
}
