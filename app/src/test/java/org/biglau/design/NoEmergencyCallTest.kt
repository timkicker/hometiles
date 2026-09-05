package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the sos calls nobody.
 *
 * BigLau's sos sends messages, no more - no call, least of all to 112. that was always so
 * and stood nowhere as a rule; it stood only because nobody had written it down. `PLAN.md`
 * 4.8 and P7 name an automatic call instead, and a plan promising more than the source does
 * is the beginning of somebody building it.
 *
 * the rule stands here because the price of a fault is not measured in screens: an
 * accidental emergency call ties up people needed elsewhere. the emergency numbers in
 * [org.biglau.phone.PhoneNumbers] exist for the opposite - the dial pad should **recognise**
 * them and hand them to the system dialer instead of dialling itself.
 */
class NoEmergencyCallTest {

    /** everything involved in the sos. */
    private val sosFiles = Quelltext.files()
        .filter { it.name.startsWith("Sos") }

    @Test
    fun `no part of the sos dials`() {
        assertTrue(
            "not a single Sos file found - does the rule still read what it means?",
            sosFiles.size >= 5,
        )
        val dials = sosFiles.flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) ->
                    val bare = line.trim()
                    !Quelltext.isCommentLine(line) &&
                        ("ACTION_CALL" in bare || "Intents.call(" in bare)
                }
                .map { (i, line) -> "${file.name}:${i + 1}: ${line.trim()}" }
        }
        assertEquals(
            "the sos dials here. BigLau's sos sends messages - a call from here can hit an " +
                "emergency number, and somebody else pays for the mistake.",
            emptyList<String>(),
            dials,
        )
    }

    @Test
    fun `no emergency number stands in the sos path`() {
        val named = sosFiles.flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> "WELL_KNOWN_EMERGENCY" in line || "looksLikeEmergency" in line }
                .map { (i, _) -> "${file.name}:${i + 1}" }
        }
        assertEquals(
            "the sos path touches the emergency numbers. the list exists so the dial pad " +
                "recognises them and hands them on - not so anyone dials them.",
            emptyList<String>(),
            named,
        )
    }

    @Test
    fun `the preview does not reach the sending`() {
        // without comments: the branch itself carries a sentence naming `Sos.send`, and the
        // first version of this rule read it as a call.
        val source = Quelltext.withoutComments("org/biglau/toggles/SosActivity.kt")
        val from = source.indexOf("if (preview) {")
        assertTrue("the preview branch no longer exists", from > 0)
        val branch = source.substring(from, source.indexOf("return@LaunchedEffect", from))
        assertTrue(
            "the preview reaches the sending. a preview that sends is none.",
            "Sos.send" !in branch,
        )
        assertTrue(
            "the preview no longer shows what would go out - then the text could only be " +
                "found out by sending it.",
            "Sos.compose" in branch,
        )
    }

    @Test
    fun `without a number nothing goes out`() {
        val send = Quelltext.file("org/biglau/toggles/Sos.kt").readText()
            .let { Quelltext.cut(it, "fun send(") }
        val first = send.lines().drop(1).first { it.isNotBlank() }
        assertTrue(
            "the first line of Sos.send no longer checks whether a number is entered at " +
                "all: $first",
            "numbers.isEmpty()" in first && "return" in first,
        )
    }
}
