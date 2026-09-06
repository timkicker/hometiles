package dev.kicker.hometiles.phone

import dev.kicker.hometiles.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * what HomeTiles has to bring along to be selectable as a phone app at all.
 *
 * `RoleRequestTest` next door checks that the role dialog is opened correctly. this rule
 * checks the other half: that there is anything to choose.
 *
 * android demands an `ACTION_DIAL` entry for the phone role; without it HomeTiles does not even
 * stand in the choice of default phone app. the app's own call screen needs an
 * `InCallService` on top, with the permission and the `IN_CALL_SERVICE_UI` mark - without
 * the mark android keeps showing its own view, without a word about it.
 */
class PhoneRoleTest {

    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun `there is an entry for ACTION_DIAL`() {
        val dialer = Quelltext.cut(manifest, "\".phone.DialerActivity\"", "</activity>")
        assertTrue("ACTION_DIAL is missing - HomeTiles is then not up for choice", "android.intent.action.DIAL" in dialer)
        assertTrue("the entry is not exported - android then does not see it", "android:exported=\"true\"" in dialer)
        assertTrue("tel: is missing - a call from another app lands nowhere", "\"tel\"" in dialer)
    }

    @Test
    fun `the app's own call screen is registered as such`() {
        // from the name attribute, not the first occurrence: a paragraph above a comment
        // *mentions* the service, and the cut landed there.
        val service = Quelltext.cut(manifest, "\".phone.BigInCallService\"", "</service>")
        assertTrue("the InCallService filter is missing", "android.telecom.InCallService" in service)
        assertTrue("BIND_INCALL_SERVICE is missing", "android.permission.BIND_INCALL_SERVICE" in service)
        assertTrue(
            "IN_CALL_SERVICE_UI is missing - android then shows its own view, wordlessly",
            "android.telecom.IN_CALL_SERVICE_UI" in service && "android:value=\"true\"" in service,
        )
    }

    @Test
    fun `the home screen registers itself as a home screen`() {
        val main = Quelltext.cut(manifest, "\".MainActivity\"", "</activity>")
        listOf(
            "android.intent.action.MAIN",
            "android.intent.category.HOME",
            "android.intent.category.DEFAULT",
        ).forEach { assertTrue("$it is missing - without it HomeTiles is no home screen", it in main) }
    }
}
