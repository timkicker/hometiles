package org.biglau.ui

import kotlinx.serialization.json.Json
import org.biglau.data.Appearance
import org.biglau.data.IconVisibility
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.2: „Icons anzeigen: ja / nein / nur wenn Platz".
 *
 * Die dritte Stufe ist auf drei Zoll die interessante. Eine 1×1-Kachel im 3×5-Raster ist
 * gut 60 dp hoch; nach der Beschriftung bleibt so wenig übrig, dass das Symbol zum Fleck
 * schrumpft — und ein Fleck sagt nichts, kostet aber die halbe Kachel.
 */
class IconRoomTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `immer heisst immer, auch wenn es eng wird`() {
        assertEquals(true, IconRoom.show(IconVisibility.ALWAYS, desiredDp = 40f, fittedDp = 16f))
    }

    @Test
    fun `nie heisst nie, auch wenn viel platz waere`() {
        assertEquals(false, IconRoom.show(IconVisibility.NEVER, desiredDp = 96f, fittedDp = 96f))
    }

    @Test
    fun `gequetscht heisst weg`() {
        assertEquals(false, IconRoom.show(IconVisibility.IF_ROOM, desiredDp = 40f, fittedDp = 30f))
        assertEquals(true, IconRoom.show(IconVisibility.IF_ROOM, desiredDp = 40f, fittedDp = 40f))
    }

    /**
     * Der Weg von der Zellgröße zur Entscheidung, in einem Stück: eine flache Kachel im
     * dichten Raster verliert das Symbol, eine normale behält es.
     */
    @Test
    fun `erst das dichte raster mit grossen symbolen laesst sie fallen`() {
        fun passt(breite: Float, hoehe: Float, prozent: Int): Boolean {
            val labelSp = labelSizeSp(breite, hoehe, userScale = 1f)
            val zone = labelZoneDp(hoehe, labelSp)
            return IconRoom.show(
                IconVisibility.IF_ROOM,
                iconSizeDp(breite, hoehe, prozent),
                iconSizeDp(breite, hoehe, prozent, labelZoneDp = zone),
            )
        }
        // 2x3 auf diesem Geraet: reichlich Platz, das Symbol bleibt.
        assertEquals(true, passt(breite = 165.6f, hoehe = 186.4f, prozent = 40))
        // 3x5 mit Vorgabegroesse: passt auch noch.
        assertEquals(true, passt(breite = 108f, hoehe = 105f, prozent = 40))
        // 3x8 - das dichteste Raster, das PLAN.md zulaesst - mit 60 Prozent: jetzt
        // muesste gequetscht werden, und die Beschriftung geht vor.
        assertEquals(false, passt(breite = 108f, hoehe = 66f, prozent = 60))
    }

    // Wie bei der Haptik: die alte Angabe steht noch in jeder frueher geschriebenen Datei.
    @Test
    fun `alte konfigurationen werden uebernommen`() {
        assertEquals(
            IconVisibility.ALWAYS,
            json.decodeFromString<Appearance>("""{"showIcons":true}""").icons,
        )
        assertEquals(
            IconVisibility.NEVER,
            json.decodeFromString<Appearance>("""{"showIcons":false}""").icons,
        )
    }

    @Test
    fun `withIcons haelt beide felder gleich`() {
        val ohne = Appearance().withIcons(IconVisibility.NEVER)
        assertEquals(false, ohne.showIcons)
        val nurPlatz = Appearance().withIcons(IconVisibility.IF_ROOM)
        assertEquals(true, nurPlatz.showIcons)
        assertEquals(IconVisibility.IF_ROOM, nurPlatz.icons)
    }

    @Test
    fun `die vorgabe zeigt symbole`() {
        assertEquals(IconVisibility.ALWAYS, Appearance().icons)
    }
}

/**
 * „Keine Symbole" gilt auch für die Ordner-Vorschau.
 *
 * Am Gerät standen die vier kleinen Symbole im Ordner noch da, während überall sonst
 * keine mehr waren. Das sieht nach einem Fehler aus, und man sucht ihn bei sich.
 */
class FolderPreviewIconsTest {

    private fun zeigtVorschau(icons: IconVisibility, gefuellt: Boolean): Boolean =
        gefuellt && icons != IconVisibility.NEVER

    @Test
    fun `ohne symbole bleibt der ordnername allein`() {
        assertEquals(false, zeigtVorschau(IconVisibility.NEVER, gefuellt = true))
    }

    @Test
    fun `sonst zeigt ein gefuellter ordner seinen inhalt`() {
        assertEquals(true, zeigtVorschau(IconVisibility.ALWAYS, gefuellt = true))
        assertEquals(true, zeigtVorschau(IconVisibility.IF_ROOM, gefuellt = true))
        assertEquals(false, zeigtVorschau(IconVisibility.ALWAYS, gefuellt = false))
    }
}
