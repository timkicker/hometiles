package org.biglau

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a mode one cannot see is a trap.
 *
 * edit mode exists for those who cannot manage the long press: a short tap then opens the
 * tile editor instead of the app. so that this is no surprise a banner stands at the top, and
 * it is at the same time the way out, since tapping it ends the mode.
 *
 * reproduced on the jelly 2 on 04.09.2026: from the open folder into the settings, "change
 * tiles" there, back - and the folder still stood open, without the banner. the mode was on
 * (the next tap opened the editor, on the right tile even), but nothing said so, and the way
 * out lay under the same overlay. the banner stands in the home screen's column, which with a
 * folder open is not only covered but silenced by `clearAndSetSemantics` - so it was neither
 * to be seen nor to be heard.
 *
 * hence the overlay gets a banner of its own. not "close the folder then": edit mode **works**
 * in the folder, and the very people it is built for would otherwise never reach the tiles
 * inside one.
 */
class EditBannerTest {

    private val home = Quelltext.withoutComments("org/biglau/MainActivity.kt")

    @Test
    fun `the folder shows the banner itself`() {
        val call = Quelltext.cut(
            home,
            // more precise than `FolderOverlay(`: the menu key's list uses the same frame, so
            // that mark stood there twice. the folder is meant, so the cut is at its name.
            from = "                        name = folder.name,",
            to = "                    ) {",
        )
        assertTrue(
            "the folder overlay gets no edit banner: $call",
            "banner" in call && "editMode" in call,
        )
    }

    @Test
    fun `the banner stands in both places`() {
        val howOften = Regex("EditModeBanner \\{").findAll(home).count()
        assertTrue(
            "EditModeBanner is shown $howOften times. twice it must be: once on the home " +
                "screen and once in the folder overlay, which otherwise covers it.",
            howOften >= 2,
        )
    }
}
