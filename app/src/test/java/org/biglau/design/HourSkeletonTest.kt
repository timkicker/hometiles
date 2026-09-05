package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * a pattern that **prescribes** the hour instead of leaving it to the language.
 *
 * `DateFormat.getBestDateTimePattern` takes components: `H` means hour 0-23, `j` means the
 * hour as this phone writes it. the call log and the message list asked for `H` until
 * 3.9.2026 and got 24-hour time even where everything else said 5:39 PM - in the list stood
 * 17:39, in the header the same minute as 5:39 PM.
 */
class HourSkeletonTest {

    private val sources = Quelltext.files()

    @Test
    fun `no pattern forces twenty-four hours`() {
        val hits = sources.flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) ->
                    Regex("""bestDatePattern\("[^"]*H""").containsMatchIn(line)
                }
                .map { (i, _) -> "${file.name}:${i + 1}" }
        }
        assertEquals(
            "a date pattern asks for H instead of j - that forces 24-hour time even when the " +
                "phone writes AM/PM.",
            emptyList<String>(),
            hits,
        )
    }
}
