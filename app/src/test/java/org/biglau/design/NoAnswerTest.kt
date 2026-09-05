package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * no answer is not an empty answer.
 *
 * `ShortcutRepository` wrote it into its own head: showing an empty list is wrong, because
 * nobody can see whether the app has no shortcuts or we were not allowed to ask. one line
 * further down a failed `getShortcuts` became `emptyList()` all the same - and the screen
 * said the app offers no shortcuts without anyone having asked it.
 *
 * the rule holds both halves: the difference must not vanish in the type, and it has to
 * arrive in the surface as two different sentences.
 */
class NoAnswerTest {

    private val repository = Quelltext.file("org/biglau/shortcuts/ShortcutRepository.kt")
    private val editor = Quelltext.file("org/biglau/tiles/TileEditorActivity.kt")

    @Test
    fun `forPackage tells failure and empty list apart in the type`() {
        val line = repository.readLines().firstOrNull { "fun forPackage(" in it }
        assertTrue("forPackage no longer exists - does the rule move with it?", line != null)
        assertTrue(
            "forPackage returns a bare list again: $line - then a failure can no longer be " +
                "told from an empty answer.",
            "ShortcutAnswer" in line!!,
        )
    }

    @Test
    fun `a failed call does not become the empty list`() {
        val merged = repository.readLines().withIndex()
            .filter { (_, line) -> "getShortcuts" in line && !Quelltext.isCommentLine(line) }
            .filter { (_, line) -> ".orEmpty()" in line || "getOrDefault(emptyList" in line }
            .map { it.index + 1 }
        assertEquals(
            "a failed getShortcuts becomes an empty list here. afterwards it can no longer " +
                "be seen whether the app has no shortcuts or whether we got no answer.",
            emptyList<Int>(),
            merged,
        )
    }

    @Test
    fun `the surface says something different for the two states`() {
        val source = editor.readText()
        val start = source.indexOf("private fun ShortcutList(")
        assertTrue("ShortcutList no longer exists", start > 0)
        val body = Quelltext.cut(source.substring(start), "", "\n@Composable")

        assertTrue(
            "ShortcutList no longer knows the failure - then the sentence about the app " +
                "holds even when nobody asked it.",
            "ShortcutAnswer.Failed" in body,
        )
        assertTrue(
            "both states show the same sentence.",
            "R.string.shortcut_none" in body && "R.string.shortcut_unreadable" in body,
        )
        // a sentence naming a problem needs a way out - and it stands in the same branch, so
        // it comes along on the failure too.
        assertTrue(
            "the failure ends in a dead end: no button to another app.",
            body.indexOf("R.string.shortcut_other_app") > body.indexOf("R.string.shortcut_unreadable"),
        )
    }

    @Test
    fun `both sentences exist in both languages`() {
        // one file per language carrying the sentence is enough - which module that is, is
        // none of the rule's business, it may move.
        val missing = listOf("shortcut_none", "shortcut_unreadable").flatMap { name ->
            listOf("values", "values-de").filterNot { language ->
                Quelltext.texts(language).any { "name=\"$name\"" in it.readText() }
            }.map { "$name in $it" }
        }
        assertEquals(
            "a sentence is missing in one language. german is the device's language here, " +
                "not the fallback.",
            emptyList<String>(),
            missing,
        )
    }
}
