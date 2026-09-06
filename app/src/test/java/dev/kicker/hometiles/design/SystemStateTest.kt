package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * what the system grants is read again on coming back.
 *
 * a permission asked through `rememberLauncherForActivityResult` reports back and the screen
 * redraws. notification access is granted in a system setting, and from there nothing comes
 * back: the settings screen kept offering "grant notification access" to whoever had just
 * granted it. a line that lies is worse than one that is missing.
 *
 * `HomeTilesActivity.resumes` counts the returns; whoever reads such a state wraps it in
 * `remember(resumes.intValue) { … }`.
 */
class SystemStateTest {

    private val base = Quelltext.file("dev/kicker/hometiles/ui/HomeTilesActivity.kt").readText()

    @Test
    fun `the shared base counts the returns`() {
        assertTrue("HomeTilesActivity no longer knows `resumes`", "resumes" in base)
        val resume = Quelltext.cut(base, "override fun onResume()", "\n    }")
        assertTrue(
            "onResume no longer counts up - then nobody notices the screen is in front again.",
            "resumes.intValue += 1" in resume,
        )
    }

    @Test
    fun `notification access is read again on coming back`() {
        val place = Quelltext.file("dev/kicker/hometiles/settings/SettingsActivity.kt").readText()
            .let { Quelltext.cut(it, "accessGranted =") }
            .take(200)
        assertTrue(
            "accessGranted is read once again. whoever grants access and comes back still " +
                "reads \"grant\".",
            "remember(resumes.intValue)" in place,
        )
    }

    /**
     * the wizard skips what is done, but read its state **once** and refreshed it only from
     * its own two dialogs. the same way past the check is the fallback for the home role:
     * that one goes through `startActivity` and returns no result at all.
     */
    @Test
    fun `the wizard reads its state again on coming back`() {
        val place = Quelltext.file("dev/kicker/hometiles/wizard/WizardActivity.kt").readText()
            .let { Quelltext.cut(it, "var state by") }
            .take(120)
        assertTrue(
            "the wizard reads its state only once again - then a finished step stays " +
                "standing: $place",
            "remember(resumes.intValue)" in place,
        )
    }

    /** a counter, not a `Boolean`: the same value set twice triggers no redraw. */
    @Test
    fun `it is a counter and not a switch`() {
        assertEquals(
            "resumes has to be an Int state - a Boolean set to true twice does not redraw.",
            true,
            "mutableIntStateOf(0)" in base,
        )
    }

    /**
     * and the rule holds for the **whole class**, not for the one line.
     *
     * four states the system grants and reports nothing back about: notification access, the
     * sms role, the dialer role, the home role. a screen reading one of them has to read it
     * again on coming back.
     *
     * places **without a surface** are exempt: a receiver checking when it fires reads fresh
     * anyway.
     */
    @Test
    fun `no system state is read once in a surface`() {
        // places that read **on a tap** and are fresh anyway.
        //
        // naming one of them by its wording broke as soon as the same thing for the sms role
        // wrapped a line differently. the question is whether the read sits in a handler that
        // runs on a tap, however it is written.
        val handler = Regex("""on[A-Z]\w* = \{""")
        val withoutSurface = setOf(
            "MessageReminderReceiver.kt", "SmsRepository.kt",
            // diagnostics collects the lines; the call **to it** is the guarded one.
            "Diagnostics.kt", "WizardActivity.kt", "MainActivity.kt",
        )
        val pattern = Regex(
            """(isDefaultSmsApp\(\)|NotificationRepository\.isEnabled\(|""" +
                """Diagnostics\.isDefaultHome\(|DialerRole\.held\()""",
        )
        val places = Quelltext.files()
            .filterNot { it.name in withoutSurface }
            .flatMap { file ->
                val lines = file.readLines()
                lines.withIndex()
                    .filter { (_, line) ->
                        pattern.containsMatchIn(line) && !Quelltext.isCommentLine(line) &&
                            "fun " !in line
                    }
                    .filterNot { (i, _) ->
                        // the call stands in `remember(resumes…)` - here or a line above,
                        // depending on how it wraps.
                        (i downTo maxOf(0, i - 2)).any {
                            "remember(resumes.intValue)" in lines[it]
                        }
                    }
                    .filterNot { (i, _) ->
                        // upwards to the next handler start - at most twelve lines, or every
                        // place would eventually be "in a handler".
                        (i downTo maxOf(0, i - 12)).any { handler.containsMatchIn(lines[it]) }
                    }
                    .map { (i, _) -> "${file.name}:${i + 1}" }
            }
        assertEquals(
            "a state the system grants is read here, once, while drawing. wrap it in " +
                "`remember(resumes.intValue) { … }`, or pass it down from the activity.",
            emptyList<String>(),
            places,
        )
    }

    /**
     * the same for permissions granted in the **app settings**.
     *
     * a permission asked through `rememberLauncherForActivityResult` reports back. once it is
     * permanently denied that way is gone, HomeTiles sends the user to `Intents.appSettings`,
     * and from there nothing comes back: four screens still read "no permission" afterwards
     * — on the very screen that had sent the user there.
     */
    @Test
    fun `whoever sends to the app settings reads the permission again`() {
        val places = Quelltext.files()
            .filter { "Intents.appSettings(" in it.readText() }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) ->
                        Regex("""remember \{ mutableStateOf\(\w+\.has\w*Permission\(\)\)""")
                            .containsMatchIn(line)
                    }
                    .map { (i, _) -> "${file.name}:${i + 1}" }
            }
        assertEquals(
            "this screen sends to the app settings but does not read the permission again " +
                "afterwards. use `remember(resumes.intValue) { … }`.",
            emptyList<String>(),
            places,
        )
    }
}
