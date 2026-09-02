package org.biglau.ui

import java.io.File
import org.biglau.ui.theme.Tokens
import org.biglau.ui.theme.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Warnfarbe gehört nicht auf eine Kachel.
 *
 * Gefunden am Bildschirm: bei null Balken stand „4G" in Rot auf der orangen Empfangskachel.
 * Nachgemessen sind das **1,78 zu 1** — die App verlangt für Beschriftungen auf Kacheln
 * 4,5 (`PLAN.md` 3.3). Dasselbe galt für den Ladestand: bei neun Prozent wurde die Zahl rot
 * und damit ausgerechnet dann schwer zu lesen, wenn sie zählt. Das ist der schlimmste Fall
 * dieser Sorte: eine Warnung, die sich selbst versteckt.
 *
 * Rot bleibt richtig auf dem **Hintergrund** — dort steht es gegen fast Schwarz und ist
 * geprüft. Auf einer Kachel steht es gegen eine der sechs Kachelfarben, und gegen die kommt
 * es in keinem Thema über 1,8.
 *
 * Getragen wird die Warnung jetzt von der Sache selbst: neun Prozent sind neun Prozent,
 * null von vier Balken sind null von vier — und für den Screenreader steht „schwach" als
 * Wort in der Ansage.
 */
class TileDangerTest {

    /** Die Dateien, die *auf* einer Kachel zeichnen. */
    private val aufKacheln = listOf(
        "src/main/java/org/biglau/ui/InfoTiles.kt",
        "src/main/java/org/biglau/ui/BigTile.kt",
        "src/main/java/org/biglau/ui/WidgetTile.kt",
        "src/main/java/org/biglau/ui/HomeScreenView.kt",
    ).map(::File)

    @Test
    fun `die Warnfarbe faellt auf jedem Kachelton durch`() {
        listOf(
            Triple("dunkel", Tokens.DARK_DANGER, Tokens.DARK_TILES),
            Triple("hell", Tokens.LIGHT_DANGER, Tokens.LIGHT_TILES),
        ).forEach { (thema, danger, tiles) ->
            tiles.forEach { ton ->
                val verhaeltnis = contrastRatio(danger, ton)
                assertTrue(
                    "Im Thema $thema käme die Warnfarbe auf ${ton.toString(16)} auf " +
                        "$verhaeltnis zu 1 — steigt das je über ${Tokens.MIN_LABEL_ON_TILE}, " +
                        "darf die Regel darunter weg",
                    verhaeltnis < Tokens.MIN_LABEL_ON_TILE,
                )
            }
        }
    }

    @Test
    fun `keine Kachelzeichnung greift zur Warnfarbe`() {
        val treffer = aufKacheln.flatMap { datei ->
            datei.readLines().mapIndexedNotNull { index, zeile ->
                if ("palette.danger" in zeile) "${datei.name}:${index + 1}" else null
            }
        }
        assertEquals(emptyList<String>(), treffer)
    }
}
