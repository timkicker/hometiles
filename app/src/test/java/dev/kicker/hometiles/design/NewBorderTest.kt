package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the border for something new is thin and quiet.
 *
 * it used to pulse. once the focus border for key operation arrived, the user decided the
 * other way round: a pulsing rectangle suits "the focus is here" better on a key phone than
 * "something new is here". for the new one a very thin, quiet border is enough.
 *
 * that is a redistribution, not carelessness. on a screen where one thing moves, one looks
 * at that thing; on a key phone that should be the place the select key hits. nothing is
 * lost - the number in the corner still says how much is waiting, and more precisely than
 * any movement.
 */
class NewBorderTest {

    private val tile = Quelltext.withoutComments("dev/kicker/hometiles/ui/BigTile.kt")

    @Test
    fun `nothing on the tile pulses any more`() {
        listOf("rememberInfiniteTransition", "infiniteRepeatable", "RepeatMode.Reverse").forEach {
            assertTrue(
                "$it stands in BigTile again. the border for something new should be quiet; " +
                    "whoever builds a movement back in takes it from the focus.",
                it !in tile,
            )
        }
    }

    @Test
    fun `the border for something new is thin`() {
        val number = Regex("""BADGE_BORDER_DP = ([0-9.]+)f""").find(tile)
            ?: throw AssertionError("BADGE_BORDER_DP no longer stands in the source")
        val dp = number.groupValues[1].toFloat()
        assertTrue("at $dp dp the border for something new is no thin border", dp <= 3f)
    }

    @Test
    fun `the number in the corner stays`() {
        // it is the exact information now that the movement is gone.
        assertTrue(
            "the tile no longer shows the count - then nothing says how much is waiting",
            "badgeCount" in tile,
        )
    }
}
