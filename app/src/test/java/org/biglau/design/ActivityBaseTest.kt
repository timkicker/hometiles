package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * every screen inherits from `BigLauActivity`.
 *
 * three things hang on that one place that one never gets complete separately: the screen
 * **orientation** (PLAN.md 4.2 - `portrait` stood twelve times in the manifest before), the
 * **language** (`attachBaseContext`, or the screen shows the system's instead of the chosen
 * one) and the **rebuild** when the language changes.
 *
 * a new activity extending `ComponentActivity` loses all three silently - it looks perfectly
 * right on the developer's machine as long as system language and portrait hold there. that
 * kind of fault has caught the palette in this project before (see [PaletteScopeTest]).
 */
class ActivityBaseTest {

    private val sources: List<File> =
        Quelltext.files()

    private fun activityLines(): List<Pair<String, String>> =
        sources.flatMap { file ->
            Regex("""^\s*(?:internal\s+)?class\s+(\w*Activity)\s*:\s*([\w.]+)""", RegexOption.MULTILINE)
                .findAll(file.readText())
                .map { file.name to it.groupValues[1] + " : " + it.groupValues[2] }
                .toList()
        }

    @Test
    fun `every activity inherits from BigLauActivity`() {
        val foreign = activityLines()
            .filterNot { (file, _) -> file == "BigLauActivity.kt" }
            .filterNot { (_, line) -> line.endsWith(": BigLauActivity") }
            .map { "${it.first}: ${it.second}" }
        assertEquals(
            "these screens do not inherit from BigLauActivity and so lose orientation, " +
                "language and the rebuild on a language change: $foreign",
            emptyList<String>(),
            foreign,
        )
    }

    @Test
    fun `there are activities to check at all`() {
        // otherwise the rule would pass green because the pattern finds nothing any more.
        assertTrue("at least ten screens", activityLines().size >= 10)
    }

    @Test
    fun `BigLauActivity sets the orientation itself`() {
        val text = sources.first { it.name == "BigLauActivity.kt" }.readText()
        assertTrue("it sets requestedOrientation", text.contains("requestedOrientation"))
        assertTrue("it sets the language", text.contains("attachBaseContext"))
    }
}
