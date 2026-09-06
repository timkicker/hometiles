package dev.kicker.hometiles.ui

import dev.kicker.hometiles.Quelltext
import dev.kicker.hometiles.data.Button
import dev.kicker.hometiles.tiles.TileEdits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a tile's self-chosen icon. `PLAN.md` 2.2 (`icon: IconRef?`) and 3.4.
 *
 * deriving every icon from the action mostly fits, but not always: the folder with the bank
 * business carries a card, not a folder.
 */
class IconCatalogueTest {

    @Test
    fun `every name in the catalogue has a picture too`() {
        val withoutPicture = IconCatalogue.NAMES.filter { IconCatalogue.vectorFor(it) == null }
        assertEquals(emptyList<String>(), withoutPicture)
    }

    /**
     * `PLAN.md` 3.6: no icon-only buttons without a label anywhere in the app. a wall of
     * icons without a word is a guessing game.
     */
    @Test
    fun `every icon has a word`() {
        val withoutWord = IconCatalogue.NAMES.filter { IconCatalogue.labelFor(it) == null }
        assertEquals(emptyList<String>(), withoutWord)
    }

    /** the same icon twice in the list would be the same choice twice. */
    @Test
    fun `no icon stands there twice`() {
        assertEquals(IconCatalogue.NAMES.size, IconCatalogue.NAMES.toSet().size)
    }

    @Test
    fun `the groups are not empty and have a heading`() {
        assertTrue(IconCatalogue.GROUPS.isNotEmpty())
        IconCatalogue.GROUPS.forEach { group ->
            assertTrue("group without icons", group.names.isNotEmpty())
            assertTrue("group without a heading", group.titleRes != 0)
        }
    }

    /**
     * an unknown name gives `null` and not a stand-in icon: the tile falls back on the
     * derived one. a backup from a later version must leave no empty tile behind.
     */
    @Test
    fun `an unknown name falls back`() {
        assertNull(IconCatalogue.vectorFor("GibtsNicht"))
        assertNull(IconCatalogue.vectorFor(null))
        assertNotNull(IconCatalogue.vectorFor(IconCatalogue.NAMES.first()))
    }

    /**
     * with icons switched off globally the row says so, otherwise the icon choice is a
     * setting with no visible effect. the row stays: the choice holds as soon as icons are
     * on again.
     */
    @Test
    fun `with icons switched off the reason stands beside it`() {
        val source = Quelltext.file("dev/kicker/hometiles/tiles/TileEditorActivity.kt").readText()
        // with the comma: without it the mark also hits the heading of the icon choice
        // further down, and which of the two places gets checked would be decided by the
        // order in the source.
        val row = Quelltext.cut(source, "R.string.editor_pick_icon),", "onClick")
        assertTrue("the hint is missing: $row", "editor_pick_icon_off" in row)
        assertTrue("the visibility is not read: $row", "LocalIconVisibility" in row)
    }

    @Test
    fun `automatic is no name`() {
        val withIcon = TileEdits.withIcon(Button(), "Home")
        assertEquals("Home", withIcon.iconName)
        assertNull(TileEdits.withIcon(withIcon, null).iconName)
        assertNull(TileEdits.withIcon(withIcon, "  ").iconName)
    }
}
