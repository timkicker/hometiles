package dev.kicker.hometiles.design

import java.io.File
import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a control with an empty action is a promise without cover.
 *
 * the "new message to ..." row appears when HomeTiles is opened through an `smsto:` link, looks
 * like a button, is highlighted, and had `onClick = {}`. whoever came from a link tapped it
 * and **nothing happened**.
 *
 * three further rows with an empty action turned up, all three mere information. those are a
 * fault too, if a quieter one: they swallow the tap silently and the screen reader announces
 * them as a control. `BigRow` therefore knows rows **without** an action - those are neither
 * clickable nor a button.
 */
class DeadControlTest {

    private val pattern = Regex("""on(Click|LongClick|Pick|Confirm|Accept)\s*=\s*\{\s*\}""")

    private fun files(): List<File> =
        Quelltext.files()

    @Test
    fun `no control with an empty action`() {
        val empty = mutableListOf<String>()
        files().forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                // explanations may name the rule without breaking it.
                if (!Quelltext.isCommentLine(line) && pattern.containsMatchIn(line)) {
                    empty += "${file.name}:${index + 1}: ${line.trim()}"
                }
            }
        }
        assertTrue(
            "these controls do nothing - either they get an action, or they must not be one " +
                "(BigRow without onClick):\n" + empty.joinToString("\n"),
            empty.isEmpty(),
        )
    }
}
