package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a tile starts nothing without having asked the lock.
 *
 * the app tile asked, the shortcut tile did not. whoever put a locked app on a tile as a
 * **shortcut** got in without the pin - the same app, the same lock, another way. found on
 * 03.09.2026, three lines below the app tile that did it right.
 *
 * it is the same fault as with the magnifier key in the app list (see [SearchKeyLockTest]),
 * found the same day: the check hangs on the single way instead of on the place where all
 * ways meet. that place is `startAction` now, and this rule holds that nobody gets past it.
 */
class LockOnEveryTileTest {

    private val lines = Quelltext.file("org/biglau/MainActivity.kt").readLines()

    @Test
    fun `only one place starts apps and shortcuts`() {
        val starts = lines.withIndex()
            .filter { (_, line) ->
                val bare = line.trim()
                !Quelltext.isCommentLine(line) &&
                    ("apps.launch(" in bare || Regex("""ShortcutRepository[^)]*\)\.launch\(""").containsMatchIn(bare))
            }
            .map { it.index + 1 }
        assertTrue("nothing starts anything any more - does the rule still read what it means?", starts.isNotEmpty())

        val startAt = lines.indexOfFirst { it.trim().startsWith("private fun startAction(") }
        assertTrue("`startAction` does not exist any more", startAt > 0)
        val end = lines.drop(startAt).indexOfFirst { it == "    }" } + startAt + 1

        val outside = starts.filterNot { it in (startAt + 1)..end }
        assertEquals(
            "something is started here without going through `startAction`. as long as there " +
                "are two ways, the lock is forgotten on one of them - that is exactly how the " +
                "shortcut tile got through without a pin.",
            emptyList<Int>(),
            outside,
        )
    }

    @Test
    fun `before every start stands the question about the lock`() {
        val withoutAsking = lines.withIndex()
            .filter { (_, line) -> line.trim().startsWith("startAction(") }
            .filter { (i, _) ->
                lines.subList(maxOf(0, i - 6), i)
                    .none { "isLocked(" in it || "onAccept" in it }
            }
            .map { it.index + 1 }
        assertEquals(
            "`startAction` is called here without the lock having been asked first or the " +
                "pin already entered.",
            emptyList<Int>(),
            withoutAsking,
        )
    }

    @Test
    fun `the lock knows both kinds of tile`() {
        val lockedAt = lines.indexOfFirst { it.trim().startsWith("private fun isLocked(") }
        assertTrue("`isLocked` does not exist any more", lockedAt > 0)
        val body = lines.subList(lockedAt, minOf(lines.size, lockedAt + 20)).joinToString("\n")
        assertTrue(
            "the lock does not ask about shortcuts - then it can be walked around with a " +
                "shortcut tile.",
            "ButtonAction.Shortcut" in body && "ButtonAction.App" in body,
        )
    }
}
