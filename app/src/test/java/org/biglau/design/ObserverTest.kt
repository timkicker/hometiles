package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the list follows the database, not only itself.
 *
 * `SmsRepository.changes` ticked only when BigLau wrote - a new message, a mark as read. if
 * somebody else changed something, the list stood still. seen exactly that way on 03.09.2026:
 * two messages had been deleted from the database and stood there until the app restarted.
 *
 * it is the same fault as with the states the system grants (see `SystemStateTest`): read
 * once, never asked again. only the source here is a database, not a role - and it reports
 * of its own accord once one asks it to.
 */
class ObserverTest {

    private val source = Quelltext.withoutComments("org/biglau/sms/SmsActivity.kt")

    @Test
    fun `the message screen listens to the database`() {
        assertTrue(
            "SmsActivity registers no observer any more. then the list keeps showing what " +
                "has long been deleted - and hides what came in from elsewhere.",
            "registerContentObserver" in source && "Telephony.Sms.CONTENT_URI" in source,
        )
    }

    @Test
    fun `and unregisters it again`() {
        assertTrue(
            "the observer is not unregistered - it outlives the screen and keeps it alive.",
            "unregisterContentObserver" in source,
        )
        val registered = source.indexOf("registerContentObserver")
        val unregistered = source.indexOf("unregisterContentObserver")
        assertTrue(
            "unregistering does not stand after registering - then it does not belong to it.",
            unregistered > registered,
        )
    }

    @Test
    fun `the observer passes the change on`() {
        val from = source.indexOf("ContentObserver(")
        assertTrue("there is no observer any more", from > 0)
        val body = source.substring(from, minOf(source.length, from + 400))
        assertTrue(
            "the observer does nothing with what it hears.",
            "notifyChanged" in body,
        )
    }
}
