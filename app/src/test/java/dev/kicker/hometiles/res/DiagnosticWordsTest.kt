package dev.kicker.hometiles.res

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * one word for four lines fits two of them.
 *
 * the diagnostics page puts a value behind every line, and one german string stood everywhere
 * for "nothing there": "keine". read on the emulator on 04.09.2026 it was wrong twice - the
 * german words stay quoted because they are the check object:
 *
 * * "Letzter Absturz: keine" - the crash is masculine in german, it takes "keiner".
 * * "Akku-Sparen: keine" when the query fails - there nothing is missing, the *answer* is;
 *   "unbekannt" says that.
 *
 * plus an inequality in the same section: the off case said "Für HomeTiles aus", the on case
 * only "An" - which reads as the phone's own saving mode. both name HomeTiles now.
 */
class DiagnosticWordsTest {

    private val source = Quelltext.withoutComments("dev/kicker/hometiles/settings/Diagnostics.kt")

    @Test
    fun `the last crash has a word of its own`() {
        assertTrue(
            "the crash line uses the general \"keine\" - that is wrong german",
            "diag_crash_none" in source,
        )
    }

    @Test
    fun `a missing answer is not called nothing`() {
        assertTrue(
            "when the battery query fails it says \"keine\" instead of \"unbekannt\"",
            "diag_unknown" in source,
        )
    }

    @Test
    fun `both sides of the battery line are equally wide`() {
        listOf("values-de", "values").forEach { language ->
            val on = Quelltext.textValue("diag_battery_saving_on", language)
            val off = Quelltext.textValue("diag_battery_saving_off", language)
            val namesApp = { text: String -> "HomeTiles" in text }
            assertTrue(
                "only one of the two answers names HomeTiles (language \"$language\"): " +
                    "on=\"$on\", off=\"$off\" - then the other reads as a statement about " +
                    "the whole phone",
                namesApp(on) == namesApp(off),
            )
        }
    }
}
