package dev.kicker.hometiles.design

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * whatever HomeTiles did not build itself, it starts through [dev.kicker.hometiles.actions.Intents].
 *
 * `startActivity` on a foreign app throws `ActivityNotFoundException` when the app is not
 * there and `SecurityException` when the permission is missing. `Intents` catches both and
 * says a sentence about it; two lines in the contact list did it themselves, and tapping
 * "edit in the address book" without an address book app crashed HomeTiles.
 *
 * our own screens are exempt - a class from the same program always exists.
 */
class ForeignIntentTest {

    /** how one sees that the target is HomeTiles itself. */
    private val ourOwn = listOf("::class.java", "SettingsLink.", ".intent(", "Intents.")

    /**
     * the emergency branch hands over to a foreign keypad and deliberately wants **not** to
     * hit HomeTiles; the call itself needs its own catch, since a `SecurityException` there
     * means "permission missing" and leads to the question, not to a hint.
     *
     * `Notice` starts one of our own screens through an intent rather than the class, so the
     * design system does not depend on the app.
     */
    private val allowed = listOf("DialerActivity.kt", "Notice.kt")

    @Test
    fun `foreign intents run through Intents`() {
        val bare = mutableListOf<String>()
        Quelltext.files().forEach { file ->
            if (file.name == "Intents.kt" || file.name in allowed) return@forEach
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                if (!line.contains("startActivity(")) return@forEachIndexed
                // the target often stands on the next line when the call wraps.
                val window = lines.subList(index, minOf(lines.size, index + 4)).joinToString(" ")
                // a catch of one's own counts too; the rule means the **uncaught** ones.
                val caught = line.contains("runCatching") ||
                    lines.subList(maxOf(0, index - 3), index).any { it.trim() == "try {" }
                if (!caught && ourOwn.none { it in window }) {
                    bare += "${file.name}:${index + 1}: ${line.trim()}"
                }
            }
        }
        assertTrue(
            "these calls start a foreign app without a catch - if it is missing, HomeTiles " +
                "crashes instead of saying a sentence:\n" + bare.joinToString("\n"),
            bare.isEmpty(),
        )
    }

    /**
     * the contact list sends into the address book's editor and gets no result back, so
     * whoever changed the number kept seeing the old one - and "call now" would have dialled
     * it.
     */
    @Test
    fun `the contact list reads again after the foreign editor`() {
        val source = Quelltext.withoutComments("dev/kicker/hometiles/contacts/ContactsActivity.kt")
        val load = Quelltext.cut(source, "LaunchedEffect(granted", "}")
        assertTrue(
            "the contacts are read only once. after editing in the address book the old " +
                "number would still stand here.",
            source.contains("LaunchedEffect(granted, resumes.intValue)"),
        )
        // a fixed window rather than a bound on a variable name: `substringBefore` on a name
        // that one day is gone returns **the whole rest** of the file, and the rule stays
        // green having checked nothing.
        val window = source
            .let { Quelltext.cut(it, "LaunchedEffect(granted, resumes.intValue)") }
            .take(800)
        assertTrue(
            "the open contact is not carried along when reading again: $load",
            window.contains("selected = selected?.let"),
        )
    }

    /**
     * `FLAG_ACTIVITY_NEW_TASK` hangs the foreign app in **its** task: "edit in the address
     * book" landed in the address book's task, and one back press left you in its contact
     * list rather than back in HomeTiles - out of the big type. from a service or the
     * application there is no task to go into, so the flag hangs on whether a screen stands
     * behind the call and not on a fixed value.
     */
    @Test
    fun `a foreign app starts in HomeTiles's task when there is one`() {
        val intents = Quelltext.withoutComments("dev/kicker/hometiles/actions/Intents.kt")
        val start = Quelltext.cut(intents, "private inline fun start(", "\n    }")
        assertTrue(
            "Intents hangs every foreign app in a task of its own; the back key then does " +
                "not lead back to HomeTiles:\n$start",
            start.contains("activityBehind(context) != null"),
        )
        val contacts = Quelltext.withoutComments("dev/kicker/hometiles/contacts/ContactRepository.kt")
        assertTrue(
            "the contact editor brings the flag itself - then the catch in Intents does not " +
                "help.",
            !contacts.contains("FLAG_ACTIVITY_NEW_TASK"),
        )
    }
}
