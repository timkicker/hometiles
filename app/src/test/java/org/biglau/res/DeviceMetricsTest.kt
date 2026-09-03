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
 * Die Auslegungsbasis aus `PLAN.md` 3.2 rechnet sich selbst nach.
 *
 * Die nutzbare Hoehe des Jelly 2 stand in drei Fassungen da - 581, dann 565, dann wieder
 * 581 -, und die mittlere war falsch, weil die Statusleiste mit 40 dp angesetzt war statt
 * mit den gemessenen 24. Aufgefallen ist das erst, als jemand am Geraet nachsah; bis dahin
 * stand ueber der Zahl „am Geraet gemessen“.
 *
 * Nachmessen kann dieser Test nicht - es laeuft kein Telefon dabei. Er kann aber das
 * verhindern, was den Fehler ueberhaupt erst moeglich machte: dass die Teilzahlen und die
 * Summe auseinanderlaufen, und dass anderswo eine andere Hoehe steht.
 */
class DeviceMetricsTest {

    private val plan = File("../PLAN.md").readText()

    /** Der dp-Wert aus der Zeile, die mit [zeile] beginnt. */
    private fun dpAus(zeile: String): Int {
        val stelle = plan.lineSequence().first { it.startsWith("| $zeile") }
        val zahl = Regex("""(\d+) dp""").findAll(stelle).lastOrNull()
            ?: throw AssertionError("keine dp-Zahl in: $stelle")
        return zahl.groupValues[1].toInt()
    }

    @Test
    fun `nutzbare Hoehe ist die Gesamthoehe ohne die beiden Leisten`() {
        val gesamt = Regex("""\| Gesamt \| \*\*\d+ × (\d+) dp\*\*""").find(plan)
            ?: throw AssertionError("Zeile „Gesamt“ fehlt in PLAN.md 3.2")
        val nutzbar = Regex("""\| Nutzbar nach den Systemleisten \| \*\*\d+ × (\d+) dp\*\*""")
            .find(plan) ?: throw AssertionError("Zeile „Nutzbar“ fehlt in PLAN.md 3.2")
        assertEquals(
            "Gesamt minus Status- und Gestenleiste ergibt nicht die nutzbare Hoehe",
            gesamt.groupValues[1].toInt() - dpAus("Statusleiste oben") -
                dpAus("Gestenleiste unten"),
            nutzbar.groupValues[1].toInt(),
        )
    }

    /**
     * Die dp-Zahlen der Tabelle passen zu den Pixeln daneben.
     *
     * 220 dpi heisst Faktor 1,375; eine Zeile, die 33 px als 40 dp ausgibt, ist genau der
     * Fehler von gestern. Gerundet wird auf ein dp, mehr gibt die Messung nicht her.
     */
    @Test
    fun `Pixel und dp der Systemleisten passen bei 220 dpi zusammen`() {
        listOf("Statusleiste oben", "Gestenleiste unten").forEach { zeile ->
            val stelle = plan.lineSequence().first { it.startsWith("| $zeile") }
            val px = Regex("""(\d+) px""").find(stelle)?.groupValues?.get(1)?.toInt()
                ?: throw AssertionError("keine Pixelzahl in: $stelle")
            assertEquals("$zeile rechnet nicht", Math.round(px / 1.375f), dpAus(zeile))
        }
    }

    /**
     * Wer die Hoehe des Jelly 2 in einem Test einsetzt, nimmt die aus dem Plan.
     *
     * `CallerPhotoSizeTest` rechnet aus, ob ein Foto den Annehmen-Knopf hinausschoebe.
     * Mit einer veralteten Hoehe prueft er einen Bildschirm, den es nicht gibt.
     */
    @Test
    fun `die Anrufbildschirm-Rechnung nimmt dieselbe Hoehe`() {
        val nutzbar = Regex("""\| Nutzbar nach den Systemleisten \| \*\*\d+ × (\d+) dp\*\*""")
            .find(plan)!!.groupValues[1]
        val test = Quelltext.datei("org/biglau/phone/CallerPhotoSizeTest.kt").readText()
        val wert = Regex("""val jelly = (\d+)f""").find(test)
            ?: throw AssertionError("CallerPhotoSizeTest nennt keine Hoehe mehr")
        assertEquals(
            "CallerPhotoSizeTest rechnet mit einer anderen Hoehe als PLAN.md 3.2",
            nutzbar,
            wert.groupValues[1],
        )
    }

    /** Die Tabelle behauptet, gemessen zu sein - dann steht auch dabei, womit. */
    @Test
    fun `die Leistenzeilen nennen ihre Quelle`() {
        listOf("ITYPE_STATUS_BAR", "ITYPE_NAVIGATION_BAR").forEach {
            assertTrue("$it fehlt in der Tabelle von 3.2", it in plan)
        }
    }

    /**
     * Die Zellmass-Tabelle rechnet sich aus den Funktionen, die sie beschreibt.
     *
     * Vier Zeilen mit Zelle, Labelzone und Schriftgroesse - und zwei davon standen am
     * 3.9.2026 falsch da. Die 2×2-Zeile nannte 79 dp Zone und 39 sp Schrift; richtig sind
     * 58 dp und 26 sp. Der Grund ist derselbe, den `labelSizeSp` in seinem eigenen KDoc
     * beschreibt: die Schrift kommt aus dem **Minimum beider Kanten**, seit „Contacts" auf
     * einer hohen schmalen Kachel mitten im Wort brach. Die Tabelle war auf dem Stand davor
     * stehengeblieben - der Plan beschrieb einen Fehler, den es nicht mehr gab.
     *
     * Gerechnet wird hier mit **den echten Funktionen**, nicht mit nachgebauten Formeln.
     * Eine nachgebaute Formel prueft, ob ich zweimal dasselbe gedacht habe.
     */
    @Test
    fun `die Zellmasse im Plan kommen aus den echten Funktionen`() {
        val breite = 349f
        val hoehe = Regex("""\| Nutzbar nach den Systemleisten \| \*\*\d+ × (\d+) dp\*\*""")
            .find(plan)!!.groupValues[1].toFloat()

        val zeilen = Regex(
            """^\| \*{0,2}(\d)×(\d)[^|]*\| \*{0,2}(\d+) × (\d+) dp\*{0,2} \| \*{0,2}(\d+) dp\*{0,2} \| \*{0,2}(\d+) sp\*{0,2} \|$""",
            RegexOption.MULTILINE,
        ).findAll(plan).toList()
        assertEquals("Die Zellmass-Tabelle in PLAN.md 3.2 ist nicht mehr zu finden", 4, zeilen.size)

        zeilen.forEach { treffer ->
            val (c, r, pb, ph, pz, ps) = treffer.destructured
            val masse = gridMetrics(breite, hoehe, c.toInt(), r.toInt(), gutter = 4f, borderPercent = 2)
            val sp = labelSizeSp(masse.cellWidth, masse.cellHeight, userScale = 1f)
            val zone = labelZoneDp(masse.cellHeight, sp)
            val raster = "${c}×${r}"
            assertEquals("$raster: Zellbreite", pb.toInt(), Math.round(masse.cellWidth))
            assertEquals("$raster: Zellhoehe", ph.toInt(), Math.round(masse.cellHeight))
            assertEquals("$raster: Labelzone", pz.toInt(), Math.round(zone))
            assertEquals("$raster: Schriftgroesse", ps.toInt(), Math.round(sp))
        }
    }

    /**
     * Der Plan widerspricht sich nicht selbst: das AVD hat die Dichte des Geraets.
     *
     * Abschnitt 7 nannte bis zum 3.9.2026 „480×854, 240 dpi" fuer das Emulator-AVD, waehrend
     * 3.2 zwei Absaetze weiter festhaelt: „220 dpi statt der angenommenen 240". Das AVD
     * selbst steht auf 220. Wer nach dem Plan ein AVD anlegt, baut sich damit ein Geraet,
     * das es nicht gibt - und prueft Layouts gegen falsche Masse.
     */
    @Test
    fun `das AVD im Plan hat die Dichte des Geraets`() {
        val geraet = Regex("""\| Physisch \| (\d+) × (\d+) px bei \*\*(\d+) dpi\*\*""")
            .find(plan) ?: throw AssertionError("Die Zeile Physisch fehlt in PLAN.md 3.2")
        val avd = Regex("""\*\*Emulator-AVD\*\* `jelly2` mit (\d+)×(\d+), \*\*(\d+) dpi\*\*""")
            .find(plan) ?: throw AssertionError("Die AVD-Zeile fehlt in PLAN.md 7")
        assertEquals("Breite", geraet.groupValues[1], avd.groupValues[1])
        assertEquals("Hoehe", geraet.groupValues[2], avd.groupValues[2])
        assertEquals(
            "Das AVD im Plan hat eine andere Dichte als das Geraet - dann prueft man " +
                "Layouts gegen Masse, die es nicht gibt.",
            geraet.groupValues[3],
            avd.groupValues[3],
        )
    }
}
