package dev.kicker.hometiles.design

import java.io.File
import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * logic only the test calls is not the app's logic.
 *
 * a run over all `object` functions found **22** that no place in the app called - every one
 * of them with tests, all green. among them `FolderEdits.orphaned`, which a comment expressly
 * pointed at, and `CellLayout.fitToGrid`, whose calculation stood a second time by hand in
 * `setGrid`: the tested version never ran, the running one was untested.
 *
 * this is the most expensive kind of fault in this project: the tests say it works, and they
 * are right - only nobody uses it. functions in `object`s are checked because that is the
 * pure logic; classes and composables have call paths no text comparison finds reliably.
 */
class DeadLogicTest {

    /**
     * what may exist without the app calling it. only with a reason and a path back to it -
     * an exception without both is only a fault turned down quieter.
     */
    private val reasonedExceptions = emptyMap<String, String>()

    private fun files(): List<File> =
        Quelltext.files()

    /** function name to "object.name", for every function directly in an `object`. */
    private fun declarations(): List<Pair<String, String>> {
        val found = mutableListOf<Pair<String, String>>()
        files().forEach { file ->
            var obj: String? = null
            file.readLines().forEach { line ->
                Regex("""^\s*(?:internal\s+)?object\s+(\w+)""").find(line)?.let {
                    obj = it.groupValues[1]
                }
                Regex("""^\s{4}(?:internal\s+)?fun\s+(?:<[^>]*>\s*)?(\w+)\s*\(""").find(line)?.let {
                    val name = it.groupValues[1]
                    obj?.let { o -> found += name to "$o.$name" }
                }
            }
        }
        return found
    }

    private val source: String by lazy { files().joinToString("\n") { it.readText() } }

    /**
     * three counts over the whole source - **once**, not per name.
     *
     * the first version built three `Regex`es per name and ran them over the entire source,
     * measured at **26 seconds**, a quarter of `:app`'s test run, for a rule that finds
     * nothing. now it counts three times through and afterwards only looks up.
     */
    private val calls: Map<String, Int> by lazy {
        Regex("""([A-Za-z_][A-Za-z0-9_]*)\s*\(""").findAll(source)
            .groupingBy { it.groupValues[1] }.eachCount()
    }

    private val functions: Map<String, Int> by lazy {
        Regex("""fun\s+(?:<[^>]*>\s*)?([A-Za-z_][A-Za-z0-9_]*)\s*\(""").findAll(source)
            .groupingBy { it.groupValues[1] }.eachCount()
    }

    private val references: Map<String, Int> by lazy {
        Regex("""::([A-Za-z_][A-Za-z0-9_]*)""").findAll(source)
            .groupingBy { it.groupValues[1] }.eachCount()
    }

    /** calls minus declarations, plus method references (`::name`). */
    private fun isCalled(name: String): Boolean =
        (calls[name] ?: 0) - (functions[name] ?: 0) + (references[name] ?: 0) > 0

    @Test
    fun `every function in an object is called too`() {
        val dead = declarations()
            .filterNot { (name, _) -> isCalled(name) }
            .map { it.second }
            .filterNot { it in reasonedExceptions }
            .distinct()
            .sorted()
        assertEquals(
            "only the test calls this logic. either the place where it should run is " +
                "missing, or it already exists elsewhere and this is the second copy: $dead",
            emptyList<String>(),
            dead,
        )
    }

    @Test
    fun `every exception names its reason and really exists`() {
        val known = declarations().map { it.second }.toSet()
        reasonedExceptions.forEach { (entry, reason) ->
            assertTrue("$entry no longer exists - strike the exception", entry in known)
            assertTrue("$entry needs a reason", reason.length > 20)
        }
    }

    @Test
    fun `the rule does not find an invented name`() {
        // counter-check: without it a broken pattern would wave everything through.
        assertTrue(!isCalled("dieseFunktionGibtEsNicht"))
    }

    @Test
    fun `the rule recognises a real call`() {
        assertTrue("group is called by CallLogRepository", isCalled("group"))
    }
}
