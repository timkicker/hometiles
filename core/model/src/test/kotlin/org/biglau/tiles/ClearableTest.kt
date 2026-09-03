package org.biglau.tiles

import org.biglau.data.Button
import org.biglau.data.ButtonAction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * „Kachel leeren" erscheint nur, wenn es etwas zu leeren gibt.
 *
 * Am Telefon gesehen: im Editor einer **frischen** Kachel stand „Clear tile" in Warnfarbe,
 * ganz unten, direkt über „Done" - der auffälligste Knopf des Bildschirms, und er tat
 * nichts. Derselbe Bildschirm blendet „Verschieben", „Blinken" und die Zweitbelegung für
 * leere Kacheln schon aus, mit dem Kommentar: eine Zeile anzubieten, die dann sagt „geht
 * nicht", ist schlechter als sie wegzulassen.
 */
class ClearableTest {

    @Test
    fun `eine frische Kachel hat nichts zu leeren`() {
        assertFalse(TileEdits.clearable(Button()))
    }

    @Test
    fun `eine belegte Kachel schon`() {
        assertTrue(TileEdits.clearable(Button(action = ButtonAction.GoToScreen("home"))))
    }

    /**
     * Der Fall, für den der Vergleich gegen die *ganze* Kachel geht und nicht nur gegen die
     * Aktion: eine leere Kachel darf eine eigene Beschriftung tragen - der Startbildschirm
     * zeigt sie dann statt „Antippen zum Belegen". Die ist etwas zum Leeren.
     */
    @Test
    fun `eine leere Kachel mit eigener Beschriftung hat etwas zu leeren`() {
        assertTrue(TileEdits.clearable(Button(label = "Später")))
    }

    @Test
    fun `auch eine vorgewaehlte Farbe oder ein Symbol zaehlen`() {
        assertTrue(TileEdits.clearable(Button(colorHue = 210f)))
        assertTrue(TileEdits.clearable(Button(iconName = "star")))
        assertTrue(TileEdits.clearable(Button(colorIndex = 2)))
    }

    /** Und die Zweitbelegung - sie überlebt sonst unsichtbar auf einer leeren Kachel. */
    @Test
    fun `eine Zweitbelegung zaehlt auch`() {
        assertTrue(TileEdits.clearable(Button(longPress = ButtonAction.GoToScreen("home"))))
    }
}
