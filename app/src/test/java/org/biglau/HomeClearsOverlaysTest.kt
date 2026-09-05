package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the home key clears away everything lying over the home screen.
 *
 * `onNewIntent` cleared **only** the folder, with a reasoning that holds for all of them:
 * whoever taps home does not want to keep standing in it. reproduced on the emulator: the
 * "call or write?" question stayed open through a home press, and so did the large label. a
 * question that survives the home key is a bolt.
 *
 * the rule counts the states, not the words: whoever adds a sixth overlay has to enter it
 * here, and that raises the question whether the home key clears it.
 */
class HomeClearsOverlaysTest {

    private val home = Quelltext.withoutComments("org/biglau/MainActivity.kt")

    /** state to what it lays over the home screen. */
    private val overlays = mapOf(
        "openFolder" to "the open folder",
        "popupLabelState" to "the large tile label",
        "contactChoice" to "call or write?",
        "lockedApp" to "the pin gate in front of a locked app",
        "phoneStateAsked" to "the explainer for the signal permission",
        // the menu key from PLAN.md 10.3.4 arrived later, and the rule reported it at once -
        // as its own comment had announced.
        "tileMenu" to "the menu key's list",
    )

    private val clearing = Quelltext.cut(
        home,
        from = "private fun closeOverlays() {",
        to = "\n    }",
    )

    @Test
    fun `home clears every overlay`() {
        val forgotten = overlays.filterKeys { it !in clearing }
        assertEquals(
            "closeOverlays() leaves something standing: " +
                forgotten.values.joinToString(", ") +
                ". whoever taps home wants the home screen.",
            emptyMap<String, String>(),
            forgotten,
        )
    }

    @Test
    fun `the home key calls it as well`() {
        val newIntent = Quelltext.cut(
            home,
            from = "override fun onNewIntent(",
            to = "\n    }",
        )
        assertTrue(
            "onNewIntent does not clear the overlays: $newIntent",
            "closeOverlays()" in newIntent,
        )
    }

    @Test
    fun `no overlay clears itself only singly`() {
        // the count is the actual rule: as many states, as many lines. a further overlay
        // stands out here before it survives a home key.
        val lines = clearing.lines().count { it.contains(".value") }
        assertEquals(
            "closeOverlays() clears $lines states, expected are " +
                "${overlays.size}: ${overlays.values.joinToString(", ")}",
            overlays.size,
            lines,
        )
    }
}
