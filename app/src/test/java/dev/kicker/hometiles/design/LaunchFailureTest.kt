package dev.kicker.hometiles.design

import java.io.File
import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * whoever returns "false" has to be heard.
 *
 * `AppRepository.launch` reported an uninstalled app properly with `false` - only nobody
 * looked. the tile kept the name, tapping did nothing, and nothing explained why. for
 * someone unsure whether they are operating the phone correctly, a tile that silently does
 * nothing is worse than an error message.
 *
 * the rule holds for every launch function that reports its failure.
 */
class LaunchFailureTest {

    private val checked = listOf(".launch(")

    private fun files(): List<File> =
        Quelltext.files()

    @Test
    fun `every launch attempt reads its result`() {
        val unchecked = mutableListOf<String>()
        files().forEach { file ->
            // the function itself does not count as a call site.
            if (file.name == "AppRepository.kt" || file.name == "ShortcutRepository.kt") return@forEach
            file.readLines().forEachIndexed { index, line ->
                val call = checked.any { it in line }
                if (!call) return@forEachIndexed
                // only the repositories' launch functions, not the coroutine `launch`.
                if (!line.contains("packageName")) return@forEachIndexed
                // the returned value may also be the value of a `when` assigned to a `val` a
                // few lines above.
                val lines = file.readLines()
                val read = line.contains("if (!") ||
                    line.contains("val ") ||
                    line.contains("return ") ||
                    lines.subList(maxOf(0, index - 4), index)
                        .any { Regex("""(val \w+ =|return) when""").containsMatchIn(it) }
                if (!read) unchecked += "${file.name}:${index + 1}: ${line.trim()}"
            }
        }
        assertTrue(
            "these launch attempts throw their result away - the tile would then silently " +
                "do nothing:\n" + unchecked.joinToString("\n"),
            unchecked.isEmpty(),
        )
    }
}
