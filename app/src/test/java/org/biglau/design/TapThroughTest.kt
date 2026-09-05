package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a screen lying over another swallows the taps beside it.
 *
 * the home screen's overlays - folder, large label, pin gate, contact choice, signal
 * explainer - do not **replace** it, they lie on it. a tap in their margin or a gap went
 * through: measured on the jelly 2 on 04.09.2026, a tap at (14, 250) with a folder open
 * started the contacts tile. after the change the same tap stays in the folder.
 *
 * everywhere else the question replaces the content - the call log even names the reason
 * beside it, that the question covers the list so nobody taps another row in passing. only
 * the home screen has real overlays, and only there does the question arise.
 */
class TapThroughTest {

    private val home = Quelltext.withoutComments("org/biglau/MainActivity.kt")

    /** the overlays and what shows that they keep the tap. */
    private val overlays = mapOf(
        "FolderOverlay" to "absorbTouches",
        "SignalPermissionExplainer" to "absorbTouches",
        // these two swallow it by using it: a tap beside them closes them. the same thing,
        // only with an effect.
        "LabelPopup" to "clickable",
        "ContactChoice" to "clickable",
    )

    @Test
    fun `every overlay keeps the tap to itself`() {
        val open = overlays.filterNot { (name, means) -> means in section(name) }
        assertEquals(
            "these overlays let a tap beside their content through to the home screen below " +
                "- a tile nobody can see then starts: ${open.keys}",
            emptyMap<String, String>(),
            open,
        )
    }

    /** and the gate that comes from outside, likewise. */
    @Test
    fun `the pin gate swallows taps beside it`() {
        assertTrue(
            "a tap in the pin gate's margin starts the tile below. a gate one taps past is " +
                "no gate.",
            "absorbTouches()" in Quelltext.withoutComments("org/biglau/ui/PinGate.kt"),
        )
    }

    /**
     * a function's body, up to the next one - or to the end of the file.
     *
     * `SignalPermissionExplainer` is the last in the file; a fixed end mark would there be the
     * end mark that does not exist, and `Quelltext.cut` would rightly fall over.
     */
    private fun section(name: String): String {
        val from = Quelltext.cut(home, "private fun $name(")
        val next = from.indexOf("\nprivate fun ")
        return if (next < 0) from else from.take(next)
    }
}
