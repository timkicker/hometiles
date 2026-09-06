package dev.kicker.hometiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a tap must not end in nothing.
 *
 * `runCatching` is convenient: it catches everything and returns a `Result` one may drop. it
 * stands in 86 places, and in most of them that is right - a query that may be empty, a
 * listener about to die anyway.
 *
 * in two places it was wrong, because a person stands there with a finger on the glass: the
 * toggle tiles and the buttons during a call. staying silent there leaves a button that does
 * nothing - in the worst case "answer" while the phone is ringing.
 */
class SilentFailureTest {

    @Test
    fun `no attempt ends silently in ToggleActions`() {
        val lines = Quelltext.file("dev/kicker/hometiles/toggles/ToggleActions.kt").readLines()
        val withoutAWayOut = lines.withIndex()
            .filter { (_, line) -> "runCatching" in line && !Quelltext.isCommentLine(line) }
            .filter { (i, _) ->
                // the way out may stand in the same expression - one line further down or
                // up to the block's closing brace.
                lines.drop(i).take(20).takeWhile { "runCatching" !in it || it == lines[i] }
                    .none { "onFailure" in it }
            }
            .map { it.index + 1 }
        assertEquals(
            "a toggle tap ends here without a way out. ToggleActions is the last stop: " +
                "after it there is no fallback, only a tile that does nothing.",
            emptyList<Int>(),
            withoutAWayOut,
        )
    }

    @Test
    fun `no result from InCallRepository is thrown away`() {
        // which functions return a `Result` stands in the source, not in a list here that
        // ages. `attach`, `detach` and `publish` are bookkeeping and return nothing; they
        // may stand as statements of their own.
        val withResult = Quelltext.file("dev/kicker/hometiles/phone/InCallRepository.kt")
            .readLines()
            .mapNotNull { line ->
                Regex("""fun (\w+)\([^)]*\)[^=]*= runCatching""").find(line)?.groupValues?.get(1)
            }
            .toSet()
        assertTrue("no result function found - does the rule still read what it means?", withResult.size >= 5)

        val thrownAway = Quelltext.files()
            .filter { it.name != "InCallRepository.kt" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) ->
                        val bare = line.trim()
                        withResult.any { bare.startsWith("InCallRepository.$it(") }
                    }
                    .map { (i, _) -> "${file.name}:${i + 1}" }
            }
        assertEquals(
            "a call to InCallRepository as a statement of its own throws its result away. " +
                "each of these can fail because the call is gone by then - and then the " +
                "person in front of it has to learn about it.",
            emptyList<String>(),
            thrownAway,
        )
    }

    @Test
    fun `the call screen reports a failure`() {
        val source = Quelltext.file("dev/kicker/hometiles/phone/InCallActivity.kt").readText()
        assertTrue(
            "InCallActivity no longer checks the outcome",
            "isFailure" in source && "R.string.call_action_failed" in source,
        )
    }
}
