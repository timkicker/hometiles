package org.biglau.data

import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Liest diese Fassung eine echte, gewachsene Konfiguration verlustfrei?
 *
 * Der Umzug auf ein neues Telefon ist der Grund, aus dem es die Sicherung überhaupt gibt.
 * Die synthetischen Prüfungen daneben decken jedes Feld ab, aber nicht die Mischung, die
 * über Wochen entsteht — mehrere Screens, ein Ordner, eigene Beschriftungen, Farben.
 *
 * Die Datei liegt bewusst **nicht** im Repository: sie enthält die App-Liste und die
 * Bildschirmnamen eines Menschen. Ohne sie überspringt der Test sich selbst.
 */
class RealConfigRoundTripTest {

    @Test
    fun `eine echte konfiguration ueberlebt den umzug`() {
        val pfad = System.getenv("BIGLAU_REAL_CONFIG")
        assumeTrue("BIGLAU_REAL_CONFIG nicht gesetzt", pfad != null)
        val datei = File(pfad!!)
        assumeTrue("Datei nicht da: $pfad", datei.exists())

        val text = datei.readText()
        val geladen = ConfigTransfer.import(text)
        assertEquals(true, geladen != null)
        geladen!!

        // Gegenprobe direkt aus dem JSON, ohne den Import-Weg.
        val original = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString(LauncherConfig.serializer(), text)
        assertEquals(original.screens.size, geladen.screens.size)
        assertEquals(original.screens.map { it.name }, geladen.screens.map { it.name })
        assertEquals(
            original.screens.sumOf { it.cells.size },
            geladen.screens.sumOf { it.cells.size },
        )
        assertEquals(original.appearance, geladen.appearance)
        assertEquals(original.behaviour, geladen.behaviour)
        assertEquals(original.homeScreenId, geladen.homeScreenId)
    }
}
