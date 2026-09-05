package org.biglau.res

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the home screen must be opaque - otherwise the home key leads nowhere.
 *
 * on the Jelly 2 the previous app stayed on the screen when going home. the task dump said
 * `type=home ... translucent=true`: a translucent home task does not cover the app behind it.
 * on the emulator, same build, it said `translucent=false` - the difference came from the
 * manufacturer's theme.
 *
 * so the opacity now stands in the app's own theme instead of being inherited. for a home
 * screen that is no detail: the home key is the way back that someone relies on who finds
 * their way nowhere else.
 */
class HomeOpaqueTest {

    private val theme = File("src/main/res/values/themes.xml").readText()

    @Test
    fun `the theme says expressly that it is not translucent`() {
        assertTrue(
            "windowIsTranslucent is missing",
            """<item name="android:windowIsTranslucent">false</item>""" in theme,
        )
        assertTrue(
            "windowIsFloating is missing",
            """<item name="android:windowIsFloating">false</item>""" in theme,
        )
    }

    @Test
    fun `it shows no wallpaper through either`() {
        // windowShowWallpaper makes the task translucent as well - and BigLau paints its own
        // background.
        assertTrue(
            "windowShowWallpaper is missing",
            """<item name="android:windowShowWallpaper">false</item>""" in theme,
        )
    }

    @Test
    fun `the window background is an opaque colour`() {
        val line = theme.lines().first { "android:windowBackground" in it }
        assertTrue("no colour value: $line", Regex("""#[0-9a-fA-F]{6}""").containsMatchIn(line))
        assertTrue("with alpha instead of opaque: $line", !Regex("""#[0-9a-fA-F]{8}""").containsMatchIn(line))
    }
}
