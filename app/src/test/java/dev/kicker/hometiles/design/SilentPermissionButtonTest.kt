package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * every screen that asks for a permission knows the case where android stops asking.
 *
 * after the second refusal `requestPermissions` returns at once with nothing to see. an "ask
 * now" button then does nothing, while the tile keeps saying one should tap it.
 * `PermissionGate` has always solved that, and its own comment names the trap.
 *
 * reproduced on 04.09.2026: the signal tile tapped, refused twice, then "ask now" again -
 * the screen closed, nothing else. endlessly repeatable. the signal explainer was the one
 * screen not using `PermissionState.blocked`; the rule stood nowhere, it was merely followed
 * in four places out of five.
 */
class SilentPermissionButtonTest {

    /** file -> why it asks for a permission. */
    private val askers = mapOf(
        "dev/kicker/hometiles/MainActivity.kt" to "the signal tile",
        "dev/kicker/hometiles/contacts/ContactsActivity.kt" to "the contact list",
        "dev/kicker/hometiles/phone/DialerActivity.kt" to "the call log",
        "dev/kicker/hometiles/sms/SmsActivity.kt" to "the message list",
    )

    @Test
    fun `whoever asks knows the never again too`() {
        val without = askers.filter { (path, _) ->
            val text = Quelltext.withoutComments(path)
            "RequestPermission()" in text && "PermissionState.blocked" !in text
        }
        assertEquals(
            "these screens ask for a permission but do not know the case where android " +
                "stops asking: " + without.values.joinToString(", ") +
                ". the button then does nothing, and there is no way out any more.",
            emptyMap<String, String>(),
            without,
        )
    }
}
