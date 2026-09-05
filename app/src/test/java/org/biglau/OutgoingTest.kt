package org.biglau

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * whatever goes out goes out only on a hand movement.
 *
 * BigLau can call and send messages - it is a phone. the danger is not the ability but
 * **where** it stands: a receiver, a service or the program start has no person in front of
 * it, and whatever dials from there dials at four in the morning too.
 *
 * the ban in `STATUS.md` is an agreement with the user, not a lock: `CALL_PHONE` and
 * `SEND_SMS` were **granted** on this phone on 03.09.2026. two assurances:
 *
 * 1. only the listed files may dial or send. a new one stands out here before it stands out
 *    to whoever gets called unintentionally.
 * 2. none of these files is a `BroadcastReceiver`, a `Service` or the `Application`.
 */
class OutgoingTest {

    /**
     * file to who makes the hand movement, and **which of the three cases** from `PLAN.md`
     * 3.1 that is: a row naming the action, something the user set up for it, or a question.
     */
    private val mayGoOut = mapOf(
        "DialerActivity.kt" to
            "the call button on the keypad (names the action), the speed dial (set by the " +
                "user) and the call log (asks first)",
        "Intents.kt" to "builds the intent; a screen triggers it",
        "Sos.kt" to "the emergency call, after a countdown and with a cancel button (SosActivity)",
        "SmsActivity.kt" to "the send button in the conversation, after a tap",
        "ContactsActivity.kt" to
            "the \"call\" row in a contact - it names the action it triggers",
        "MainActivity.kt" to
            "a contact tile the user set to \"call\", and the question \"call or write?\" " +
                "at ContactMode.ASK",
        "InCallActivity.kt" to
            "the short answer on a ringing call - the row carries the whole sentence it " +
                "sends (names the action)",
        "InCallRepository.kt" to
            "hands that sentence to telecom, which sends it; nothing else calls this",
    )

    /**
     * the way round through [org.biglau.actions.Intents] counts too: looking for `ACTION_CALL`
     * in plain text missed **two** screens dialling through `Intents.call(…)`. a permission
     * list one gets past with one step in between is none.
     *
     * `reject(true, …)` belongs here for the same reason although no `sendTextMessage` stands
     * beside it: telecom sends that text itself. seen at the emulator on 05.09.2026 - without
     * it this list would have stayed green while a new way out was built.
     */
    private val outgoingPattern =
        Regex("""ACTION_CALL|Intents\.call\(|sendTextMessage|sendMultipartTextMessage|reject\(true|rejectWith\(""")

    private fun places(): Map<File, List<String>> = Quelltext.files()
        .associateWith { file ->
            file.readLines().filter { line ->
                outgoingPattern.containsMatchIn(line) && !Quelltext.isCommentLine(line)
            }
        }
        .filterValues { it.isNotEmpty() }

    @Test
    fun `only named places may dial or send`() {
        assertEquals(
            "a new place dials or sends. that is the one thing which must never happen by " +
                "accident in this app - into the list in OutgoingTest with a reason, or out " +
                "with it.",
            mayGoOut.keys.sorted(),
            places().keys.map { it.name }.sorted(),
        )
    }

    @Test
    fun `none of these places runs without a person in front of it`() {
        val withoutPeople = places().keys.filter { file ->
            val text = file.readText()
            Regex(""": *(BroadcastReceiver|Service|Application)\b""").containsMatchIn(text)
        }.map { it.name }
        assertEquals(
            "something dials or sends here with nobody in front of it - a receiver, a " +
                "service or the program start. exactly what must not be.",
            emptyList<String>(),
            withoutPeople,
        )
    }

    /** file to who makes the hand movement, for the places going through `Intents`. */
    private val mayGoOutThroughIntents = mapOf(
        "ContactsActivity.kt" to "a tap on a contact in the list",
        "MainActivity.kt" to "a contact tile, and the pin flow before it",
    )

    /**
     * the **callers** count as well: `Intents.call` and `Intents.sms` build the intent,
     * whoever calls them triggers it. the list above does not see those callers, since no
     * `ACTION_CALL` stands in them.
     */
    @Test
    fun `whoever dials or writes through Intents stands in the list too`() {
        val pattern = Regex("""Intents\.(call|sms)\(""")
        val places = Quelltext.files()
            .filter { file ->
                file.name != "Intents.kt" &&
                    file.readLines().any {
                        pattern.containsMatchIn(it) && !Quelltext.isCommentLine(it)
                    }
            }
        assertEquals(
            "a new place triggers a call or a message. into the list in OutgoingTest with a " +
                "reason, or out with it.",
            mayGoOutThroughIntents.keys.sorted(),
            places.map { it.name }.sorted(),
        )
        val withoutPeople = places.filter {
            Regex(""": *(BroadcastReceiver|Service|Application)\b""").containsMatchIn(it.readText())
        }.map { it.name }
        assertEquals(
            "something dials or writes here with nobody in front of it",
            emptyList<String>(),
            withoutPeople,
        )
    }

    @Test
    fun `every exception names its hand movement`() {
        (mayGoOut + mayGoOutThroughIntents).forEach { (file, reason) ->
            assertTrue("$file: the reason is missing or too short", reason.length > 20)
        }
    }
}
