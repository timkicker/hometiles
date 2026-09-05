package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * a screen change out of a folder closes the folder.
 *
 * a folder is an overlay: it does not change the screen, it lies over it, and so it owns the
 * display. changing the screen from a tile **inside** the folder otherwise happens behind
 * the overlay - and for the user nothing happens at all.
 *
 * reproduced on 04.09.2026: a "to screen 2" tile on the free slot in a folder, tapped. the
 * folder stayed open, the five tiles stayed where they were, nothing stirred. only closing
 * the folder put one on screen 2. whoever taps a second time instead changes a second time.
 *
 * it hit four ways at once - the "to screen" tile and the builtins for home screen, next
 * screen and previous screen. hence the rule stands not at one of the four but at the one
 * place all four go through.
 */
class FolderSwitchTest {

    private val activate = Quelltext.cut(
        Quelltext.withoutComments("org/biglau/MainActivity.kt"),
        from = "private fun activate(",
        to = "\n    private fun ",
    )

    @Test
    fun `activate changes the screen through one place only`() {
        val direct = Regex("goToScreen\\(").findAll(activate).count()
        assertEquals(
            "`goToScreen(` stands $direct times in activate(). exactly one of those calls " +
                "may be there - the one in `switchScreen`, which closes the folder first. " +
                "every further one changes the screen behind an open overlay, and the user " +
                "sees nothing of it.",
            1,
            direct,
        )
    }

    @Test
    fun `the one place closes the folder`() {
        val switchScreen = Quelltext.cut(
            activate,
            from = "val switchScreen: (String) -> Unit = {",
            to = "}",
        )
        assertEquals(
            "`switchScreen` does not close the folder: $switchScreen",
            1,
            Regex("openFolder\\.value = null").findAll(switchScreen).count(),
        )
    }
}
