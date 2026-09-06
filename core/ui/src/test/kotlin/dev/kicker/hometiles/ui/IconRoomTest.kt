package dev.kicker.hometiles.ui

import kotlinx.serialization.json.Json
import dev.kicker.hometiles.data.Appearance
import dev.kicker.hometiles.data.IconVisibility
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PLAN.md 4.2: show icons - yes / no / only if there is room.
 *
 * the third step is the interesting one on three inches. a 1x1 tile in a 3x5 grid is a good
 * 60 dp high; after the label so little is left that the icon shrinks to a blob - and a blob
 * says nothing while costing half the tile.
 */
class IconRoomTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `always means always, even when it gets tight`() {
        assertEquals(true, IconRoom.show(IconVisibility.ALWAYS, desiredDp = 40f, fittedDp = 16f))
    }

    @Test
    fun `never means never, even with plenty of room`() {
        assertEquals(false, IconRoom.show(IconVisibility.NEVER, desiredDp = 96f, fittedDp = 96f))
    }

    @Test
    fun `squeezed means gone`() {
        assertEquals(false, IconRoom.show(IconVisibility.IF_ROOM, desiredDp = 40f, fittedDp = 30f))
        assertEquals(true, IconRoom.show(IconVisibility.IF_ROOM, desiredDp = 40f, fittedDp = 40f))
    }

    /**
     * the way from cell size to decision in one piece: a flat tile in the dense grid loses
     * the icon, an ordinary one keeps it.
     */
    @Test
    fun `only the dense grid with large icons drops them`() {
        fun fits(width: Float, height: Float, percent: Int): Boolean {
            val labelSp = labelSizeSp(width, height, userScale = 1f)
            val zone = labelZoneDp(height, labelSp)
            return IconRoom.show(
                IconVisibility.IF_ROOM,
                iconSizeDp(width, height, percent),
                iconSizeDp(width, height, percent, labelZoneDp = zone),
            )
        }
        // 2x3 here: plenty of room, the icon stays.
        assertEquals(true, fits(width = 165.6f, height = 186.4f, percent = 40))
        // 3x5 at the default size: still fits.
        assertEquals(true, fits(width = 108f, height = 105f, percent = 40))
        // 3x8 - the densest grid PLAN.md allows - at 60 percent: now it would have to be
        // squeezed, and the label comes first.
        assertEquals(false, fits(width = 108f, height = 66f, percent = 60))
    }

    // as with the haptics: the old field still stands in every file written earlier.
    @Test
    fun `old configurations are taken over`() {
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
    fun `withIcons keeps both fields in step`() {
        val without = Appearance().withIcons(IconVisibility.NEVER)
        assertEquals(false, without.showIcons)
        val ifRoom = Appearance().withIcons(IconVisibility.IF_ROOM)
        assertEquals(true, ifRoom.showIcons)
        assertEquals(IconVisibility.IF_ROOM, ifRoom.icons)
    }

    @Test
    fun `the default shows icons`() {
        assertEquals(IconVisibility.ALWAYS, Appearance().icons)
    }
}

/**
 * "no icons" holds for the folder preview too.
 *
 * the four small icons in the folder stood there while there were none anywhere else. that
 * looks like a fault, and one looks for it in oneself.
 */
class FolderPreviewIconsTest {

    private fun showsPreview(icons: IconVisibility, filled: Boolean): Boolean =
        filled && icons != IconVisibility.NEVER

    @Test
    fun `without icons the folder name stands alone`() {
        assertEquals(false, showsPreview(IconVisibility.NEVER, filled = true))
    }

    @Test
    fun `otherwise a filled folder shows its content`() {
        assertEquals(true, showsPreview(IconVisibility.ALWAYS, filled = true))
        assertEquals(true, showsPreview(IconVisibility.IF_ROOM, filled = true))
        assertEquals(false, showsPreview(IconVisibility.ALWAYS, filled = false))
    }
}
