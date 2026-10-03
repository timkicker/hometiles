package dev.kicker.hometiles

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * the app may not call an API that is newer than the oldest android it promises to run on.
 *
 * `minSdk = 26` is a promise, repeated in the README and in the F-Droid metadata: this runs
 * on android 8. lint checks that promise and reports every call that breaks it. all five
 * reports it had made were sitting in `lint-baseline.xml`, which does not fix a call, it
 * only stops lint from mentioning it again.
 *
 * what was silenced on 03.10.2026: `Call.Details#getCallDirection` (API 29) in two places,
 * `setShowWhenLocked` and `setTurnScreenOn` (API 27), and `Ringtone#setLooping` (API 28).
 * none of them was guarded by a version check, so each one throws `NoSuchMethodError` the
 * moment it is reached on android 8. the SOS alarm, the call screen and the full-screen
 * message notice are exactly the parts where that matters most.
 *
 * nobody had ever started the app on android 8. the only device and the only emulator image
 * in the project were android 11, where all five calls exist and nothing ever went wrong.
 *
 * `UnusedAttribute` stays allowed: a manifest attribute an older android does not know is
 * ignored, it does not throw.
 */
class MinSdkTest {

    private val baseline = File("lint-baseline.xml").readText()

    private fun silenced(id: String): List<String> =
        Regex("""<issue\s+id="$id"\s+message="([^"]*)"""").findAll(baseline)
            .map { it.groupValues[1] }
            .toList()

    @Test
    fun `no call newer than minSdk is silenced in the baseline`() {
        val calls = silenced("NewApi")
        assertEquals(
            "lint found calls that do not exist on the oldest android the app promises. a " +
                "baseline entry hides the report, it does not add the version check. guard " +
                "the call with Build.VERSION.SDK_INT and give the older android a path that " +
                "works, or raise minSdk and say so everywhere it is promised.\n" +
                calls.joinToString("\n"),
            emptyList<String>(),
            calls,
        )
    }

    @Test
    fun `no inlined constant newer than minSdk is silenced in the baseline`() {
        val fields = silenced("InlinedApi")
        assertEquals(
            "a constant from a newer android is compiled into the app as a plain number. it " +
                "does not throw, it compares against a value the older android never " +
                "produces, so the branch is silently dead there.\n" + fields.joinToString("\n"),
            emptyList<String>(),
            fields,
        )
    }
}
