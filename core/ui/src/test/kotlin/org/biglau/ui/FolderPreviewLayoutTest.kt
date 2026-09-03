package org.biglau.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Vorschau im Ordner war nur aus der Zellbreite gerechnet.
 *
 * Quer ist eine Kachel auf diesem Gerät 400 dp breit und 60 hoch: die Symbole wurden so
 * groß wie erlaubt, zwei Reihen ergaben 72 dp — und schoben die Beschriftung aus der
 * Kachel. Der Ordner hieß dann gar nichts mehr, und man sah ihm nicht an, was drin ist.
 */
class FolderPreviewLayoutTest {

    @Test
    fun `hochkant bleibt alles wie bisher`() {
        // 2x3 auf diesem Geraet: 165,6 breit, nach der Beschriftung rund 134 hoch.
        val kante = FolderPreviewLayout.edgeDp(cellWidthDp = 165.6f, availableHeightDp = 134f)
        assertEquals(33.1f, kante, 0.2f)
        assertEquals(2, FolderPreviewLayout.rows(134f, kante))
    }

    @Test
    fun `quer schrumpfen die symbole statt die beschriftung zu verdraengen`() {
        val hoehe = 41f
        val kante = FolderPreviewLayout.edgeDp(cellWidthDp = 400f, availableHeightDp = hoehe)
        assertTrue("Kante $kante passt nicht in $hoehe", kante * 2 + FolderPreviewLayout.GAP_DP <= hoehe)
    }

    // Passt nur eine Reihe, ist eine ganze Reihe ehrlicher als zwei angeschnittene.
    @Test
    fun `bei sehr wenig hoehe bleibt eine reihe`() {
        val kante = FolderPreviewLayout.edgeDp(cellWidthDp = 400f, availableHeightDp = 20f)
        assertEquals(1, FolderPreviewLayout.rows(20f, kante))
    }

    // Nach unten begrenzt: unter 14 dp ist ein Vorschausymbol kein Hinweis mehr.
    @Test
    fun `die symbole werden nicht beliebig klein`() {
        assertEquals(14f, FolderPreviewLayout.edgeDp(cellWidthDp = 400f, availableHeightDp = 4f), 0.01f)
    }
}
