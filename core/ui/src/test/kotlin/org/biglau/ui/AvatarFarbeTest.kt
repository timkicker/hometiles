package org.biglau.ui

import org.biglau.data.ThemeName
import org.biglau.ui.theme.Tokens
import org.biglau.ui.theme.contrastRatio
import org.biglau.ui.theme.paletteFor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Das Kontaktbild ohne Foto steht auf einer Listenzeile, nicht auf dem Hintergrund.
 *
 * `ContactAvatar` nimmt eine Kachelfarbe und schreibt die Initialen darauf. Beide Paare
 * waren geprüft — Kachel gegen **Hintergrund** und Beschriftung gegen Kachel. Nur liegt der
 * Avatar am Gerät in einer `BigRow`, also auf `surfaceDefault`, und dieses Paar sah bis zum
 * 04.09.2026 niemand an.
 *
 * Es hält: 3,19 im dunklen und 6,35 im hellen Thema, gegen die Schwelle 3,0. Im dunklen ist
 * das knapp, und knapp heisst hier: wer die Füllung der leeren Kachel um zwei Stufen
 * aufhellt, macht die Avatare unsichtbar, ohne dass eine Regel etwas sagt. Ab jetzt sagt
 * eine.
 *
 * Im Kontrastthema sind alle Kachelplätze der Hintergrund; dort trägt die gelbe Schrift den
 * Avatar, und die Fläche darf gleich sein.
 */
class AvatarFarbeTest {

    @Test
    fun `das kontaktbild hebt sich von der zeile ab`() {
        val schwach = mutableListOf<String>()
        themenUndSystem()
            .filter { it.first != ThemeName.HIGH_CONTRAST }
            .forEach { (thema, systemIsDark) ->
                val palette = paletteFor(thema, systemIsDark)
                val zeile = palette.surfaceDefault.fill.value.toLong() shr 32
                palette.tiles.forEachIndexed { i, farbe ->
                    val wert = contrastRatio(farbe.value.toLong() shr 32, zeile)
                    if (wert < Tokens.MIN_TILE_ON_BACKGROUND) {
                        schwach += "$thema/Avatar$i: %.2f".format(wert)
                    }
                }
            }
        assertEquals(
            "Ein Kontaktbild ohne Foto verschwindet in seiner Zeile",
            emptyList<String>(),
            schwach,
        )
    }

    @Test
    fun `die initialen sind auf jedem kontaktbild lesbar`() {
        val schwach = mutableListOf<String>()
        themenUndSystem().forEach { (thema, systemIsDark) ->
            val palette = paletteFor(thema, systemIsDark)
            palette.tiles.forEachIndexed { i, farbe ->
                val wert = contrastRatio(
                    palette.onTile.value.toLong() shr 32,
                    farbe.value.toLong() shr 32,
                )
                if (wert < Tokens.MIN_LABEL_ON_TILE) schwach += "$thema/Avatar$i: %.2f".format(wert)
            }
        }
        assertEquals(emptyList<String>(), schwach)
    }
}
