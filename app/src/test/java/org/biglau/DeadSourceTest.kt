package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * what nobody calls gets deleted.
 *
 * three such places turned up at once: a controller method, a `dpToPx` whose kdoc claimed it
 * was needed in one place (it was needed in none), and a named constant beside the same
 * number without a name in the model. the third was the worst: it looked like the one truth
 * and was only a comment with a type.
 *
 * dead source is not merely ballast. it gets read, found in every search and dragged along
 * through every rebuild - and whoever changes it checks nothing, because nothing uses it.
 *
 * **what this rule does not see**, deliberately: `private` and `internal` (the compiler
 * reports those itself), `override`, `@Composable` (previews do not call them visibly) and
 * names under four characters (too many chance hits).
 */
class DeadSourceTest {

    /** name to why it may exist anyway. */
    private val mayStay = mapOf<String, String>()

    private fun declarations(): Map<String, List<File>> {
        val hits = mutableMapOf<String, MutableList<File>>()
        Quelltext.files().forEach { file ->
            file.readLines().forEach { line ->
                if (line.trimStart().startsWith("private ") ||
                    line.trimStart().startsWith("internal ") ||
                    line.trimStart().startsWith("override ")
                ) {
                    return@forEach
                }
                Regex("""^ {0,4}(?:fun|val|const val) (\w{4,})""").find(line)?.let {
                    hits.getOrPut(it.groupValues[1]) { mutableListOf() }.add(file)
                }
            }
        }
        return hits
    }

    /**
     * how often each identifier occurs in the whole source - counted **once**.
     *
     * the first version searched all files with a `Regex` of its own per declaration: some
     * five hundred names times a hundred and forty files, measured at **55 seconds**, more
     * than half of `:app`'s entire test run. a rule costing a minute per build gets switched
     * off eventually - and then it checks nothing at all.
     *
     * now the source is split into identifiers **once** and counted; the check afterwards is
     * a lookup.
     */
    private fun frequencies(): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        val word = Regex("""[A-Za-z_][A-Za-z0-9_]*""")
        (Quelltext.files() + Quelltext.testFiles()).forEach { file ->
            word.findAll(file.readText()).forEach { hit ->
                counts[hit.value] = (counts[hit.value] ?: 0) + 1
            }
        }
        return counts
    }

    @Test
    fun `every public place has a caller`() {
        val howOften = frequencies()
        val without = declarations()
            .filterKeys { it !in mayStay }
            .filter { (name, places) -> (howOften[name] ?: 0) <= places.size }
            .map { (name, places) -> "$name (${places.joinToString { o -> o.name }})" }
            .sorted()

        assertEquals(
            "nobody calls this place. delete it - or, if it has to exist, into the list in " +
                "DeadSourceTest with a reason. a named number without a caller beside the " +
                "same number without a name is the most dangerous case: it looks like the " +
                "one truth.",
            emptyList<String>(),
            without,
        )
    }

    @Test
    fun `the rule finds anything at all`() {
        val count = declarations().size
        assertTrue("only $count public places found - does the rule still search?", count >= 100)
    }
}
