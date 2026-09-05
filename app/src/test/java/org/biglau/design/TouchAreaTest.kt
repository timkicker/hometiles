package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * two promises from `PLAN.md` 3.6 nobody had checked.
 *
 * **all dialogs as full-screen big-button dialogs, not as a material AlertDialog.** an
 * `AlertDialog` brings small text buttons that cannot be enlarged - on a device for poor
 * eyes that is exactly the button one misses.
 *
 * **no element under 48 dp.** measured on 03.09.2026: there is none. the only suspect was a
 * 32 dp icon **inside** a 56 dp area - the area counts, not the picture in it. that is why
 * this rule looks at the modifier chain that also carries `clickable`, and not at every size
 * nearby.
 */
class TouchAreaTest {

    @Test
    fun `no material AlertDialog`() {
        val hits = Quelltext.files()
            .filter { file ->
                file.readLines().any { line ->
                    "AlertDialog" in line && !Quelltext.isCommentLine(line)
                }
            }
            .map { it.name }
        assertEquals(
            "PLAN.md 3.6: dialogs are full-screen screens with large buttons. an " +
                "AlertDialog brings small text buttons that cannot be enlarged.",
            emptyList<String>(),
            hits,
        )
    }

    @Test
    fun `no clickable area under 48 dp`() {
        val pattern = Regex("""\.size\((\d+(?:\.\d+)?)\.dp\)""")
        val tooSmall = Quelltext.files().flatMap { file ->
            val lines = file.readLines()
            lines.withIndex()
                .filter { (_, line) -> ".clickable(" in line || ".combinedClickable(" in line }
                .mapNotNull { (i, _) ->
                    // the modifier chain around it: neighbouring lines starting with a dot.
                    // only there does a size count towards the area.
                    var start = i
                    while (start > 0 && lines[start - 1].trim().startsWith(".")) start--
                    var end = i
                    while (end + 1 < lines.size && lines[end + 1].trim().startsWith(".")) end++
                    lines.subList(start, end + 1)
                        .mapNotNull { pattern.find(it)?.groupValues?.get(1)?.toFloat() }
                        .filter { it < 48f }
                        .minOrNull()
                        ?.let { "${file.name}:${i + 1} (${it.toInt()} dp)" }
                }
        }
        assertEquals(
            "PLAN.md 3.6: no element under 48 dp. an icon may be smaller - the area that " +
                "takes the finger may not.",
            emptyList<String>(),
            tooSmall,
        )
    }
}
