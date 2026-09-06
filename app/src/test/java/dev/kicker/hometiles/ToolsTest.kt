package dev.kicker.hometiles

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the tools and their description stay together.
 *
 * `tools/` holds the programs for checking on the device - things no unit test can see. an
 * undocumented tool goes unused, and a documented one that does not exist costs the reader a
 * hunt.
 *
 * hence both directions here - and the executable bit with them, since a tool one has to
 * `chmod` first is one more stumbling block on the way to a measurement.
 *
 * **a limit, measured on 03.09.2026:** `tools/` does stand in the test task's inputs
 * (`app/build.gradle.kts`), but gradle compares **contents**, not permissions. take only the
 * executable bit away and the task does not re-run, so the rule stays green until something
 * else triggers it. with `--rerun-tasks` it falls over at once. for everyday work that is
 * enough - in the same move content almost always changes too.
 */
class ToolsTest {

    private val directory = File("../tools")
    private val description = File("../tools/README.md").readText()

    private fun tools(): List<File> = directory
        .listFiles { d -> d.extension == "sh" || d.extension == "py" }
        ?.sortedBy { it.name }
        .orEmpty()

    @Test
    fun `every tool is described`() {
        val undescribed = tools().map { it.name }.filterNot { "tools/$it" in description }
        assertEquals(
            "tool without an entry in tools/README.md - then nobody uses it any more",
            emptyList<String>(),
            undescribed,
        )
    }

    @Test
    fun `every described tool exists`() {
        val named = Regex("""tools/([a-z-]+\.(?:sh|py))""").findAll(description)
            .map { it.groupValues[1] }.toSortedSet()
        val missing = named.filterNot { File(directory, it).isFile }
        assertEquals("described but not present", emptyList<String>(), missing)
    }

    @Test
    fun `every tool can be started`() {
        tools().forEach {
            assertTrue("${it.name} is not executable", it.canExecute())
        }
    }
}
