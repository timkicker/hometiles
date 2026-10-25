package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the lists a flip phone works with keys follow KeyFirst (issue #2, a cat s22 flip): the focus
 * on the first entry, no photos, no page title. a screen that forgets the setting looks like
 * it works on a touch phone and does nothing on the phone it was made for.
 */
class KeyFirstListsTest {

    private val screens = listOf(
        "dev/kicker/hometiles/contacts/ContactsActivity.kt",
        "dev/kicker/hometiles/phone/DialerActivity.kt",
        "dev/kicker/hometiles/apps/AppDrawerActivity.kt",
        "dev/kicker/hometiles/sms/SmsActivity.kt",
    )

    @Test
    fun `every list screen asks the setting and puts the focus on the first entry`() {
        screens.forEach { path ->
            val text = Quelltext.withoutComments(path)
            assertTrue("$path does not ask KeyFirst", "keyFirstOn(config.behaviour.keyFirst)" in text)
            assertTrue("$path puts the focus nowhere", "firstEntryFocus(" in text)
            assertTrue("$path keeps its page title", "!keyFirst" in text)
            // without the group around the screen the window's first focus fell to the
            // search field, and its keyboard covered half the list (emulator, 25.10.2026).
            assertTrue("$path has no focus group around the list", "Focus.screen" in text)
            assertTrue("$path does not say when its list is still loading", "loading = " in text)
        }
    }

    @Test
    fun `the contact list leaves the photos out`() {
        val text = Quelltext.withoutComments("dev/kicker/hometiles/contacts/ContactsActivity.kt")
        assertTrue("contacts keep their photos", "leading = if (keyFirst) null else" in text)
    }

    @Test
    fun `the call question has the focus on yes`() {
        val text = Quelltext.withoutComments("dev/kicker/hometiles/phone/DialerActivity.kt")
        assertTrue("the call question puts the focus nowhere", "modifier = yesFocus" in text)
    }

    @Test
    fun `the settings offer all three choices`() {
        val text = Quelltext.withoutComments("dev/kicker/hometiles/settings/SettingsActivity.kt")
        assertTrue("the setting cannot be changed", "items(KeyFirst.entries.toList())" in text)
        assertTrue("a choice does not save", "behaviour.copy(keyFirst = mode)" in text)
    }
}
