package org.biglau.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gerechnet wird gegen die am Jelly 2 gemessene Flaeche: 349 x 581 dp (PLAN.md 3.2).
 */
class GridMetricsTest {

    // Zellmasse des Standardrasters, siehe PLAN.md 3.2
    private val CELL_W = 165.6f
    private val CELL_H = 186.4f

    private val width = 349f
    private val height = 581f
    private val gutter = 4f

    @Test
    fun `Standardraster liefert die im Plan festgeschriebene Zelle`() {
        val m = gridMetrics(width, height, cols = 2, rows = 3, gutter = gutter, borderPercent = 2)
        assertEquals(165.6f, m.cellWidth, 0.5f)
        assertEquals(186.4f, m.cellHeight, 0.5f)
    }

    @Test
    fun `Zellen und Abstaende fuellen die Flaeche exakt aus`() {
        listOf(1 to 1, 2 to 2, 2 to 3, 3 to 4, 3 to 5, 6 to 8).forEach { (cols, rows) ->
            val m = gridMetrics(width, height, cols, rows, gutter, borderPercent = 2)
            val usedWidth = m.cellWidth * cols + gutter * (cols - 1) + m.border * 2
            val usedHeight = m.cellHeight * rows + gutter * (rows - 1) + m.border * 2
            assertEquals("Breite bei ${cols}x$rows", width, usedWidth, 0.01f)
            assertEquals("Hoehe bei ${cols}x$rows", height, usedHeight, 0.01f)
        }
    }

    @Test
    fun `eine gespannte Zelle deckt genau die Plaetze darunter ab`() {
        val m = gridMetrics(width, height, cols = 3, rows = 4, gutter = gutter, borderPercent = 2)
        // Zwei Spalten breit heisst: zwei Zellen plus der Abstand dazwischen.
        assertEquals(m.cellWidth * 2 + gutter, m.spanWidth(2, gutter), 0.001f)
        // Und die rechte Kante der gespannten Zelle trifft die der einzelnen daneben.
        val spannedRight = m.offsetX(0, gutter) + m.spanWidth(2, gutter)
        val singleRight = m.offsetX(1, gutter) + m.cellWidth
        assertEquals(singleRight, spannedRight, 0.001f)
    }

    @Test
    fun `Aussenrand wird auf den erlaubten Bereich begrenzt`() {
        assertEquals(0f, gridMetrics(width, height, 2, 3, gutter, borderPercent = -5).border, 0.001f)
        val maxBorder = gridMetrics(width, height, 2, 3, gutter, borderPercent = 99).border
        assertEquals(width * 0.15f, maxBorder, 0.001f)
    }

    @Test
    fun `Raster ohne Spalten ist ein Programmierfehler`() {
        listOf(0 to 3, 2 to 0, -1 to 3).forEach { (cols, rows) ->
            try {
                gridMetrics(width, height, cols, rows, gutter, 2)
                throw AssertionError("${cols}x$rows haette abgelehnt werden muessen")
            } catch (expected: IllegalArgumentException) {
                // so soll es sein
            }
        }
    }

    @Test
    fun `Beschriftung bleibt zwischen 14 und 40 sp`() {
        assertEquals(14f, labelSizeSp(165.6f, 40f, userScale = 1f), 0.01f)
        assertEquals(40f, labelSizeSp(600f, 600f, userScale = 1f), 0.01f)
        assertEquals(26.1f, labelSizeSp(165.6f, 186.4f, userScale = 1f), 0.2f)
    }

    @Test
    fun `eine hohe schmale Zelle bekommt keine Schrift die nicht in ihre Breite passt`() {
        // Der Fehler, den die erste gespannte Zelle zeigte: aus der Hoehe allein gerechnet
        // ergab eine 166 x 377 dp grosse Kachel 40 sp, und "Contacts" brach mitten im Wort.
        val tall = labelSizeSp(cellWidthDp = 165.6f, cellHeightDp = 376.8f, userScale = 1f)
        val square = labelSizeSp(cellWidthDp = 165.6f, cellHeightDp = 186.4f, userScale = 1f)
        assertTrue("Hohe Zelle darf nicht groesser setzen als die Breite hergibt", tall <= 165.6f * 0.16f)
        assertTrue("Aber auch nicht kleiner als die quadratische", tall >= square)
    }

    @Test
    fun `eine breite flache Zelle richtet sich nach der Hoehe`() {
        assertEquals(14f, labelSizeSp(cellWidthDp = 340f, cellHeightDp = 90f, userScale = 1f), 0.5f)
    }

    @Test
    fun `der Nutzerfaktor wirkt erst nach der Begrenzung`() {
        // Sonst koennte ein Faktor die Untergrenze unterlaufen und Text unlesbar machen.
        assertEquals(28f, labelSizeSp(165.6f, 40f, userScale = 2f), 0.01f)
        assertEquals(20f, labelSizeSp(600f, 600f, userScale = 0.5f), 0.01f)
    }

    @Test
    fun `Beschriftungszone bleibt bei der Standardzelle wie gehabt`() {
        val labelSp = labelSizeSp(165.6f, 186.4f, 1f)
        assertEquals(52.2f, labelZoneDp(186.4f, labelSp), 0.3f)
    }

    @Test
    fun `unter einer hohen Kachel klafft keine leere Zone`() {
        val labelSp = labelSizeSp(165.6f, 376.8f, 1f)
        val zone = labelZoneDp(376.8f, labelSp)
        assertTrue("Zone waere ${zone} dp hoch", zone < 376.8f * 0.28f)
        assertEquals(labelSp * 2.2f, zone, 0.01f)
    }

    @Test
    fun `eine einzeilige Anzeige passt in die Zellbreite`() {
        // Der Fehler, den die Uhr zeigte: aus der Hoehe allein gerechnet ergab
        // "4:54 PM" eine Groesse, bei der sich der Text selbst ueberlagerte.
        val short = singleLineSizeSp("9:41", CELL_W, CELL_H, userScale = 1f)
        val long = singleLineSizeSp("12:59 PM", CELL_W, CELL_H, userScale = 1f)
        assertTrue("Laengerer Text muss kleiner gesetzt werden", long < short)
        assertTrue("passt nicht in die Breite", long * 8 * 0.60f <= CELL_W - 16f + 0.5f)
    }

    @Test
    fun `in der Standardzelle begrenzt schon bei vier Zeichen die Breite`() {
        // (165,6 - 16) / (4 * 0,60) = 62,3 sp, waehrend die Hoehe 63,4 erlauben wuerde.
        assertEquals(62.3f, singleLineSizeSp("9:41", CELL_W, CELL_H, 1f), 0.3f)
    }

    @Test
    fun `auf einer breiten flachen Zelle begrenzt die Hoehe`() {
        assertEquals(34f, singleLineSizeSp("9:41", cellWidthDp = 600f, cellHeightDp = 100f, userScale = 1f), 0.3f)
    }

    @Test
    fun `die einzeilige Anzeige hat Ober- und Untergrenze`() {
        assertEquals(12f, singleLineSizeSp("sehr langer text hier", 60f, 40f, 1f), 0.5f)
        assertEquals(72f, singleLineSizeSp("9", 900f, 900f, 1f), 0.5f)
    }

    @Test
    fun `leerer Text stuerzt nicht ab`() {
        assertTrue(singleLineSizeSp("", CELL_W, CELL_H, 1f) > 0f)
    }

    @Test
    fun `Icon misst 40 Prozent der kuerzeren Kante`() {
        assertEquals(66.2f, iconSizeDp(cellWidthDp = 165.6f, cellHeightDp = 186.4f), 0.2f)
        // Eine breite, flache Zelle richtet sich nach der Hoehe.
        assertEquals(40f, iconSizeDp(cellWidthDp = 300f, cellHeightDp = 100f), 0.01f)
        assertEquals(96f, iconSizeDp(cellWidthDp = 400f, cellHeightDp = 400f), 0.01f)
        assertEquals(24f, iconSizeDp(cellWidthDp = 30f, cellHeightDp = 30f), 0.01f)
    }
}

/**
 * PLAN.md 4.2: Label-Größe 50–150 %, Icongröße 20–60 % der Zelle.
 *
 * Beides sind Zahlen, mit denen man sich die Kachel kaputtstellen könnte. Deshalb steht
 * hier, wo die Grenzen liegen und was passiert, wenn jemand darüber hinaus will.
 */
class TileSizingTest {

    private val breit = 165.6f
    private val hoch = 186.4f

    @Test
    fun `die label-groesse multipliziert auf die globale textgroesse`() {
        val normal = labelSizeSp(breit, hoch, userScale = 1f, labelScale = 1f)
        val gross = labelSizeSp(breit, hoch, userScale = 1f, labelScale = 1.5f)
        assertEquals(normal * 1.5f, gross, 0.01f)
    }

    // Ohne Grenze koennte eine importierte Datei die Beschriftung auf null setzen - und
    // eine Kachel ohne lesbares Wort ist eine Kachel, die man raten muss.
    @Test
    fun `die label-groesse wird beschnitten`() {
        val zuKlein = labelSizeSp(breit, hoch, userScale = 1f, labelScale = 0.1f)
        val kleinstes = labelSizeSp(breit, hoch, userScale = 1f, labelScale = LABEL_SCALE_MIN)
        assertEquals(kleinstes, zuKlein, 0.01f)

        val zuGross = labelSizeSp(breit, hoch, userScale = 1f, labelScale = 9f)
        val groesstes = labelSizeSp(breit, hoch, userScale = 1f, labelScale = LABEL_SCALE_MAX)
        assertEquals(groesstes, zuGross, 0.01f)
    }

    @Test
    fun `die icongroesse folgt dem anteil`() {
        val zwanzig = iconSizeDp(breit, hoch, percent = 20)
        val vierzig = iconSizeDp(breit, hoch, percent = 40)
        assertEquals(true, vierzig > zwanzig)
    }

    /**
     * Der Fall, der die Kachel kaputt machte: 60 % auf einer flachen Kachel plus
     * Beschriftungszone ist mehr als die Kachel hoch ist. Das Symbol legte sich über das
     * Wort, und beides war schlechter zu lesen als vorher.
     */
    @Test
    fun `das icon waechst nie in die beschriftung hinein`() {
        val flach = 90f
        val labelSp = labelSizeSp(340f, flach, userScale = 1f)
        val zone = labelZoneDp(flach, labelSp)
        val icon = iconSizeDp(340f, flach, percent = 60, labelZoneDp = zone)
        assertEquals(true, icon + zone <= flach)
    }

    // Gegenprobe zum Deckel: ohne Beschriftung gehoert die ganze Zelle dem Symbol.
    @Test
    fun `ohne beschriftung gilt der deckel nicht`() {
        assertEquals(24f, iconSizeDp(30f, 30f, percent = 40, labelZoneDp = 0f), 0.01f)
    }

    @Test
    fun `ein unmoeglicher anteil wird beschnitten`() {
        assertEquals(iconSizeDp(breit, hoch, ICON_PERCENT_MAX), iconSizeDp(breit, hoch, 200), 0.01f)
        assertEquals(iconSizeDp(breit, hoch, ICON_PERCENT_MIN), iconSizeDp(breit, hoch, 0), 0.01f)
    }

    // Die Vorgaben sind die Werte, mit denen die App bisher gemalt hat - niemandem soll
    // sich der Startbildschirm veraendern, nur weil es die Einstellung jetzt gibt.
    @Test
    fun `die vorgaben aendern nichts`() {
        assertEquals(1.0f, org.biglau.data.Appearance().labelScale, 0.001f)
        assertEquals(40, org.biglau.data.Appearance().iconPercent)
        assertEquals(
            labelSizeSp(breit, hoch, userScale = 1f),
            labelSizeSp(breit, hoch, userScale = 1f, labelScale = 1.0f),
            0.001f,
        )
    }
}

/**
 * Die Beschriftungszone braucht einen Boden.
 *
 * Aufgefallen, als die Querlage einstellbar wurde: eine Zelle von 60 dp Höhe bekam über
 * die 28-Prozent-Regel nur 16,8 dp, und eine fette 14-sp-Zeile braucht mit Unterlängen
 * knapp 19. Quer standen alle Beschriftungen abgeschnitten da. Es betrifft nicht nur die
 * Querlage — jede flache Kachel in einem dichten Raster hat dasselbe Problem.
 */
class LabelZoneFloorTest {

    @Test
    fun `eine flache zelle bekommt trotzdem eine volle zeile`() {
        val labelSp = labelSizeSp(cellWidthDp = 420f, cellHeightDp = 60f, userScale = 1f)
        val zone = labelZoneDp(60f, labelSp)
        assertTrue("Zone $zone reicht nicht fuer $labelSp sp", zone >= labelSp * 1.3f)
    }

    // Der Boden darf die Kachel nicht auffressen: bleibt fuer den Inhalt nichts uebrig,
    // ist die Kachel eine Zeile Text auf einer Farbflaeche.
    @Test
    fun `der boden nimmt nie mehr als die halbe kachel`() {
        for (hoehe in listOf(30f, 40f, 60f, 90f, 190f)) {
            val labelSp = labelSizeSp(300f, hoehe, userScale = 2f)
            assertTrue(labelZoneDp(hoehe, labelSp) <= hoehe * 0.5f + 0.01f)
        }
    }

    // Gegenprobe: bei hohen Kacheln aendert sich nichts, die alte Obergrenze gilt weiter.
    @Test
    fun `hohe kacheln bleiben wie sie waren`() {
        val labelSp = labelSizeSp(165.6f, 376.8f, userScale = 1f)
        assertEquals(labelSp * 2.2f, labelZoneDp(376.8f, labelSp), 0.01f)
    }
}
