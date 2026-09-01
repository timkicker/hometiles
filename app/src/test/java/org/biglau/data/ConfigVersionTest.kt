package org.biglau.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Das Feld `version` stand in jeder gesicherten Datei und wurde von niemandem gelesen.
 *
 * Sein Zweck ist genau ein Fall: eine Sicherung vom neuen Telefon auf ein altes
 * zurückspielen. Dann enthält die Datei Felder, die die alte Fassung nicht kennt, und
 * `ignoreUnknownKeys` wirft sie beim Einlesen wortlos weg — man verliert Einstellungen,
 * ohne dass irgendetwas es sagt.
 */
class ConfigVersionTest {

    private fun datei(version: Int): String =
        """{"version":$version,"screens":[{"id":"a","name":"A","cols":2,"rows":3,"cells":[]}]}"""

    @Test
    fun `eine datei aus dieser fassung ist nicht neuer`() {
        assertEquals(false, ConfigTransfer.isFromNewerVersion(datei(CONFIG_VERSION)))
    }

    @Test
    fun `eine datei aus einer neueren fassung wird erkannt`() {
        assertEquals(true, ConfigTransfer.isFromNewerVersion(datei(CONFIG_VERSION + 1)))
    }

    @Test
    fun `eine aeltere datei ist kein grund zur warnung`() {
        assertEquals(false, ConfigTransfer.isFromNewerVersion(datei(CONFIG_VERSION - 1)))
    }

    // Ohne Angabe und bei Unfug keine Warnung: eine Warnung, die bei jeder krummen Datei
    // erscheint, sagt nichts mehr.
    @Test
    fun `ohne versionsangabe wird nicht gewarnt`() {
        assertEquals(
            false,
            ConfigTransfer.isFromNewerVersion("""{"screens":[{"id":"a","name":"A","cols":2,"rows":3,"cells":[]}]}"""),
        )
        assertEquals(false, ConfigTransfer.isFromNewerVersion("kein json"))
    }

    // Und die Datei muss trotzdem einlesbar bleiben - warnen heisst nicht ablehnen.
    @Test
    fun `eine neuere datei wird trotzdem eingelesen`() {
        val geladen = ConfigTransfer.import(datei(CONFIG_VERSION + 1))
        assertEquals(1, geladen?.screens?.size)
    }
}
