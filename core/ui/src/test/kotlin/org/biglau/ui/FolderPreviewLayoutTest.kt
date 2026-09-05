package org.biglau.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the preview in a folder was computed from the cell width alone.
 *
 * in landscape a tile here is 400 dp wide and 60 tall: the icons grew as large as allowed,
 * two rows came to 72 dp and pushed the label out of the tile. the folder then had no name at
 * all, and one could not see what was in it.
 */
class FolderPreviewLayoutTest {

    @Test
    fun `upright everything stays as before`() {
        // 2x3 here: 165.6 wide, about 134 tall after the label.
        val edge = FolderPreviewLayout.edgeDp(cellWidthDp = 165.6f, availableHeightDp = 134f)
        assertEquals(33.1f, edge, 0.2f)
        assertEquals(2, FolderPreviewLayout.rows(134f, edge))
    }

    @Test
    fun `in landscape the icons shrink instead of pushing the label out`() {
        val height = 41f
        val edge = FolderPreviewLayout.edgeDp(cellWidthDp = 400f, availableHeightDp = height)
        assertTrue("edge $edge does not fit into $height", edge * 2 + FolderPreviewLayout.GAP_DP <= height)
    }

    // if only one row fits, one whole row is more honest than two cut ones.
    @Test
    fun `with very little height one row stays`() {
        val edge = FolderPreviewLayout.edgeDp(cellWidthDp = 400f, availableHeightDp = 20f)
        assertEquals(1, FolderPreviewLayout.rows(20f, edge))
    }

    // bounded below: under 14 dp a preview icon is no longer a hint.
    @Test
    fun `the icons do not get arbitrarily small`() {
        assertEquals(14f, FolderPreviewLayout.edgeDp(cellWidthDp = 400f, availableHeightDp = 4f), 0.01f)
    }
}
