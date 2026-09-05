package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * no link to a file that does not exist.
 *
 * `PLAN.md` named two checklists under a `docs/` directory that never existed. whoever looks
 * for such a checklist looks a while and then thinks themselves blind - a dead link is worse
 * than none, because it promises work someone is supposed to have done.
 *
 * only paths **with a directory** ending in `.md`, `.sh` or `.py` are checked: documents and
 * tools. with a bare `strings.xml` it would not be clear which one is meant.
 *
 * **`STATUS.md` is exempt, with a reason.** it is a chronicle: it reports on moved files,
 * deleted tools and on exactly those two dead links. a chronicle allowed to name only paths
 * that still exist would have to rewrite its own history. the rule fell over that on its
 * first run, and that was the rule's fault, not the entry's.
 */
class LinksTest {

    /** path to why it may stand there anyway. */
    private val allowed = mapOf<String, String>()

    // without backticks: in `tools/README.md` the tools stand in command lines, not in
    // backticks. the first version found nothing there and reported "only two paths found" -
    // true, and beside the point.
    private val pattern = Regex("""([A-Za-z0-9_./-]+/[A-Za-z0-9_.-]+\.(?:md|sh|py))""")

    /**
     * a web address is not a path in this repository.
     *
     * the badge link `https://img.shields.io/...` reads to the pattern above as a file
     * `img.sh` in a directory - and the rule reported it as dead. it is not dead, it is not
     * a file at all.
     */
    private fun withoutUrls(text: String): String = text.replace(Regex("""https?://\S+"""), " ")

    @Test
    fun `every named path exists`() {
        val dead = listOf("PLAN.md", "README.md", "tools/README.md")
            .flatMap { name ->
                val text = withoutUrls(File("../$name").readText())
                pattern.findAll(text).map { it.groupValues[1] }.map { name to it }
            }
            .filterNot { (_, path) -> path in allowed }
            .filterNot { (_, path) -> File("../$path").exists() }
            .map { (where, path) -> "$where names $path" }
            .distinct()
            .sorted()

        assertEquals(
            "a link to a file that does not exist. either create the file, or replace the " +
                "link with what really is there.",
            emptyList<String>(),
            dead,
        )
    }

    @Test
    fun `the rule finds any paths at all`() {
        val count = listOf("PLAN.md", "README.md", "tools/README.md")
            .sumOf { pattern.findAll(withoutUrls(File("../$it").readText())).count() }
        assertTrue("only $count paths found - does the rule still search?", count >= 5)
    }

    /**
     * and `QUERY_ALL_PACKAGES` stays a mention, not a permission.
     *
     * `PLAN.md` 6 says explicitly: no `QUERY_ALL_PACKAGES`. a launcher gets by with
     * `<queries>` and `LauncherApps`, and this is the permission apps get thrown out of the
     * play store for. in the manifest it stands only in a comment explaining why it is not
     * needed - a `grep` alone would not tell the two apart.
     */
    @Test
    fun `QUERY_ALL_PACKAGES stands only in the comment`() {
        val manifest = File("src/main/AndroidManifest.xml").readLines()
        val real = manifest.withIndex()
            .filter { (_, line) -> "QUERY_ALL_PACKAGES" in line }
            .filter { (_, line) -> "uses-permission" in line }
            .map { (i, _) -> "AndroidManifest.xml:${i + 1}" }
        assertEquals(
            "PLAN.md 6: no QUERY_ALL_PACKAGES. a launcher gets by with <queries> and " +
                "LauncherApps.",
            emptyList<String>(),
            real,
        )
        assertTrue(
            "the comment explaining why the permission is not needed is gone - then the next " +
                "reader wonders whether it was on purpose.",
            manifest.any { "QUERY_ALL_PACKAGES" in it },
        )
    }
}
