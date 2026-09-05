package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * no screen reads from disk while it is being drawn.
 *
 * a file on the device does not show it. but the way `ImportActivity` describes - a backup
 * out of a cloud app or a mail attachment - runs through a foreign provider, which may fetch
 * it from the network first. then the screen stands still and android declares the app hung,
 * of all moments during the move to a new phone.
 *
 * the rule holds for the surface, not for the repositories: those may read, they do it in
 * the right thread.
 */
class MainThreadTest {

    private val slow = listOf(
        "contentResolver.openInputStream(",
        "contentResolver.openOutputStream(",
        "contentResolver.query(",
    )

    @Test
    fun `no activity reaches a provider outside an effect`() {
        val places = Quelltext.files()
            .filter { it.name.endsWith("Activity.kt") }
            .flatMap { file ->
                val lines = file.readLines()
                lines.withIndex()
                    .filter { (_, line) ->
                        val bare = line.trim()
                        slow.any { it in bare } && !Quelltext.isCommentLine(line)
                    }
                    .filterNot { (i, _) ->
                        // in the right thread, or in a function called only from an effect -
                        // both show as `Dispatchers.IO` within the twenty lines above.
                        lines.subList(maxOf(0, i - 20), i).any { "Dispatchers.IO" in it }
                    }
                    .map { (i, _) -> "${file.name}:${i + 1}" }
            }
        assertEquals(
            "here a screen reads from a provider without changing the thread. with a file " +
                "out of a cloud app that is network traffic on the main thread - the screen " +
                "stands until android declares the app hung.",
            emptyList<String>(),
            places,
        )
    }

    @Test
    fun `the import says that it is reading`() {
        val source = Quelltext.file("org/biglau/settings/ImportActivity.kt").readText()
        assertTrue(
            "the import reads without a loading state - then the screen stands empty, and " +
                "whoever sees nothing taps again.",
            "R.string.transfer_reading" in source && "loading" in source,
        )
    }
}
