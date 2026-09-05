package org.biglau.design

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a field only ever filled and never read is a forgotten promise.
 *
 * `CallView.otherCallWaiting` was set dutifully by the service on every second call, and
 * **not one line of the surface ever read it**. it showed only on the emulator: a call came
 * in during a conversation, the screen showed the new caller alone, and the first call was
 * afterwards neither to be seen nor to be reached.
 *
 * this is the sister of [DeadLogicTest]: functions nobody calls there, values nobody reads
 * here. both look like finished work in the source.
 *
 * only fields in the head of a `data class` are checked - the values passed from one place to
 * another. filling one (`field = value` as a named parameter) does not count as reading.
 */
class DeadFieldTest {

    /** what may be filled without being read. only with a reason. */
    private val reasonedExceptions = mapOf(
        // stands in every saved file and is checked while reading the raw json
        // (ConfigTransfer), not through this field.
        "LauncherConfig.version" to "is read from the json itself while loading",
    )

    private fun files(): List<File> =
        Quelltext.files()

    /** every field in the head of a `data class`, as "class.field" with its place. */
    private fun fields(): List<Triple<String, String, File>> {
        val found = mutableListOf<Triple<String, String, File>>()
        files().forEach { file ->
            var cls: String? = null
            file.readLines().forEach { line ->
                Regex("""^(?:@\w+\s+)?data class (\w+)""").find(line.trim())?.let {
                    // a one-line `data class X(val a: Int)` has no multi-line head - it would
                    // otherwise own everything standing after it in the body.
                    cls = if (line.trimEnd().endsWith(")")) null else it.groupValues[1]
                }
                // the head ends with the closing bracket at the start of a line; after that
                // the body or the next declaration begins, and a `val` there is not part of it.
                if (Regex("""^\)""").containsMatchIn(line) ||
                    Regex("""^(object|class|enum|sealed|fun|interface)\b""")
                        .containsMatchIn(line)
                ) {
                    cls = null
                }
                val field = Regex("""^ {4}val (\w+):""").find(line) ?: return@forEach
                cls?.let { found += Triple(it, field.groupValues[1], file) }
            }
        }
        return found
    }

    /** a line that only fills the field: `field = value` as a named parameter. */
    private fun onlyFilled(line: String, field: String): Boolean =
        Regex("""^\s*$field = """).containsMatchIn(line)

    private fun declaration(line: String, field: String): Boolean =
        Regex("""^\s*(?:val|var) $field:""").containsMatchIn(line)

    @Test
    fun `every field of a data class is read too`() {
        val lines = files().flatMap { it.readLines() }
        val dead = mutableListOf<String>()
        fields().forEach { (cls, field, _) ->
            val name = "$cls.$field"
            if (name in reasonedExceptions) return@forEach
            val read = lines.any { line ->
                Regex("""(^|[^\w.])$field\b""").containsMatchIn(line) &&
                    !declaration(line, field) &&
                    !onlyFilled(line, field) ||
                    Regex("""\.$field\b""").containsMatchIn(line)
            }
            if (!read) dead += name
        }
        assertTrue(
            "these fields are filled and never read:\n" + dead.joinToString("\n"),
            dead.isEmpty(),
        )
    }
}
