package dev.kicker.hometiles.phone

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * issue #21: on a tablet lying on its side the caller's photo never showed during a call.
 *
 * the arithmetic lives in CallerPhotoSizeTest. this holds the screen to it: beside the
 * buttons the photo must not reserve their rows, or it is dropped exactly as before.
 */
class CallPhotoBesideTest {

    private val screen = File(Quelltext.appRoot, "dev/kicker/hometiles/phone/InCallActivity.kt").readText()

    @Test
    fun `the call screen asks whether it is wide`() {
        assertTrue(screen.contains("CallerPhotoSize.beside("))
    }

    @Test
    fun `beside the buttons the photo reserves none of their rows`() {
        assertTrue(screen.contains("buttons = if (beside) 0 else rows.size"))
    }

    @Test
    fun `the room for the photo is the room the screen was given`() {
        // `screenHeightDp` leaves the bars out on android 11 and 12 and counts them in later.
        // subtracting them by hand took 72 dp too much there and dropped the photo.
        assertTrue(!screen.contains(".screenHeightDp"))
        assertTrue(screen.contains("val usableHeightDp = boxHeight.value"))
    }

    @Test
    fun `beside the buttons the keypad does not take the photo away`() {
        assertTrue(screen.contains("if (showKeypad && !beside) 0f"))
    }
}
