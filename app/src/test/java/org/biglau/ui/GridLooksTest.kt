package org.biglau.ui

import java.io.File
import org.biglau.data.Appearance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PLAN.md 4.1 sagt Rasterabstand, Außenrand und Eckenradius als Einstellungen zu; 3.2
 * nennt die Grenzen. Alle drei standen seit dem ersten Tag im Modell, wurden an achtzehn
 * Stellen gelesen — und waren nirgends zu ändern.
 */
class GridLooksTest {

    @Test
    fun `die vorgaben sind die aus dem plan`() {
        assertEquals(4, Appearance().gutterDp)
        assertEquals(2, Appearance().safeBorderPercent)
        assertEquals(12, Appearance().cornerRadiusDp)
    }

    @Test
    fun `die auswahl haelt sich an die grenzen aus 3 punkt 2`() {
        assertTrue(GridLooks.GUTTERS.all { it in 0..12 })
        assertTrue(GridLooks.BORDERS.all { it in 0..15 })
        assertTrue(GridLooks.RADII.all { it in 0..24 })
    }

    // Eine importierte Datei kann alles enthalten. Ein Aussenrand von 80 Prozent liesse
    // vom Raster einen Streifen uebrig, aus dem man ohne adb nicht mehr herauskaeme.
    @Test
    fun `unmoegliche werte werden beschnitten`() {
        assertEquals(12, GridLooks.gutter(99))
        assertEquals(0, GridLooks.gutter(-5))
        assertEquals(15, GridLooks.border(80))
        assertEquals(24, GridLooks.radius(100))
    }

    @Test
    fun `jede vorgabe steht auch zur wahl`() {
        assertTrue(Appearance().gutterDp in GridLooks.GUTTERS)
        assertTrue(Appearance().safeBorderPercent in GridLooks.BORDERS)
        assertTrue(Appearance().cornerRadiusDp in GridLooks.RADII)
    }
}
