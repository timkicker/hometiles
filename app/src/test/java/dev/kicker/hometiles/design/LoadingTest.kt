package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * while reading is going on, no list says it is empty.
 *
 * "no messages yet" is a true sentence, but not while the messages are being read - and on
 * this phone that takes visibly long, because the load fetches the messages **and all 338
 * contacts** for the names. **a true sentence at the wrong moment is a false statement.**
 *
 * the call log looked like a good example: it tells three reasons for being empty, and none
 * of them was "is being read". praise in a comment is no check, so it stands here as a rule.
 */
class LoadingTest {

    private val sms = Quelltext.file("dev/kicker/hometiles/sms/SmsActivity.kt").readText()

    private val calls = Quelltext.file("dev/kicker/hometiles/phone/DialerActivity.kt").readText()

    /**
     * three times in one day the same thing, each found on its own: at the third time a
     * single rule is no longer the right answer.
     *
     * **only lists filled afterwards count.** one already complete while drawing - an app's
     * shortcuts, the widgets present, the hidden apps from the configuration - is never
     * wrongly empty, so the rule asks whether the checked collection is a `var` with
     * `mutableStateOf`: only then is there a moment at which it is empty without being empty.
     */
    @Test
    fun `no sentence about emptiness hangs on an empty list alone`() {
        val emptySentences = Regex("""R\.string\.[a-z_]*(none|empty|no_missed|no_match)\b""")
        val evidence = listOf("loading", "isNotEmpty()")
        val unguarded = Quelltext.files()
            .filter { it.name.endsWith("Activity.kt") }
            .flatMap { file ->
                val lines = file.readLines()
                val text = file.readText()
                lines.withIndex()
                    .filter { (_, line) ->
                        emptySentences.containsMatchIn(line) && !Quelltext.isCommentLine(line)
                    }
                    .filterNot { (i, _) ->
                        lines.subList(maxOf(0, i - 12), i + 1)
                            .any { line -> evidence.any { it in line } }
                    }
                    .filter { (i, _) ->
                        // what does the sentence hang on, and is that collection filled only
                        // afterwards?
                        val checked = lines.subList(maxOf(0, i - 12), i + 1)
                            .firstNotNullOfOrNull {
                                Regex("""(\w+)\.isEmpty\(\)""").find(it)?.groupValues?.get(1)
                            }
                        checked != null &&
                            Regex("""var $checked by remember""").containsMatchIn(text)
                    }
                    .map { (i, line) -> "${file.name}:${i + 1}: ${line.trim()}" }
            }
        assertEquals(
            "a sentence about emptiness stands here hanging on nothing but the empty list " +
                "itself - and that list is filled only afterwards. while reading it is " +
                "empty, and then the sentence is a false statement.",
            emptyList<String>(),
            unguarded,
        )
    }

    @Test
    fun `the call log tells loading from empty`() {
        assertTrue("calllog_loading is gone", "R.string.calllog_loading" in calls)
        val from = calls.indexOf("emptyBecause != null")
        assertTrue("the call log's empty state is gone", from > 0)
        val block = calls.substring(from, minOf(calls.length, from + 700))
        assertTrue(
            "the reason for being empty is shown without asking first whether anything has " +
                "been read. then it says: no calls, while reading.",
            "if (loading)" in block,
        )
    }

    /** the loading state has to end in **both** exits. */
    @Test
    fun `the call log's loading state ends without the permission too`() {
        val hits = Regex("""loading = false""").findAll(calls).count()
        assertEquals(
            "loading is not reset in both exits. a loading state that never ends is worse " +
                "than none.",
            2,
            hits,
        )
    }

    @Test
    fun `the message list tells loading from empty`() {
        assertTrue("sms_loading is gone", "R.string.sms_loading" in sms)
        assertTrue(
            "the empty state hangs on nothing - then \"no messages yet\" stands there while " +
                "reading too.",
            "if (loading) R.string.sms_loading else R.string.sms_empty" in sms,
        )
    }

    @Test
    fun `the loading state ends, without the permission too`() {
        val load = Quelltext.cut(sms, "var loading by remember", "val threads")
        assertEquals(
            "`loading = false` does not appear twice: once after reading and once in the " +
                "branch without the read permission. without the second it says \"reading " +
                "messages\" forever.",
            2,
            Regex("""loading = false""").findAll(load).count(),
        )
    }

    @Test
    fun `the contact list has kept its loading hint`() {
        val contacts = Quelltext.file("dev/kicker/hometiles/contacts/ContactsActivity.kt").readText()
        assertTrue("contacts_loading is gone", "R.string.contacts_loading" in contacts)
    }
}
