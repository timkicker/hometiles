package dev.kicker.hometiles.design

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * no em dashes in our own prose, not only in the app's texts.
 *
 * [dev.kicker.hometiles.res.TextDashesTest] keeps the screen texts free. the instruction of 04.09.2026 was
 * for everything written, and `PLAN.md` still carried 141 long dashes, 15 short ones and 29
 * middle dots that day. whoever sets a rule for the app and does not keep it themselves has
 * no rule but an opinion about others.
 *
 * the chronicle was the large remainder: 1470 dashes in 361 old entries, rewritten by
 * `tools/dashes.py` by pattern rather than character for character - brackets for a
 * double aside, a comma before a conjunction, a colon before a list or a code block, else a
 * full stop. checked on a sample, not on each of the 1470; some read as an ellipsis rather
 * than a whole sentence. that is a work diary, not a screen text.
 *
 * a middle dot used as a **separator** between two pieces of information on screen is not
 * prose and is not meant here; two such places stand in the source.
 */
class ProseDashesTest {

    private val forbidden = mapOf(
        '—' to "long dash",
        '–' to "short dash",
        '·' to "middle dot",
    )

    private fun file(name: String) = File("../$name")

    /**
     * the store texts belong here too, and did not until 06.09.2026.
     *
     * the rule read the readme and the tools page and stopped there. the german store
     * description carried two long dashes all along, and it took F-Droid's own scan on the
     * merge request to show them - on the page every visitor sees first. a rule that covers
     * what one happens to think of is not a rule.
     */
    private fun prose(): List<File> =
        listOf(file("README.md"), file("tools/README.md")) +
            File("../fastlane/metadata/android")
                .listFiles().orEmpty()
                .flatMap { language ->
                    listOf("full_description.txt", "short_description.txt", "title.txt")
                        .map { File(language, it) } +
                        File(language, "changelogs").listFiles().orEmpty()
                }
                .filter { it.isFile }

    @Test
    fun `the readme and the store texts carry no dash`() {
        val hits = prose().flatMap { file ->
            val content = file.readText()
            forbidden.entries.filter { (c, _) -> c in content }.map { (c, what) ->
                "${file.path}: $what, ${content.count { it == c }} times"
            }
        }
        assertEquals(
            "the replacement is a comma, a colon, a full stop or a second line. a range of " +
                "numbers takes the hyphen",
            emptyList<String>(),
            hits,
        )
    }

    @Test
    fun `the rule would find a dash`() {
        // counter-check: an empty set of characters would wave everything through.
        assertTrue(forbidden.keys.any { it in "a sentence — with a dash" })
    }
}
