package dev.kicker.hometiles.sms

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the notice belongs inside the conversation, not before it.
 *
 * set fixed above the list, the sentence about HomeTiles not being the messages app took five
 * lines at 200 percent text size - and of the conversation a strip of **two pixels** was
 * left. a notice that pushes out the content it is about is no notice any more.
 *
 * inside the list it costs nothing: the conversation opens at the newest message, and whoever
 * looks up finds it where the oldest stand.
 */
class ConversationLayoutTest {

    @Test
    fun `the notice stands inside the list`() {
        val source = Quelltext.file("dev/kicker/hometiles/sms/SmsActivity.kt").readText()
        val list = source.indexOf("LazyColumn(\n            state = listState")
        val notice = source.indexOf("R.string.sms_not_default")
        assertTrue("the conversation's LazyColumn was not found", list > 0)
        assertTrue("the notice was not found", notice > 0)
        assertTrue(
            "the notice stands before the list - at large text it pushes the list out",
            notice > list,
        )
    }
}
