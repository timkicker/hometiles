package org.biglau.settings

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a warning about a backup stands before the loading, not after it.
 *
 * a backup from a newer BigLau carries fields this version does not know; they fall away on
 * reading. the sentence about it stood only under `done && fromNewer` - retold at the
 * emulator on 04.09.2026 with a file carrying `"version": 99`. whoever reads it then can
 * decide nothing any more: the old setup is already replaced.
 */
class WarnBeforeLoadingTest {

    private val importing = Quelltext.withoutComments("org/biglau/settings/ImportActivity.kt")

    @Test
    fun `the origin stands before the confirmation`() {
        val before = Regex("""!done && fromNewer|fromNewer && !done""").containsMatchIn(importing)
        assertTrue(
            "ImportActivity names the newer origin only after loading. then the existing " +
                "setup is already replaced, and the information comes too late.",
            before,
        )
    }

    @Test
    fun `the sentence before says what will happen, not what has happened`() {
        listOf("values-de", "values").forEach { language ->
            val text = Quelltext.textValue("transfer_confirm_newer", language)
            assertTrue(
                // the german word is the check object: "bleibt" is the future tense the
                // german sentence has to carry.
                "the sentence before loading stands in the past (language \"$language\"): " +
                    text + "\nbefore the decision the future belongs there: what *will* fall away.",
                "bleibt" in text || "will be" in text,
            )
        }
    }
}
