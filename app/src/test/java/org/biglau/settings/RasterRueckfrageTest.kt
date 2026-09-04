package org.biglau.settings

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ein kleineres Raster kostet Kacheln - auf beiden Wegen dorthin wird vorher gefragt.
 *
 * Das Raster laesst sich auf **zwei** Arten aendern: ueber die Vorschlaege ganz oben
 * („2 × 3 Felder") und ueber die freien Listen darunter („3 Zeilen"). Beide fuehren in
 * dieselbe Sache, und beide muessen dieselbe Rueckfrage stellen: erst die Zahl der Kacheln
 * nennen, die verschwinden, und erst beim zweiten Tipp aendern. Zwei Wege, die
 * auseinanderlaufen, sind der Fall, den diese Nacht schon zweimal gebracht hat - beim
 * Einlesen einer Sicherung und bei der Zeile zu den ausgeblendeten Apps.
 *
 * Am 04.09.2026 am Jelly 2 nachgezaehlt: bei 2 × 4 Feldern und acht Kacheln sagte die Liste
 * „1 × 2 Felder - 6 Kacheln gingen verloren", „2 × 2 - 4", „2 × 3 - 2" und bei „2 × 4"
 * „Das ist das jetzige". Die Zahlen stimmen mit `config.json`.
 */
class RasterRueckfrageTest {

    private val einstellungen = Quelltext.ohneKommentare("org/biglau/settings/SettingsActivity.kt")

    @Test
    fun `beide Wege zaehlen die Kacheln, die verschwinden`() {
        val stellen = einstellungen.split("ScreenEdits.dropped(").size - 1
        assertTrue(
            "Nicht jeder Weg zum Raster zaehlt, was verloren geht - dann kostet einer von " +
                "beiden Kacheln, ohne es zu sagen.",
            stellen >= 2,
        )
    }

    @Test
    fun `beide Wege aendern erst beim zweiten Tipp`() {
        // Die Vorschlagsliste und `GridChoiceRow` - zwei Stellen, ein Verhalten.
        val scharf = Regex("""armed ->""").findAll(einstellungen).count()
        assertTrue(
            "Nur eine der beiden Rasterlisten fragt vor einem Verlust nach: $scharf " +
                "Stellen mit einer scharfen Zeile.",
            scharf >= 2,
        )
        assertEquals(
            "Ein Rasterwechsel ohne Verlust soll nicht nachfragen - eine Rueckfrage, die " +
                "auch dann kommt, wenn nichts passiert, liest bald niemand mehr.",
            2,
            einstellungen.split("loses == 0 ->").size - 1 +
                (einstellungen.split("verliert == 0 ->").size - 1),
        )
    }
}
