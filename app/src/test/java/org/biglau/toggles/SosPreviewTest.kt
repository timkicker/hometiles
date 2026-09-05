package org.biglau.toggles

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the preview must under no circumstances send.
 *
 * to be able to look at the countdown screen a test number had been entered on the emulator
 * and the sms permission revoked - and **installing the new build granted it again**
 * (`GRANTED_BY_ROLE`). the next countdown ran through and sent the message to the test number.
 * nothing left the emulator, but that was not something to rely on.
 *
 * since then there is the preview: the same run, and **no** `Sos.send` at the end.
 */
class SosPreviewTest {

    // without comment lines: whether an alarm goes off that is meant for nobody hangs on this.
    private val source = Quelltext.withoutComments("org/biglau/toggles/SosActivity.kt")

    @Test
    fun `the preview does not send`() {
        val beforePreview = Quelltext.cut(source, "", "Sos.send(")
        assertTrue(
            "there is no check for the preview before sending",
            "if (preview)" in beforePreview,
        )
        assertTrue("the preview does not return before sending", "return@LaunchedEffect" in beforePreview)
    }

    @Test
    fun `the preview says so as well`() {
        // or whoever happens to see the screen does not know whether this is real.
        assertTrue("sos_preview_title" in source)
        assertTrue("sos_preview_done" in source)
    }

    @Test
    fun `the preview also runs without contacts entered`() {
        // otherwise a number would have to be entered first to see the run - exactly the
        // order that made the fault above possible.
        assertTrue("if (!configured && !preview) return@LaunchedEffect" in source)
    }

    /**
     * the preview also shows **what** would go out. before, the text could only be found out
     * by sending it, on a way one does not want to try. whoever sets the emergency call up for
     * someone should be able to read it before it reaches somebody else in earnest.
     */
    @Test
    fun `the preview shows the text that would go out`() {
        assertTrue("the text is not composed", "Sos.compose(" in source)
        assertTrue("the text is not shown", "previewText" in source)
        assertTrue(
            "it does not say whether a location is in it",
            "sos_preview_text_location" in source,
        )
    }

    /**
     * and the preview raises no alarm either.
     *
     * it says of itself that the same run happens and nothing goes out - and started the siren
     * while doing so: `SosAlarm.start` stood **before** the check for the preview. the sound
     * runs with `USAGE_ALARM`, so past do-not-disturb and at full volume.
     *
     * whoever wants to hear the alarm has "try it now" in the settings with a stop button
     * beside it - deliberate and labelled, instead of a side effect.
     */
    @Test
    fun `no alarm starts in the preview`() {
        val previewAt = source.indexOf("if (preview)")
        val alarmAt = source.indexOf("SosAlarm.start(")
        assertTrue("SosAlarm.start is gone entirely", alarmAt > 0)
        assertTrue(
            "the alarm starts before the check for the preview",
            previewAt in 1 until alarmAt,
        )
        val beforeAlarm = source.substring(0, alarmAt)
        assertTrue("the preview does not return before the alarm", "return@LaunchedEffect" in beforeAlarm)
    }
}
