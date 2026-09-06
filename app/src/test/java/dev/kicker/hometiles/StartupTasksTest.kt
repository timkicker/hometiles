package dev.kicker.hometiles

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * what has to be hooked up at start is hooked up at start.
 *
 * `PhoneNumbers` only computes and therefore lies in the pure kotlin module. two answers it
 * cannot fetch there - a number's spelling and the sim's country - and `SystemNumbers.install`
 * hooks both up at start.
 *
 * removing the call gives **no** error: HomeTiles then writes phone numbers in bare groups of
 * three. that is exactly how the fault arose that the user saw in his own contacts on
 * 02.09.2026 - "+436 804 ..." where austria is "+43". a quiet regression nobody reports.
 */
class StartupTasksTest {

    /**
     * read without comment lines: while counter-checking, the first version of this rule fell
     * for it itself - commenting the call out left it green, since the string still stood
     * there. a rule taking a commented-out call for a call checks nothing.
     */
    private val start = Quelltext.file("dev/kicker/hometiles/HomeTilesApp.kt")
        .readLines()
        .filterNot { Quelltext.isCommentLine(it) }
        .joinToString("\n")

    @Test
    fun `the start hooks up the system parts of the phone numbers`() {
        assertTrue(
            "HomeTilesApp no longer calls SystemNumbers.install. without the call HomeTiles writes " +
                "phone numbers in bare groups of three - no error, no crash.",
            "SystemNumbers.install" in start,
        )
    }

    /**
     * the alarm for the repeated reminder hangs on `ELAPSED_REALTIME_WAKEUP` and is gone after
     * the phone restarts. a `BOOT_COMPLETED` receiver deliberately does not exist - it would
     * cost another permission, and HomeTiles **is** the home screen: it runs after every restart
     * anyway.
     *
     * without this line a message unread before the restart would never remind again -
     * silently, and that is exactly what the setting promises.
     */
    @Test
    fun `the start sets the reminder alarm again`() {
        assertTrue(
            "HomeTilesApp no longer sets the reminder alarm - after a restart nothing reminds " +
                "of an unread message any more.",
            "MessageReminderReceiver.schedule" in start,
        )
        assertTrue(
            "without the check on SmsReminder.active HomeTiles sets an alarm at every start " +
                "that nobody ordered",
            "SmsReminder.active" in start,
        )
    }

    @Test
    fun `the start does not touch telephony itself`() {
        val framework = listOf("TelephonyManager", "PhoneNumberUtils")
            .filter { it in start }
        assertTrue(
            "HomeTilesApp touches android telephony directly: $framework. that belongs in " +
                "core:system (PLAN.md 2.1), or system access stands everywhere again.",
            framework.isEmpty(),
        )
    }
}
