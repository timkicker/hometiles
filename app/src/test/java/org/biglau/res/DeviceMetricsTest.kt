package org.biglau.res

import java.io.File
import org.biglau.Quelltext
import org.biglau.ui.gridMetrics
import org.biglau.ui.labelSizeSp
import org.biglau.ui.labelZoneDp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the design basis of `PLAN.md` 3.2 checks its own arithmetic.
 *
 * the jelly 2's usable height stood there in three versions - 581, then 565, then 581 again -
 * and the middle one was wrong because the status bar was put at 40 dp instead of the
 * measured 24, under a heading that said "measured on the device".
 *
 * no phone runs here, so this cannot measure. it can stop what made the mistake possible:
 * the parts and the sum drifting apart, and another height standing elsewhere.
 */
class DeviceMetricsTest {

    private val plan = File("../PLAN.md").readText()

    /** the dp value from the line starting with [line]. */
    private fun dpFrom(line: String): Int {
        val place = plan.lineSequence().first { it.startsWith("| $line") }
        val number = Regex("""(\d+) dp""").findAll(place).lastOrNull()
            ?: throw AssertionError("no dp number in: $place")
        return number.groupValues[1].toInt()
    }

    @Test
    fun `the usable height is the whole height without the two bars`() {
        val whole = Regex("""\| Gesamt \| \*\*\d+ × (\d+) dp\*\*""").find(plan)
            ?: throw AssertionError("the total row is gone from PLAN.md 3.2")
        val usable = Regex("""\| Nutzbar nach den Systemleisten \| \*\*\d+ × (\d+) dp\*\*""")
            .find(plan) ?: throw AssertionError("the usable row is gone from PLAN.md 3.2")
        assertEquals(
            "total minus status and gesture bar does not give the usable height",
            whole.groupValues[1].toInt() - dpFrom("Statusleiste oben") -
                dpFrom("Gestenleiste unten"),
            usable.groupValues[1].toInt(),
        )
    }

    /** 220 dpi means a factor of 1.375; a row printing 33 px as 40 dp is the old mistake. */
    @Test
    fun `pixels and dp of the system bars match at 220 dpi`() {
        listOf("Statusleiste oben", "Gestenleiste unten").forEach { line ->
            val place = plan.lineSequence().first { it.startsWith("| $line") }
            val px = Regex("""(\d+) px""").find(place)?.groupValues?.get(1)?.toInt()
                ?: throw AssertionError("no pixel number in: $place")
            assertEquals("$line does not add up", Math.round(px / 1.375f), dpFrom(line))
        }
    }

    /**
     * `CallerPhotoSizeTest` works out whether a photo would push the answer button off the
     * screen. with an outdated height it checks a screen that does not exist.
     */
    @Test
    fun `the call screen arithmetic takes the same height`() {
        val usable = Regex("""\| Nutzbar nach den Systemleisten \| \*\*\d+ × (\d+) dp\*\*""")
            .find(plan)!!.groupValues[1]
        val test = Quelltext.file("org/biglau/phone/CallerPhotoSizeTest.kt").readText()
        val value = Regex("""val jelly = (\d+)f""").find(test)
            ?: throw AssertionError("CallerPhotoSizeTest names no height any more")
        assertEquals(
            "CallerPhotoSizeTest works with a different height than PLAN.md 3.2",
            usable,
            value.groupValues[1],
        )
    }

    /** the table claims to be measured - then it says with what. */
    @Test
    fun `the bar rows name their source`() {
        listOf("ITYPE_STATUS_BAR", "ITYPE_NAVIGATION_BAR").forEach {
            assertTrue("$it is gone from the table in 3.2", it in plan)
        }
    }

    /**
     * two of the four rows stood wrong: the 2x2 row said 79 dp zone and 39 sp where 58 and 26
     * are right. the type comes from the **minimum of both edges** since "Contacts" broke
     * mid-word on a tall narrow tile, and the table had stayed on the state before that.
     *
     * computed with **the real functions**: a rebuilt formula only checks whether the same
     * thought was had twice.
     */
    @Test
    fun `the cell sizes in the plan come from the real functions`() {
        val width = 349f
        val height = Regex("""\| Nutzbar nach den Systemleisten \| \*\*\d+ × (\d+) dp\*\*""")
            .find(plan)!!.groupValues[1].toFloat()

        val rows = Regex(
            """^\| \*{0,2}(\d)×(\d)[^|]*\| \*{0,2}(\d+) × (\d+) dp\*{0,2} \| \*{0,2}(\d+) dp\*{0,2} \| \*{0,2}(\d+) sp\*{0,2} \|$""",
            RegexOption.MULTILINE,
        ).findAll(plan).toList()
        assertEquals("the cell size table in PLAN.md 3.2 is no longer to be found", 4, rows.size)

        rows.forEach { hit ->
            val (c, r, pw, ph, pz, ps) = hit.destructured
            val metrics = gridMetrics(width, height, c.toInt(), r.toInt(), gutter = 4f, borderPercent = 2)
            val sp = labelSizeSp(metrics.cellWidth, metrics.cellHeight, userScale = 1f)
            val zone = labelZoneDp(metrics.cellHeight, sp)
            val grid = "${c}x${r}"
            assertEquals("$grid: cell width", pw.toInt(), Math.round(metrics.cellWidth))
            assertEquals("$grid: cell height", ph.toInt(), Math.round(metrics.cellHeight))
            assertEquals("$grid: label zone", pz.toInt(), Math.round(zone))
            assertEquals("$grid: type size", ps.toInt(), Math.round(sp))
        }
    }

    /**
     * section 7 named 240 dpi for the emulator avd while 3.2 holds 220, which is what the avd
     * itself runs at: building an avd from the plan would build a device that does not exist.
     */
    @Test
    fun `the avd in the plan has the density of the device`() {
        val device = Regex("""\| Physisch \| (\d+) × (\d+) px bei \*\*(\d+) dpi\*\*""")
            .find(plan) ?: throw AssertionError("the physical row is gone from PLAN.md 3.2")
        val avd = Regex("""\*\*Emulator-AVD\*\* `jelly2` mit (\d+)×(\d+), \*\*(\d+) dpi\*\*""")
            .find(plan) ?: throw AssertionError("the avd row is gone from PLAN.md 7")
        assertEquals("width", device.groupValues[1], avd.groupValues[1])
        assertEquals("height", device.groupValues[2], avd.groupValues[2])
        assertEquals(
            "the avd in the plan has a different density than the device - then layouts are " +
                "checked against sizes that do not exist.",
            device.groupValues[3],
            avd.groupValues[3],
        )
    }
}
