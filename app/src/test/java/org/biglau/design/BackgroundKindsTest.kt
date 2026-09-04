package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Es gibt genau zwei Arten von Hintergrund: Themafarbe und eigene Farbe.
 *
 * Kein Bild. Hinter einem Foto ist kein Kontrast planbar - an jeder Stelle des Bildes
 * anders -, und die Kontrastpruefungen in `ScreenBackgroundTest` waeren damit hinfaellig.
 * Diese Regel haelt fest, dass der Fall gar nicht erst im Modell steht: ein Fall, den
 * niemand behandelt, ist schlimmer als einer, den es nicht gibt.
 *
 * Steht in `:app` und nicht bei den Farbtests in `:core:ui`, weil sie **Quelltext liest**:
 * die Liste der Modulwurzeln gehoert zum Testquelltext von `:app`, und alle Regeln, die
 * Quelltext lesen, stehen deshalb dort beisammen.
 */
class BackgroundKindsTest {

    @Test
    fun `es gibt genau zwei arten von hintergrund`() {
        val modell = Quelltext.datei("org/biglau/data/Model.kt").readText()
        val abschnitt = Quelltext.ausschnitt(modell, "sealed interface Background {", "}")
        assertEquals(true, abschnitt.contains("Theme"))
        assertEquals(true, abschnitt.contains("Solid"))
        assertEquals(false, abschnitt.contains("Image"))
    }
}
