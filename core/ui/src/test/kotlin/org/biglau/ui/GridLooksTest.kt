package org.biglau.ui

import java.io.File
import org.biglau.data.Appearance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PLAN.md 4.1 promises gutter, outer border and corner radius as settings; 3.2 names the
 * bounds. all three stood in the model from the first day, were read at eighteen places, and
 * could be changed nowhere.
 */
class GridLooksTest {

    @Test
    fun `the defaults are the ones from the plan`() {
        assertEquals(4, Appearance().gutterDp)
        assertEquals(2, Appearance().safeBorderPercent)
        assertEquals(12, Appearance().cornerRadiusDp)
    }

    @Test
    fun `the choices keep to the bounds from 3 point 2`() {
        assertTrue(GridLooks.GUTTERS.all { it in 0..12 })
        assertTrue(GridLooks.BORDERS.all { it in 0..15 })
        assertTrue(GridLooks.RADII.all { it in 0..24 })
    }

    // an imported file can hold anything. an outer border of 80 percent would leave a strip
    // of the grid one could not get out of again without adb.
    @Test
    fun `impossible values are trimmed`() {
        assertEquals(12, GridLooks.gutter(99))
        assertEquals(0, GridLooks.gutter(-5))
        assertEquals(15, GridLooks.border(80))
        assertEquals(24, GridLooks.radius(100))
    }

    @Test
    fun `every default is on offer too`() {
        assertTrue(Appearance().gutterDp in GridLooks.GUTTERS)
        assertTrue(Appearance().safeBorderPercent in GridLooks.BORDERS)
        assertTrue(Appearance().cornerRadiusDp in GridLooks.RADII)
    }
}
