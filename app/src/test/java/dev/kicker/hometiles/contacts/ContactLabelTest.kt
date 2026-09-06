package dev.kicker.hometiles.contacts

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * a number's label comes from the system, not from us.
 *
 * seen on the user's phone (03.09.2026): under a number stood the german word for mobile
 * while the button beside it said "Call straight away". the device is set to english; three
 * german words stood hardcoded in a mapping, and everything outside those three - fax, pager,
 * main line, custom labels - had none at all.
 *
 * `Phone.getTypeLabel` knows every kind and translates into the language of the resources
 * handed to it. that is why `load` now gets them from the caller: an activity passes its own,
 * and those already stand in the **app's** language, not the phone's. without that detour
 * the fault would merely have moved from german to english.
 */
class ContactLabelTest {

    private val source = Quelltext.file("dev/kicker/hometiles/contacts/ContactRepository.kt").readText()

    @Test
    fun `the label comes from getTypeLabel`() {
        assertTrue("getTypeLabel is not used", "getTypeLabel(" in source)
    }

    @Test
    fun `no mapping of our own with fixed words`() {
        // german words among them: they are what stood in the source, and the check compares
        // them literally.
        val forbidden = listOf("\"Mobil\"", "\"Privat\"", "\"Arbeit\"", "\"Mobile\"", "\"Home\"", "\"Work\"")
        assertEquals(
            "fixed label in the source",
            emptyList<String>(),
            forbidden.filter { it in source },
        )
    }

    /**
     * and the resources come from outside. were the reading to take the application
     * context's, the label would stand in the phone's language instead of the app's - the
     * same fault, only less conspicuous.
     */
    @Test
    fun `the caller sets the language`() {
        assertTrue("load takes no resources", "fun load(resources: Resources" in source)
        val callers = listOf(
            "dev/kicker/hometiles/contacts/ContactsActivity.kt",
            "dev/kicker/hometiles/sms/SmsActivity.kt",
            "dev/kicker/hometiles/phone/DialerActivity.kt",
        )
        val without = callers.filterNot { "load(resources)" in Quelltext.file(it).readText() }
        assertEquals("reads contacts without its own language: $without", emptyList<String>(), without)
    }
}
