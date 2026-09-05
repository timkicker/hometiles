package org.biglau.ui

import org.biglau.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a state carried only by a colour or an icon is not there when read aloud.
 *
 * the message list appended a bare "(2)" to the name - a number without a thing - and the
 * call log showed the direction as an arrow without a name, in the one list where the
 * direction is everything.
 *
 * choice and switch have their own announcements; this is the third case, and it has no fixed
 * formula, so the list brings its own sentence.
 */
class StateSpokenTest {

    private val row = Quelltext.withoutComments("org/biglau/ui/BigRow.kt")

    @Test
    fun `a row can bring its own state`() {
        assertTrue(
            "BigRow knows no free state - then something like \"unread\" stays stuck in the " +
                "colour.",
            row.contains("state: String? = null"),
        )
        assertTrue(
            "the state handed over is not announced.",
            row.contains("state != null -> stringResource(R.string.a11y_state"),
        )
    }

    @Test
    fun `the message list says what is unread`() {
        val list = Quelltext.withoutComments("org/biglau/sms/SmsActivity.kt")
        assertTrue(
            "the message list names the unread ones only as a number in brackets.",
            list.contains("a11y_unread"),
        )
    }

    @Test
    fun `the call log says the direction`() {
        val list = Quelltext.withoutComments("org/biglau/phone/DialerActivity.kt")
        assertTrue(
            "the call log shows the direction only as an arrow. an arrow has no name.",
            list.contains("state = stringResource(callDirectionSpeech("),
        )
    }

    /**
     * the settings tree lists the same kinds for switching on and off, with category names in
     * the plural, while the row needs the adjective. two sentences, two purposes - but both
     * mappings in **one** file, or they drift apart.
     */
    @Test
    fun `the direction words stand only once`() {
        val places = Quelltext.files().filter {
            val t = it.readText()
            "CallDirection.MISSED -> R.string.call_type_missed" in t ||
                "CallDirection.MISSED -> R.string.call_dir_missed" in t
        }
        assertEquals(
            "the mapping direction to word stands more than once: " + places.map { it.name },
            1,
            places.size,
        )
    }

    /**
     * the message list paints the number in brackets behind the name and says it in the state
     * as a sentence - read aloud it came twice. the call log paints it too but says it nowhere
     * else, so there the brackets carry it.
     */
    @Test
    fun `the message list does not say the number twice`() {
        val list = Quelltext.withoutComments("org/biglau/sms/SmsActivity.kt")
        assertTrue(
            "the row hands its label including the bracketed number to be read aloud, while " +
                "the state already says the same number as a sentence.",
            list.contains("labelSpeech = thread.titleOr("),
        )
    }

    /**
     * a bubble hangs left or right and has one colour or the other - read aloud both are
     * nothing. one heard a row of sentences without a sender, and "on my way" without a sender
     * is the opposite.
     */
    @Test
    fun `every message bubble says who it is from`() {
        val list = Quelltext.withoutComments("org/biglau/sms/SmsActivity.kt")
        assertTrue(
            "the message bubbles do not say whether they were received or sent.",
            list.contains("a11y_message_in") && list.contains("a11y_message_out"),
        )
        assertTrue(
            "the description is built but not announced.",
            list.contains("semantics(mergeDescendants = true)"),
        )
    }

    /**
     * on the pin screen there was no feedback at all: BigLau deliberately makes no sound, and
     * whether a key arrived was only to be seen. the **number** of digits does not give the
     * pin away, and keeping it quiet helps nobody.
     *
     * as a `liveRegion`, because it has to be said while typing and not only when the dots are
     * touched.
     */
    @Test
    fun `the row of dots says how many digits stand there`() {
        val keypad = Quelltext.withoutComments("org/biglau/ui/BigKeypad.kt")
        val dots = Quelltext.cut(keypad, "fun PinDots(")
        assertTrue(
            "the row of dots says nothing. whoever cannot see it does not know on the pin " +
                "screen whether a key arrived.",
            dots.contains("a11y_pin_digits") && dots.contains("a11y_pin_empty"),
        )
        assertTrue(
            "the announcement comes only on touch, not while typing.",
            dots.contains("liveRegion = LiveRegionMode.Polite"),
        )
    }
}
