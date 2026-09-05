package org.biglau.res

import org.biglau.Quelltext
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * texts claiming something about *this* phone go and look.
 *
 * two sentences did not, and both showed up on the emulator where BigLau holds the sms and
 * the dialer role: the hiding list said BigLau was not this phone's sms app while it was
 * storing the incoming messages, and the phone-state question said BigLau could not call
 * anyone while the dialer role gave it exactly that.
 *
 * both sentences exist to build trust, and one that builds trust while being wrong is worse
 * than none.
 */
class DeviceClaimsTest {

    private fun source(path: String) = Quelltext.file(path).readText()

    @Test
    fun `the hint about the hiding list asks for the sms role`() {
        val place = source("org/biglau/sms/MessagesSettingsList.kt")
            .let { Quelltext.cut(it, "sms_filter_numbers_heading", "OutlinedTextField") }
        // the value now arrives as `holdsSmsRole` from the activity, so the rule asks for
        // either spelling: what it checks is that the sentence hangs on the role.
        assertTrue(
            "does not look at the role: $place",
            "isDefaultSmsApp()" in place || "holdsSmsRole" in place,
        )
        assertTrue("the second wording is gone: $place", "sms_filter_hint_default" in place)
    }

    @Test
    fun `the text before the phone-state question looks at CALL_PHONE`() {
        val place = source("org/biglau/MainActivity.kt")
            .let { Quelltext.cut(it, "R.string.signal_permission_title", "BigRow") }
        assertTrue("does not look at CALL_PHONE: $place", "CALL_PHONE" in place)
        assertTrue("the second wording is gone: $place", "signal_permission_body_may_call" in place)
    }

    /**
     * the number block promised "are refused without ringing", but only the default phone app
     * sees incoming calls. the page must ask rather than assume: on this very phone the role
     * has been held by two different apps.
     */
    @Test
    fun `the hint about blocked numbers asks for the dialer role`() {
        val text = source("org/biglau/settings/SettingsActivity.kt")
        val place = text
            .let { Quelltext.cut(it, "R.string.blocked_numbers)", "OutlinedTextField") }
        assertTrue("the role is read nowhere", "DialerRole.held(" in text)
        assertTrue("does not look at the role: $place", "hatTelefonRolle" in place)
        assertTrue("the second wording is gone: $place", "blocked_numbers_hint_outgoing" in place)
    }

    /**
     * the sentence about blinking demanded notification access. since the missed-calls and
     * messages tiles count the call log and the message provider, those two blink without it,
     * and demanding an access that is not needed costs trust twice.
     */
    @Test
    fun `the sentence about blinking matches what counts without access`() {
        val logic = source("org/biglau/notify/TileNotifications.kt")
        assertTrue("missed calls are not counted by us", "Builtin.MISSED_CALLS) return missed" in logic)
        assertTrue("unread messages are not counted by us", "unread != null) return unread" in logic)
        listOf(
            "values-de" to listOf("verpasste anrufe", "ungelesene nachrichten"),
            "values" to listOf("missed calls", "unread messages"),
        ).forEach { (directory, words) ->
            // compared in lower case: whether the phrase opens the sentence or sits in the
            // middle is grammar, not the claim.
            val sentence = Quelltext.texts(directory).joinToString("\n") { it.readText() }
                .let { Quelltext.cut(it, "name=\"blink_explainer\"", "</string>") }
                .lowercase()
            words.forEach { word ->
                assertTrue("$directory: $word is missing from the sentence: $sentence", word in sentence)
            }
        }
    }

    @Test
    fun `both wordings stand in both languages`() {
        // per language, not per file - the texts lie in several modules.
        listOf("values", "values-de").forEach { language ->
            val texts = Quelltext.texts(language).joinToString("\n") { it.readText() }
            listOf(
                "sms_filter_hint",
                "sms_filter_hint_default",
                "signal_permission_body",
                "signal_permission_body_may_call",
                "blocked_numbers_hint",
                "blocked_numbers_hint_outgoing",
            ).forEach { name ->
                assertTrue("$language: $name is missing", "\"$name\"" in texts)
            }
        }
    }
}
