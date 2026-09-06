package dev.kicker.hometiles.sms

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * reading picture messages out of the provider.
 *
 * three things about that table cannot be seen while reading the code, and each one produced
 * a wrong list until it was written down: the date counts in seconds, the sender is not in the
 * row, and an mms id may collide with an sms id.
 */
class MmsInListTest {

    private val source = Quelltext.file("dev/kicker/hometiles/sms/SmsRepository.kt").readText()
    private val mms = Quelltext.cut(source, "private fun loadMms", "private fun partsFor")

    @Test
    fun `the mms date is turned into milliseconds`() {
        assertTrue(
            "the date of an mms is not multiplied by 1000 - then every picture message " +
                "stands in january 1970 while the sms beside it is right.",
            "* 1000L" in mms,
        )
    }

    @Test
    fun `the sender comes from the addr sub-table`() {
        val sender = Quelltext.cut(source, "private fun senderOf", "companion object")
        assertTrue(
            "the sender is no longer read from the addr sub-table - the mms row itself has " +
                "no address, so the list would show an empty name.",
            "appendPath(\"addr\")" in sender && "137" in sender,
        )
    }

    @Test
    fun `an mms id cannot collide with an sms id`() {
        assertTrue(
            "the mms id is no longer negated - the conversation keys its list by the id, and " +
                "two equal keys crash it.",
            "id = -id" in mms,
        )
    }

    @Test
    fun `the parts are put together instead of shown raw`() {
        assertTrue(
            "MmsParts is no longer used - then the layout part could land in the list.",
            "MmsParts.preview" in mms,
        )
    }

    /** the picture is handed to coil as a uri; loading it here would hold a full bitmap. */
    @Test
    fun `the picture goes to the screen as a uri`() {
        assertTrue(
            "the picture uri is no longer built - then the conversation shows the word for " +
                "a picture and never the picture.",
            "content://mms/part/" in mms,
        )
        val conversation = Quelltext.file("dev/kicker/hometiles/sms/SmsActivity.kt").readText()
        assertTrue(
            "the conversation no longer draws message.imageUri - the picture would be read " +
                "and then thrown away.",
            "message.imageUri" in conversation && "AsyncImage" in conversation,
        )
    }
}
