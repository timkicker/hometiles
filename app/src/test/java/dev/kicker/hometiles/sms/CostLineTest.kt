package dev.kicker.hometiles.sms

import dev.kicker.hometiles.Quelltext
import dev.kicker.hometiles.data.ContactsConfig
import dev.kicker.hometiles.data.SmsConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * issue #22: the cost line under Send and the message row for a landline can be switched
 * off. both switches keep what was there before by default, so an update changes nothing.
 */
class CostLineTest {

    private fun source(path: String) = File(Quelltext.appRoot, "dev/kicker/hometiles/$path").readText()

    @Test
    fun `an update keeps the cost line and every message row`() {
        assertTrue(SmsConfig().showCost)
        assertFalse(ContactsConfig().messagesToMobileOnly)
    }

    @Test
    fun `the cost line follows its switch`() {
        assertTrue(source("sms/SmsActivity.kt").contains("draft.isNotBlank() && showCost -> pluralStringResource("))
    }

    @Test
    fun `the message row follows its switch`() {
        assertTrue(source("contacts/ContactsActivity.kt").contains("if (!textsMobileOnly || number.takesTexts) item {"))
    }

    @Test
    fun `both switches can be reached`() {
        assertTrue(source("sms/MessagesSettingsList.kt").contains("showCost = !sms.showCost"))
        assertTrue(source("settings/SettingsActivity.kt").contains("messagesToMobileOnly = !it.contacts.messagesToMobileOnly"))
    }
}
