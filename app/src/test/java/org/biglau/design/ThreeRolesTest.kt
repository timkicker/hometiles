package org.biglau.design

import org.biglau.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * three roles, one place.
 *
 * BigLau can hold three roles of the system: home screen, phone, messages. two of them stood
 * in the settings tree with their state and a way to change it. the third stood only in the
 * message screen, and there only while BigLau did **not** hold it (`sms_not_default`).
 * whoever held it learned that nowhere, and could not get rid of it from here.
 *
 * noticed on 03.09.2026, when BigLau got the role: the tree said BigLau was the phone app and
 * said nothing about the messages.
 */
class ThreeRolesTest {

    private val tree = Quelltext.withoutComments("org/biglau/settings/SettingsActivity.kt")

    @Test
    fun `all three roles stand in the settings tree`() {
        listOf(
            "is_home" to "set_as_home",
            "is_dialer" to "set_as_dialer",
            "is_sms" to "set_as_sms",
        ).forEach { (held, open) ->
            assertTrue(
                "the role $held is missing from the settings tree - then it can be neither " +
                    "seen nor changed from here.",
                "R.string.$held" in tree && "R.string.$open" in tree,
            )
        }
    }

    /**
     * and each says how it stands **now**. roles are granted by the system and nothing comes
     * back from there. read once, the invitation one has just accepted still stands after
     * returning - see [SystemStateTest].
     */
    @Test
    fun `every role is read again on coming back`() {
        listOf(
            "isHomeScreen = remember",
            "isDialerApp = remember",
            "isSmsApp = remember",
        ).forEach { start ->
            val place = tree.indexOf(start)
            assertTrue("$start does not exist any more", place > 0)
            assertTrue(
                "$start reads the state only once - then the invitation still stands after " +
                    "the role is granted.",
                "resumes.intValue" in tree.substring(place, place + 60),
            )
        }
    }

    @Test
    fun `the six sentences exist in both languages`() {
        val missing = listOf(
            "is_home", "set_as_home", "is_dialer", "set_as_dialer", "is_sms", "set_as_sms",
        ).flatMap { name ->
            listOf("values", "values-de").filterNot { language ->
                Quelltext.texts(language).any { "name=\"$name\"" in it.readText() }
            }.map { "$name in $it" }
        }
        assertTrue("a sentence is missing in one language: $missing", missing.isEmpty())
    }
}
