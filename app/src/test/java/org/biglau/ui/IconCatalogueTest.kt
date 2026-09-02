package org.biglau.ui

import org.biglau.data.Button
import org.biglau.tiles.TileEdits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Das selbst gewählte Symbol einer Kachel. `PLAN.md` 2.2 („`icon: IconRef?`") und 3.4.
 *
 * Die App leitete jedes Symbol aus der Aktion ab. Das trifft meistens, aber nicht immer: der
 * Ordner mit den Bankgeschäften trägt keinen Ordner, sondern eine Karte. Der Plan sah die
 * Wahl von Anfang an vor; im Editor gab es sie nicht.
 */
class IconCatalogueTest {

    @Test
    fun `jeder Name im Katalog hat auch ein Bild`() {
        val ohneBild = IconCatalogue.NAMES.filter { IconCatalogue.vectorFor(it) == null }
        assertEquals(emptyList<String>(), ohneBild)
    }

    /**
     * `PLAN.md` 3.6: „Keine reinen Icon-Buttons ohne Label irgendwo in der App."
     *
     * Eine Wand aus Symbolen ohne Wort ist ein Ratespiel - für jemanden, der schlecht sieht,
     * und für einen Screenreader erst recht.
     */
    @Test
    fun `jedes Symbol hat ein Wort`() {
        val ohneWort = IconCatalogue.NAMES.filter { IconCatalogue.labelFor(it) == null }
        assertEquals(emptyList<String>(), ohneWort)
    }

    /** Zweimal dasselbe Symbol in der Liste hiesse zweimal dieselbe Wahl. */
    @Test
    fun `kein Symbol steht zweimal da`() {
        assertEquals(IconCatalogue.NAMES.size, IconCatalogue.NAMES.toSet().size)
    }

    @Test
    fun `die Gruppen sind nicht leer und haben eine Ueberschrift`() {
        assertTrue(IconCatalogue.GROUPS.isNotEmpty())
        IconCatalogue.GROUPS.forEach { gruppe ->
            assertTrue("Gruppe ohne Symbole", gruppe.names.isNotEmpty())
            assertTrue("Gruppe ohne Überschrift", gruppe.titleRes != 0)
        }
    }

    /**
     * Ein unbekannter Name gibt `null` und nicht etwa ein Ersatzsymbol: die Kachel fällt
     * dann auf das abgeleitete zurück. Eine Sicherung aus einer späteren Fassung darf keine
     * leere Kachel hinterlassen.
     */
    @Test
    fun `ein unbekannter Name faellt zurueck`() {
        assertNull(IconCatalogue.vectorFor("GibtsNicht"))
        assertNull(IconCatalogue.vectorFor(null))
        assertNotNull(IconCatalogue.vectorFor(IconCatalogue.NAMES.first()))
    }

    /**
     * Sind die Symbole global abgeschaltet, sagt die Zeile es.
     *
     * Sonst waere die Symbolwahl eine Einstellung ohne sichtbare Wirkung: der Nutzer waehlt
     * ein Herz und auf der Kachel passiert nichts. Die Zeile bleibt trotzdem stehen - die
     * Wahl gilt, sobald die Symbole wieder an sind.
     */
    @Test
    fun `bei ausgeschalteten Symbolen steht der Grund daneben`() {
        val quelle = java.io.File("src/main/java/org/biglau/tiles/TileEditorActivity.kt").readText()
        val zeile = quelle.substringAfter("R.string.editor_pick_icon)").substringBefore("onClick")
        assertTrue("Der Hinweis fehlt: $zeile", "editor_pick_icon_off" in zeile)
        assertTrue("Die Sichtbarkeit wird nicht gelesen: $zeile", "LocalIconVisibility" in zeile)
    }

    @Test
    fun `automatisch heisst kein Name`() {
        val mitSymbol = TileEdits.withIcon(Button(), "Home")
        assertEquals("Home", mitSymbol.iconName)
        assertNull(TileEdits.withIcon(mitSymbol, null).iconName)
        assertNull(TileEdits.withIcon(mitSymbol, "  ").iconName)
    }
}
