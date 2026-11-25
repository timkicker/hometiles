package dev.kicker.hometiles.sms

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * android admits to the default sms app that it is the default only while it holds six app
 * ops. one missing and `getDefaultSmsPackage` answers null inside the app, although the
 * system settings show HomeTiles as the messaging app: nothing incoming got stored, nothing
 * got marked read (user mail 25.11.2026, android 15; emulator api 35, 25.11.2026). the role
 * grants each op only when the manifest asks for its permission.
 */
class SmsRoleOpsTest {

    /**
     * the ops `SmsApplication` checks, by the permission behind each. the sixth, write_sms,
     * has no permission of its own and the role sets it alone.
     */
    private val exclusive = listOf(
        "READ_SMS",
        "RECEIVE_SMS",
        "RECEIVE_WAP_PUSH",
        "SEND_SMS",
        "READ_CELL_BROADCASTS",
    )

    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun `the manifest asks for every permission the default sms app is checked for`() {
        val missing = exclusive.filterNot { manifest.contains("android.permission.$it\"") }
        assertEquals(
            "without these the default sms app does not know it is the default on android 15",
            emptyList<String>(),
            missing,
        )
    }
}
