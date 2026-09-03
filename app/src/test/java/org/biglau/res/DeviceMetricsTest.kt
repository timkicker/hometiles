package org.biglau.res

import java.io.File
import org.biglau.Quelltext
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
}
