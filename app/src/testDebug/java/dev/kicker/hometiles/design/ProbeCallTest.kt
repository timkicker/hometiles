package dev.kicker.hometiles.design

import java.io.File
import dev.kicker.hometiles.Quelltext
import dev.kicker.hometiles.phone.probe.ProbeCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the probe call stays a probe.
 *
 * this file lies in `src/testDebug`, not in `src/test`: `src/test` is compiled for both
 * variants, `src/debug` exists in only one - a test there that names the probe breaks the
 * release build.
 *
 * two things must hold, and both are held here: it must never touch an emergency number, and
 * it must never land in a delivered program.
 */
class ProbeCallTest {

    private val debugRoot = File("src/debug/java/dev/kicker/hometiles/phone/probe")

    @Test
    fun `no probe with an emergency number`() {
        listOf("112", "911", "+112", "110", "999").forEach { number ->
            assertFalse(
                "with $number a probe would be possible - and out of a probe an emergency " +
                    "call would have become.",
                ProbeCall.allowed(number),
            )
        }
    }

    @Test
    fun `the probe number belongs to nobody`() {
        assertTrue("the probe number is not allowed", ProbeCall.allowed(ProbeCall.NUMBER))
        assertTrue(
            "the probe number no longer lies in the range reserved for film and television " +
                "(07700 900000-900999). an invented number can one day really exist.",
            ProbeCall.NUMBER.startsWith("+447700900"),
        )
    }

    @Test
    fun `without a number there is no probe`() {
        assertFalse(ProbeCall.allowed(""))
        assertFalse(ProbeCall.allowed("   "))
    }

    @Test
    fun `the probe lies in the debug build only`() {
        assertTrue(
            "the probe no longer lies in src/debug - then it ships with the delivered program.",
            debugRoot.isDirectory && debugRoot.listFiles().orEmpty().isNotEmpty(),
        )
        val inMainBuild = Quelltext.files()
            .filter { file ->
                val text = file.readText()
                "phone.probe" in text || "ProbeConnectionService" in text
            }
            .map { it.name }
        assertEquals(
            "the main build knows the probe. a way to fake a call does not belong in the " +
                "hands of somebody who merely installed the app.",
            emptyList<String>(),
            inMainBuild,
        )
    }

    /**
     * and it clears only **its own** trace.
     *
     * a delete on `CallLog.Calls.CONTENT_URI` without a condition empties the whole call log.
     * the probe deletes there, in a debug build that runs on the user's everyday phone - the
     * condition is the difference between tidying up and losing data.
     */
    @Test
    fun `the probe clears only its own trace`() {
        val source = File("src/debug/java/dev/kicker/hometiles/phone/probe/ProbeConnectionService.kt")
        assertTrue("the connection service is gone", source.isFile)
        val text = source.readText()
        assertTrue(
            "the probe no longer clears its trace from the call log - then the probes pile " +
                "up on top of the user's list.",
            "CallLog.Calls.CONTENT_URI" in text,
        )
        val delete = text.substringAfter("contentResolver.delete(").substringBefore(")")
        assertTrue(
            "the delete does not name the probe number: $delete - without a condition it " +
                "empties the whole call log.",
            "ProbeCall.NUMBER" in delete && "LIKE" in delete,
        )
        assertFalse(
            "the delete has no condition.",
            Regex("""CONTENT_URI,\s*null,\s*null""").containsMatchIn(text),
        )
    }

    @Test
    fun `the connection service stands in the debug manifest only`() {
        val debug = File("src/debug/AndroidManifest.xml")
        assertTrue("there is no debug manifest any more", debug.isFile)
        assertTrue(
            "the connection service is no longer in the debug manifest",
            "ProbeConnectionService" in debug.readText(),
        )
        assertFalse(
            "the connection service is in the main manifest - that would put it in every build.",
            "probe" in File("src/main/AndroidManifest.xml").readText(),
        )
    }
}
