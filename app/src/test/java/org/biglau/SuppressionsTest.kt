package org.biglau

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * every `@Suppress` line says why it stands there.
 *
 * a suppression switches off a warning someone once thought important. set without a word it
 * leaves a decision nobody can check - and in two years nobody dares take it away. on
 * 3.9.2026 ten such lines stood in the program, none of them with a reason.
 *
 * `"unused"` is not allowed at all: what nobody calls belongs gone, not silenced.
 */
class SuppressionsTest {

    private fun places(): List<Triple<String, Int, String>> =
        Quelltext.files().flatMap { file ->
            val lines = file.readLines()
            lines.withIndex()
                .filter { (_, line) -> line.trimStart().startsWith("@Suppress") }
                .map { (i, line) -> Triple(file.name, i + 1, line.trim()) }
                .map { (name, number, line) ->
                    Triple("$name:$number", number, lines.getOrElse(number - 2) { "" }.trim() + "|" + line)
                }
        }

    @Test
    fun `every suppression names its reason`() {
        val without = places()
            .filter { (_, _, around) -> !Quelltext.isCommentLine(Quelltext.cut(around, "", "|")) }
            .map { it.first }
        assertEquals(
            "a @Suppress line without a comment above it. write down which warning is " +
                "switched off and why - otherwise nobody takes it away later.",
            emptyList<String>(),
            without,
        )
    }

    @Test
    fun `nothing is silenced as unused`() {
        val silent = places()
            .filter { (_, _, around) -> "\"unused\"" in Quelltext.cut(around, "|") }
            .map { it.first }
        assertEquals(
            "what nobody calls is deleted, not quietened with @Suppress(\"unused\").",
            emptyList<String>(),
            silent,
        )
    }
}
