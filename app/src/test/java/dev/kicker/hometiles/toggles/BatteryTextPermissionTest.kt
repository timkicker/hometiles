package dev.kicker.hometiles.toggles

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * the battery text goes out with nobody at the phone. without the right to send it fails
 * silently, so switching it on has to ask for that right, the same way the location does.
 */
class BatteryTextPermissionTest {

    private val settings = Quelltext.withoutComments("dev/kicker/hometiles/toggles/SosSettings.kt")

    @Test
    fun `switching the battery text on asks for the right to send`() {
        assertTrue(
            "the battery text switch does not ask for SEND_SMS when it goes on.",
            Regex("""if \(on && !smsGranted\) onAskSms\(\)""").containsMatchIn(settings),
        )
    }

    @Test
    fun `a missing right is shown on the page`() {
        assertTrue(
            "with the battery text on and no right to send, the page says nothing.",
            Regex("""config\.batteryText && !smsGranted""").containsMatchIn(settings),
        )
    }
}
