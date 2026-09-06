package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the startup way carries nothing that can wait.
 *
 * measured on the Jelly 2: two seconds from the tap to the visible home screen (debug build).
 * the first read of the configuration alone cost 186 ms - not because of the file (twelve
 * kilobytes) but because of the first use of the converter. it now runs alongside while
 * android brings the activity up, which is safe because `ConfigStore.get` is guarded against
 * two calls at once. that gained around 80 to 130 ms.
 */
class StartupTest {

    /** without comments: an explanation may name the rule without breaking it. */
    private val app = Quelltext.file("dev/kicker/hometiles/HomeTilesApp.kt")
        .readLines()
        .filterNot { Quelltext.isCommentLine(it) }
        .joinToString("\n")

    @Test
    fun `the configuration is not read on the startup thread`() {
        val beforeTheThread = Quelltext.cut(app, "", "Thread {")
        assertTrue("no thread of its own in the startup", "Thread {" in app)
        assertTrue(
            "ConfigStore is still built on the startup thread",
            "ConfigStore.get" !in beforeTheThread,
        )
    }

    @Test
    fun `the crash recorder stays at the front`() {
        // it costs two milliseconds and must stand before anything can crash - otherwise the
        // emergency mode has nothing to show at the next start.
        val beforeTheThread = Quelltext.cut(app, "", "Thread {")
        assertTrue("CrashRecorder is missing at the start", "CrashRecorder.get" in beforeTheThread)
    }
}
