package dev.kicker.hometiles.design

import java.io.File
import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the setting is called text size - then the text has to grow along.
 *
 * it kept only half its promise: tiles, rows and headings grew (`BigRow` and `BigTile` read
 * `LocalTextScale`), the explaining text beside them did not. at 200 percent large buttons
 * stood next to small type - on exactly the sentences one wants to read enlarged.
 *
 * `bigSp` is the way there. sizes computed from the area (`dpSp`) are **not** meant: those
 * must not be scaled a second time. tried and taken back out: the dial pad's header at 200
 * percent showed only the first word, the rest lay outside the screen, and the hint below it
 * had vanished entirely.
 */
class TextScaleTest {

    /** the screens with lists - there is room to grow there because they scroll. */
    private val screens = Quelltext.files().filter { it.name.endsWith("Activity.kt") }

    private val fixedSize = Regex("""fontSize = \d+(\.\d+)?\.sp""")

    @Test
    fun `no fixed font size stands on the screens`() {
        val fixed = mutableListOf<String>()
        screens.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                if (fixedSize.containsMatchIn(line)) {
                    fixed += "${file.name}:${index + 1}: ${line.trim()}"
                }
            }
        }
        assertTrue(
            "these sizes do not follow the chosen text size - take bigSp(), or dpSp() when " +
                "the size comes from the area:\n" + fixed.joinToString("\n"),
            fixed.isEmpty(),
        )
    }

    @Test
    fun `bigSp exists and reads the setting`() {
        val source = Quelltext.file("dev/kicker/hometiles/ui/TextSizing.kt").readText()
        assertTrue("bigSp is missing", "fun bigSp(" in source)
        assertTrue("bigSp does not read the setting", "LocalTextScale.current" in source)
    }
}
