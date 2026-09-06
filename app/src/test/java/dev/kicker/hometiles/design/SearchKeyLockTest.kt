package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the search key is not a side door.
 *
 * the app list has long carried the sentence that the lock holds here too and not only on the
 * tiles, or it could be walked around in one tap. the rows kept to that and went through
 * `open`, which asks for the pin. the search key beside them called `launch` and started
 * directly - so a locked app could be opened through the search field in one key press. the
 * comment stood there, and so did the exception.
 *
 * and the smaller half of the same place: the counter above the search field counted only the
 * apps although the settings row stands in the list too. two rows stood there under "1 hit",
 * and the search key opened one of them without showing which.
 */
class SearchKeyLockTest {

    private val file = Quelltext.file("dev/kicker/hometiles/apps/AppDrawerActivity.kt")
    private val lines = file.readLines()
    private val source = file.readText()

    @Test
    fun `no app starts without the lock being asked`() {
        val unchecked = lines.withIndex()
            .filter { (_, line) ->
                val bare = line.trim()
                Regex("""(^|[^.\w])launch\(""").containsMatchIn(bare) &&
                    !Quelltext.isCommentLine(line) &&
                    !bare.startsWith("fun launch(") &&
                    "repository.launch(" !in bare
            }
            .filter { (i, _) ->
                // the look at the lock may stand above the start - either as the question
                // (`open`) or as the yes after it (the pin entered).
                lines.subList(maxOf(0, i - 12), i)
                    .none { "AppLock.needsPin" in it || "onAccept" in it }
            }
            .map { it.index + 1 }
        assertEquals(
            "an app starts here without the app lock being asked. it can be walked around " +
                "this way - which is exactly what the comment above `open` gives as the " +
                "reason for its existence.",
            emptyList<Int>(),
            unchecked,
        )
    }

    @Test
    fun `the counter means the same set as the list`() {
        val counter = Regex("""R\.plurals\.search_matches,\s*(\w+)""")
            .find(source)?.groupValues?.get(1)
        assertNotNull("the hit counter is gone - does the rule move with it?", counter)

        // what does the settings row hang on? that stands above it, not in this rule.
        val atRow = lines.indexOfFirst { "label = settingsLabel" in it }
        assertTrue("the settings row no longer exists", atRow > 0)
        val condition = lines.subList(maxOf(0, atRow - 8), atRow)
            .reversed()
            .firstNotNullOfOrNull { Regex("""if \((\w+)\)""").find(it)?.groupValues?.get(1) }
        assertNotNull("the settings row hangs on no condition any more", condition)

        val explanation = lines.firstOrNull { it.trim().startsWith("val $counter ") }
        assertNotNull("$counter is explained nowhere", explanation)
        assertTrue(
            "the counter ($counter) knows nothing of the settings row ($condition). then the " +
                "number 1 stands above a list of two rows.",
            condition!! in explanation!!,
        )
    }

    @Test
    fun `the search key works on the same set as the counter`() {
        val counter = Regex("""R\.plurals\.search_matches,\s*(\w+)""")
            .find(source)!!.groupValues[1]
        val atSearch = lines.indexOfFirst { "onSearch = " in it }
        assertTrue("onSearch no longer exists", atSearch > 0)
        val called = Regex("""(\w+)[?.]""").find(lines[atSearch].substringAfter("onSearch = "))
            ?.groupValues?.get(1)
        assertNotNull("onSearch calls nothing named", called)

        val atExplanation = lines.indexOfFirst { it.trim().startsWith("val $called") }
        assertTrue("$called is explained nowhere", atExplanation > 0)
        assertTrue(
            "the search key ($called) does not work with the counter ($counter). then the " +
                "sentence beside it promises something unambiguous about an ambiguous state: " +
                "two rows, and the key opens one of them.",
            lines.subList(atExplanation, minOf(lines.size, atExplanation + 6))
                .any { counter in it },
        )
    }
}
