package org.biglau.settings

import java.io.File
import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * no switch in the model that nobody reads.
 *
 * a dead switch is worse than a missing one: it stands in the saved configuration, looks
 * like a promise and does nothing.
 */
class DeadSettingsTest {

    /** fields still to be implemented. implementing one means striking it from here. */
    private val notYetImplemented = emptySet<String>()

    /**
     * counts **reads**, not mere name matches: searching for the bare name counted
     * `longPress` as used because `HapticFeedback.longPress(...)` exists. so: a dot before
     * it, no bracket after it.
     */
    private fun usedOutsideTheModel(field: String): Int {
        val read = Regex("""\.$field\b(?!\s*\()""")
        val assignment = Regex("""\b$field\s*=""")
        return Quelltext.files()
            .filter { it.name != "Model.kt" }
            .sumOf { file ->
                file.readLines().count { read.containsMatchIn(it) || assignment.containsMatchIn(it) }
            }
    }

    /**
     * a field may be read inside the model only, but then whatever reads it has to arrive
     * outside. without that second question a dead field could hide behind a dead getter.
     */
    private fun alive(field: String): Boolean {
        if (usedOutsideTheModel(field) > 0) return true
        return derivations(field).any { usedOutsideTheModel(it) > 0 }
    }

    /** names in Model.kt whose body reads [field]. */
    private fun derivations(field: String): List<String> {
        val text = Quelltext.file("org/biglau/data/Model.kt").readText()
        val head = Regex("""(?:val|fun) (\w+)[:(]""")
        val hits = mutableListOf<String>()
        var name: String? = null
        for (line in text.lines()) {
            head.find(line.trimStart())?.let { name = it.groupValues[1] }
            if (name != null && name != field && Regex("""\b$field\b""").containsMatchIn(line)) {
                hits += name!!
            }
        }
        return hits.distinct()
    }

    /**
     * counts **writes**, the counterpart to the question above and a fault of its own:
     * `Button.longPress` was written and never read, `swipeOrder` read and never written.
     */
    private fun isWritten(field: String): Boolean {
        val assignment = Regex("""\b$field\s*=\s*[^=]""")
        return Quelltext.files()
            .filter { it.name != "Model.kt" && it.name != "Defaults.kt" }
            .any { file -> file.readLines().any { assignment.containsMatchIn(it) } }
    }

    /**
     * payload, not settings: a setting gets changed and so needs a way to change it, while
     * `App("com.x", "Main")` is built whole when a tile is filled and its fields are never
     * switched one by one.
     */
    private val payload = setOf(
        "Cell", "Screen", "LaunchableApp", "SpeedDialTarget",
        // ButtonAction and its variants - what sits on a tile.
        "App", "Contact", "GoToScreen", "Folder", "Link", "Shortcut", "Widget", "Action", "Solid",
    )

    /** read from Model.kt, not kept by hand: a new class must not slip through. */
    private fun configClasses(): List<String> =
        Regex("""data class (\w+)\(""")
            .findAll(Quelltext.file("org/biglau/data/Model.kt").readText())
            .map { it.groupValues[1] }
            .filterNot { it in payload }
            .toList()

    @Test
    fun `no field stays unread`() {
        val dead = mutableListOf<String>()
        for (cls in configClasses()) {
            val fields = Regex("""val (\w+): [\w?<>., ]+""")
                .findAll(modelSection("data class $cls("))
                .map { it.groupValues[1] }
            for (field in fields) {
                if (field !in notYetImplemented && !alive(field)) dead += "$cls.$field"
            }
        }
        assertEquals(emptyList<String>(), dead)
    }

    /**
     * the other question: can it be changed at all? `searchNumbers` and `favouritesFirst`
     * were read and never written by any screen, so they stayed on their default forever.
     *
     * counted is an assignment **inside a `copy(`**: a named parameter in a composable call
     * looks exactly like an assignment.
     */
    private fun settable(field: String): Boolean {
        val inCopy = Regex("""copy\((?:[^()]|\([^()]*\))*\b$field\s*=""", RegexOption.DOT_MATCHES_ALL)
        val direct = Quelltext.files()
            .filter { it.name != "Model.kt" }
            .any { inCopy.containsMatchIn(it.readText()) }
        if (direct) return true
        // or through a setter in the model - `withIcons`, `withClock` and kin - used outside.
        return derivations(field).any { usedOutsideTheModel(it) > 0 }
    }

    /**
     * fields the app keeps itself: `recent` tracks recently started apps, `version` is the
     * format number, `pin` is hashed rather than assigned, `speedDial` goes through
     * SpeedDial.assign. named here, or the rule would have a silent exception.
     */
    private val keptByTheApp = setOf("recent", "version", "pin", "speedDial", "screens", "swipeOrder")

    @Test
    fun `every setting can also be set`() {
        val fixed = mutableListOf<String>()
        for (cls in configClasses()) {
            val fields = Regex("""val (\w+): [\w?<>., ]+""")
                .findAll(modelSection("data class $cls("))
                .map { it.groupValues[1] }
            for (field in fields) {
                if (field in keptByTheApp || field in notYetImplemented) continue
                if (!settable(field)) fixed += "$cls.$field"
            }
        }
        assertEquals(emptyList<String>(), fixed)
    }

    @Test
    fun `the exception list stays short`() {
        // a note, not a shelf: growing means the plan is further from the built thing.
        assertEquals(true, notYetImplemented.size <= 3)
    }

    /**
     * the head of a data class, up to its **own** closing bracket: taking the first closing
     * bracket at all, `listOf(Defaults.mainScreen())` in the default of `screens` ended the
     * section after two fields and the test reported everything in order.
     */
    private fun modelSection(head: String): String {
        val text = Quelltext.file("org/biglau/data/Model.kt").readText()
        val start = text.indexOf(head)
        require(start >= 0) { "no section $head in Model.kt" }
        var depth = 0
        for (i in start + head.length - 1 until text.length) {
            when (text[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i)
                }
            }
        }
        error("closing bracket for $head not found")
    }
}
