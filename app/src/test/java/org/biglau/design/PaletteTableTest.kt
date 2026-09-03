package org.biglau.design

import java.io.File
import kotlin.math.abs
import org.biglau.ui.theme.Tokens
import org.biglau.ui.theme.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Farbtabelle in `PLAN.md` 3.3 stimmt mit den Konstanten überein.
 *
 * Der Abschnitt trägt den Satz „Kontrast ist eine Funktion, kein Geschmack" — und stand
 * bis zum 3.9.2026 an drei von sechs Zeilen falsch da: Blau mit 5,65:1 statt 5,63:1, Grün
 * mit 3,51 statt 3,53, Magenta mit 3,50 statt 3,51. Alles Kleinigkeiten, und genau deshalb
 * gefährlich: eine Tabelle mit vier Stellen sieht nachgerechnet aus.
 *
 * `ContrastTest` prüft die **Schwellen** (hebt sich ab, ist lesbar). Diese Regel prüft die
 * **Zahlen im Plan** — also das, was jemand liest, der die Palette ändern will und wissen
 * muss, wo der Spielraum endet.
 */
class PaletteTableTest {

    private val plan = File("../PLAN.md").readText()

    /** Name, Hex, Verhältnis gegen den Grund, Verhältnis für weissen Text. */
    private data class Zeile(val name: String, val farbe: Long, val grund: Double, val text: Double)

    private fun tabelle(): List<Zeile> = Regex(
        // `\w` ist in Kotlin ASCII - „Grün" und „Türkis" fielen damit aus der Tabelle,
        // und die Regel meldete „nur vier Zeilen" statt der eigentlichen Sache.
        """^\| ([^|]+?) \| #([0-9A-F]{6}) \| (\d,\d\d):1 \| (\d,\d\d):1 \|$""",
        RegexOption.MULTILINE,
    ).findAll(plan).map {
        Zeile(
            name = it.groupValues[1],
            farbe = 0xFF000000L or it.groupValues[2].toLong(16),
            grund = it.groupValues[3].replace(',', '.').toDouble(),
            text = it.groupValues[4].replace(',', '.').toDouble(),
        )
    }.toList()

    @Test
    fun `die Tabelle hat sechs Zeilen und nennt genau die Palettenfarben`() {
        val zeilen = tabelle()
        assertEquals("Die Farbtabelle in PLAN.md 3.3 ist nicht mehr zu finden", 6, zeilen.size)
        assertEquals(
            "Die Farben im Plan sind nicht mehr die der dunklen Palette",
            Tokens.DARK_TILES,
            zeilen.map { it.farbe },
        )
    }

    @Test
    fun `jede angegebene Zahl stimmt auf zwei Stellen`() {
        tabelle().forEach { zeile ->
            val grund = contrastRatio(zeile.farbe, Tokens.DARK_BACKGROUND)
            val text = contrastRatio(0xFFFFFFFFL, zeile.farbe)
            assertTrue(
                "${zeile.name} gegen den Grund: Plan sagt ${zeile.grund}, gerechnet " +
                    "${"%.4f".format(grund)}",
                abs(grund - zeile.grund) < 0.006,
            )
            assertTrue(
                "${zeile.name}, weisser Text: Plan sagt ${zeile.text}, gerechnet " +
                    "${"%.4f".format(text)}",
                // 0,006 und nicht 0,005: Blau liegt mit 5,6250 genau auf der Grenze
                // zwischen 5,62 und 5,63. Eine Regel, die auf einer Rundungsgrenze steht,
                // faellt irgendwann wegen der Rundung und nicht wegen der Sache.
                abs(text - zeile.text) < 0.006,
            )
        }
    }

    /** Auch die beiden Zahlen im Fliesstext darunter. */
    @Test
    fun `weiss auf den beiden Untergruenden stimmt`() {
        assertTrue("19,8:1 steht nicht mehr im Plan", "19,8:1" in plan)
        assertTrue("18,1:1 steht nicht mehr im Plan", "18,1:1" in plan)
        assertEquals(19.8, contrastRatio(0xFFFFFFFFL, Tokens.DARK_BACKGROUND), 0.05)
        assertEquals(18.1, contrastRatio(0xFFFFFFFFL, Tokens.DARK_EMPTY_TILE), 0.05)
    }

    /**
     * Und die Themen-Tabelle darüber - jede Farbe, die dort steht, ist eine Konstante.
     *
     * Sie war am 3.9.2026 an der hellen Zeile gleich dreifach falsch: die leere Kachel stand
     * als `#FFFFFF` da (wirklich `#E8EAEC`), ihr Rand als `#BDBDBD` (wirklich `#8A8A8A`), und
     * die Breite als 1 dp (wirklich 2 dp). Der dunklen Zeile fehlte ihr Rand ganz. Solche
     * Werte liest jemand ab, der eine Farbe nachbauen oder anpassen will - und bekommt dann
     * ein Ergebnis, das *fast* stimmt, was schlimmer ist als eines, das offensichtlich
     * falsch ist.
     */
    @Test
    fun `die Themen-Tabelle nennt die tatsaechlichen Farben`() {
        fun zeile(anfang: String): String =
            plan.lineSequence().firstOrNull { it.startsWith(anfang) }
                ?: throw AssertionError("Zeile fehlt in PLAN.md 3.3: $anfang")

        fun hex(wert: Long) = "#%06X".format(wert and 0xFFFFFFL)

        val dunkel = zeile("| **Dunkel**")
        listOf(
            Tokens.DARK_BACKGROUND, Tokens.DARK_EMPTY_TILE, Tokens.DARK_EMPTY_TILE_BORDER,
            Tokens.DARK_ON_BACKGROUND, Tokens.DARK_ACCENT, Tokens.DARK_DANGER,
        ).forEach { assertTrue("dunkel: ${hex(it)} fehlt in der Zeile", hex(it) in dunkel) }

        val hell = zeile("| **Hell**")
        listOf(
            Tokens.LIGHT_BACKGROUND, Tokens.LIGHT_EMPTY_TILE, Tokens.LIGHT_EMPTY_TILE_BORDER,
            Tokens.LIGHT_ON_BACKGROUND, Tokens.LIGHT_ACCENT, Tokens.LIGHT_DANGER,
        ).forEach { assertTrue("hell: ${hex(it)} fehlt in der Zeile", hex(it) in hell) }

        val kontrast = zeile("| **Kontrast**")
        listOf(
            Tokens.CONTRAST_BACKGROUND, Tokens.CONTRAST_INK, Tokens.CONTRAST_DANGER,
        ).forEach { assertTrue("kontrast: ${hex(it)} fehlt in der Zeile", hex(it) in kontrast) }
    }
}
